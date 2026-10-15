package hits.fire;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.nio.ByteBuffer;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

public final class Gpu {
    public static final int NONE = 0;
    public static final int PRE = 1;
    private static final OutputTarget OUT = new OutputTarget("fireglow_mask", () -> Gpu.target);
    private static final Map<RenderType, RenderType> MASKS = new IdentityHashMap<>();
    private static final Map<RenderType, RenderType> FLATS = new IdentityHashMap<>();
    private static final RenderPipeline[] UI = new RenderPipeline[5];
    private static final Vector4f CLEAR = new Vector4f(0f, 0f, 0f, 0f);
    private static RenderPipeline mask;
    private static RenderPipeline flat;
    private static Target target;
    private static Tex maskTex;
    private static Tex screenTex;
    private static Tex wrapped;

    public static final class Tex {
        private final GpuTexture texture;
        private final GpuTextureView view;

        Tex(GpuTexture texture, GpuTextureView view) {
            this.texture = texture;
            this.view = view;
        }

        public GpuTextureView view() {
            return view;
        }

        public int width() {
            return view.texture().getWidth(0);
        }

        public int height() {
            return view.texture().getHeight(0);
        }

        public void close() {
            if (texture == null) return;
            view.close();
            texture.close();
        }
    }

    public static final class Buf {
        private final GpuBuffer buffer;

        Buf(GpuBuffer buffer) {
            this.buffer = buffer;
        }
    }

    public static final class Pipe {
        private final RenderPipeline pipeline;

        Pipe(RenderPipeline pipeline) {
            this.pipeline = pipeline;
        }

        public RenderPipeline get() {
            return pipeline;
        }
    }

    private Gpu() {
    }

    private static RenderTarget main() {
        return Minecraft.getInstance().gameRenderer.mainRenderTarget();
    }

    public static boolean frame() {
        RenderTarget main = main();
        if (main.width <= 0 || main.height <= 0) return false;
        if (target == null) target = new Target();
        if (target.width != main.width || target.height != main.height || target.getColorTexture() == null) {
            target.resize(main.width, main.height);
        }
        return true;
    }

    public static void clearMask() {
        RenderSystem.getDevice().createCommandEncoder().clearColorTexture(target.getColorTexture(), CLEAR);
    }

    public static Tex mask() {
        GpuTextureView v = target.getColorTextureView();
        if (maskTex == null || maskTex.view != v) maskTex = new Tex(null, v);
        return maskTex;
    }

    public static Tex screen() {
        GpuTextureView v = main().getColorTextureView();
        if (screenTex == null || screenTex.view != v) screenTex = new Tex(null, v);
        return screenTex;
    }

    public static Tex wrap(GpuTextureView v) {
        if (wrapped == null || wrapped.view != v) wrapped = new Tex(null, v);
        return wrapped;
    }

    public static boolean redirected() {
        return RenderSystem.outputColorTextureOverride != null;
    }

    private static ColorTargetState color(int blend) {
        if (blend == NONE) return new ColorTargetState(Optional.empty(), GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_ALL);
        return new ColorTargetState(Optional.of(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA), GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_COLOR);
    }

    public static Pipe post(String frag, String block, int blend, boolean masked) {
        BindGroupLayout.Builder layout = BindGroupLayout.builder()
            .withSampler("InSampler")
            .withUniform(block, UniformType.UNIFORM_BUFFER);
        if (masked) layout.withSampler("MaskSampler");
        return new Pipe(RenderPipeline.builder()
            .withLocation(Glow.id("pipeline/" + frag + "_" + blend))
            .withVertexShader(Glow.id("core/quad"))
            .withFragmentShader(Glow.id("core/" + frag))
            .withBindGroupLayout(layout.build())
            .withCull(false)
            .withColorTargetState(color(blend))
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .build());
    }

    public static RenderPipeline ui(int mode) {
        if (UI[mode] == null) {
            UI[mode] = RenderPipeline.builder()
                .withLocation(Glow.id("pipeline/ui_" + mode))
                .withVertexShader(Glow.id("core/ui"))
                .withFragmentShader(Glow.id("core/ui"))
                .withShaderDefine("MODE", mode)
                .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .build();
        }
        return UI[mode];
    }

    public static Pipe halo() {
        return new Pipe(RenderPipeline.builder()
            .withLocation(Glow.id("pipeline/halo"))
            .withVertexShader(Glow.id("core/ui"))
            .withFragmentShader(Glow.id("core/halo"))
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withColorTargetState(color(PRE))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .build());
    }

