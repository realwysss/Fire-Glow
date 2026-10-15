package hits.fire.mix;

import com.mojang.blaze3d.vertex.PoseStack;
import hits.fire.Bloom;
import hits.fire.Fires;
import hits.fire.Gpu;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FlameFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FlameFeatureRenderer.class)
abstract class FlameMix {
    @Unique
    private boolean fg$inside;

    @Shadow
    private void renderFlame(PoseStack.Pose pose, MultiBufferSource buffers, EntityRenderState state, Quaternionf rotation, AtlasManager atlas) {
    }

    @Inject(method = "renderFlame", at = @At("HEAD"))
    private void fg$mask(PoseStack.Pose pose, MultiBufferSource buffers, EntityRenderState state, Quaternionf rotation, AtlasManager atlas, CallbackInfo ci) {
        if (fg$inside || !Bloom.want(Fires.MOB)) return;
        RenderType mask = Gpu.maskType(Sheets.cutoutBlockSheet());
        if (mask == null) return;
        fg$inside = true;
        renderFlame(pose.copy(), t -> buffers.getBuffer(mask), state, rotation, atlas);
        fg$inside = false;
        Bloom.touch();
    }
}
