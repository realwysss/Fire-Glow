package hits.fire.mix;

import com.mojang.blaze3d.vertex.PoseStack;
import hits.fire.Bloom;
import hits.fire.Fires;
import hits.fire.Gpu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class FireMix {
    @Inject(method = "submitBlockEntities", at = @At("TAIL"))
    private void fg$fires(PoseStack pose, LevelRenderState state, SubmitNodeCollector out, CallbackInfo ci) {
        int kinds = Bloom.kinds();
        if (kinds == 0) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        RenderType mask = Gpu.maskType(RenderTypes.entityCutoutCull(TextureAtlas.LOCATION_BLOCKS));
        if (level == null || mask == null) return;
        BlockStateModelSet models = mc.getModelManager().getBlockStateModelSet();
        RandomSource random = RandomSource.createThreadLocalInstance(0L);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        Vec3 cam = state.cameraRenderState.pos;
        Fires.near(cam.x, cam.y, cam.z, Fires.RANGE, kinds, (x, y, z, kind) -> {
            BlockState s = level.getBlockState(at.set(x, y, z));
            if (Fires.kind(s) == 0) return;
            List<BlockStateModelPart> parts = new ArrayList<>();
            random.setSeed(s.getSeed(at));
            models.get(s).collectParts(random, parts);
            if (parts.isEmpty()) return;
            pose.pushPose();
            pose.translate(x - cam.x, y - cam.y, z - cam.z);
            pose.translate(s.getOffset(at));
            out.submitBlockModel(pose, mask, parts, BlockModelRenderState.EMPTY_TINTS, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
            Bloom.touch();
        });
    }
}