    public static void warm() {
        try {
            com.mojang.blaze3d.systems.RenderSystem.getDevice().precompilePipeline(maskPipe());
            com.mojang.blaze3d.systems.RenderSystem.getDevice().precompilePipeline(flatPipe());
            com.mojang.blaze3d.systems.RenderSystem.getDevice().precompilePipeline(Halo.pipeline().get());
        } catch (Exception e) {
        }
    }

    private static RenderPipeline maskPipe() {
        if (mask == null) {
            mask = RenderPipeline.builder()
                .withLocation(Glow.id("pipeline/mask"))
                .withVertexShader(Glow.id("core/mask"))
                .withFragmentShader(Glow.id("core/mask"))
                .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                .withColorTargetState(color(NONE))
                .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
                .withVertexBinding(0, DefaultVertexFormat.ENTITY)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .build();
        }
        return mask;
    }

    private static RenderPipeline flatPipe() {
        if (flat == null) {
            flat = RenderPipeline.builder()
                .withLocation(Glow.id("pipeline/flat"))
                .withVertexShader(Glow.id("core/flat"))
                .withFragmentShader(Glow.id("core/mask"))
                .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                .withColorTargetState(color(NONE))
                .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .build();
        }
        return flat;
    }

    public static RenderType maskType(RenderType src) {
        return derive(MASKS, src, maskPipe(), "fireglow_mask");
    }

    public static RenderType flatType(RenderType src) {
        return derive(FLATS, src, flatPipe(), "fireglow_flat");
    }

    private static RenderType derive(Map<RenderType, RenderType> cache, RenderType src, RenderPipeline pipe, String name) {
        if (cache.containsKey(src)) return cache.get(src);
        Identifier atlas = atlasOf(src);
        RenderType t = atlas == null ? null : RenderType.create(name, RenderSetup.builder(pipe)
            .withTexture("Sampler0", atlas)
            .setOutputTarget(OUT)
            .createRenderSetup());
        cache.put(src, t);
        return t;
    }

    private static Identifier atlasOf(RenderType src) {
        GpuTextureView bound = null;
        for (PreparedRenderType.Texture t : src.prepare().textures()) {
            if ("Sampler0".equals(t.name())) bound = t.textureView();
        }
        if (bound == null) return null;
        TextureManager tm = Minecraft.getInstance().getTextureManager();
        if (bound == tm.getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView()) return TextureAtlas.LOCATION_BLOCKS;
        if (bound == tm.getTexture(TextureAtlas.LOCATION_ITEMS).getTextureView()) return TextureAtlas.LOCATION_ITEMS;
        return null;
    }

    public static Tex texture(String label, int w, int h) {
        GpuDevice device = RenderSystem.getDevice();
        GpuTexture t = device.createTexture(label, 15, GpuFormat.RGBA8_UNORM, w, h, 1, 1);
        return new Tex(t, device.createTextureView(t));
    }

    public static void draw(Pipe p, Tex out, String block, Buf ubo, Tex in, Tex mask) {
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "fireglow", out.view, Optional.empty())) {
            pass.setPipeline(p.pipeline);
            pass.setUniform(block, ubo.buffer);
            pass.bindTexture("InSampler", in.view, linear());
            if (mask != null) pass.bindTexture("MaskSampler", mask.view, nearest());
            pass.draw(3, 1, 0, 0);
        }
    }

    public static Buf ubo(String label, int size) {
        return new Buf(RenderSystem.getDevice().createBuffer(() -> label, GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, size));
    }

    public static void write(Buf ubo, float... values) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = stack.malloc(values.length * 4);
            for (float v : values) data.putFloat(v);
            data.flip();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(ubo.buffer.slice(), data);
        }
    }

    public static int guiScale() {
        return Minecraft.getInstance().getWindow().getGuiScale();
    }

    public static GpuSampler linear() {
        return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
    }

    public static GpuSampler nearest() {
        return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
    }

    private static final class Target extends RenderTarget {
        Target() {
            super("fireglow mask", true, GpuFormat.RGBA8_UNORM);
        }

        @Override
        public void createBuffers(int w, int h) {
            RenderSystem.assertOnRenderThread();
            GpuDevice device = RenderSystem.getDevice();
            this.width = w;
            this.height = h;
            this.colorTexture = device.createTexture(() -> this.label + " / Color", 15, this.format, w, h, 1, 1);
            this.colorTextureView = device.createTextureView(this.colorTexture);
        }

        @Override
        public GpuTexture getDepthTexture() {
            return main().getDepthTexture();
        }

        @Override
        public GpuTextureView getDepthTextureView() {
            return main().getDepthTextureView();
        }
    }
}
