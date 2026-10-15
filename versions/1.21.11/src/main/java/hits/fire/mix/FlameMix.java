package hits.fire.mix;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import hits.fire.Bloom;
import hits.fire.Fires;
import hits.fire.Gpu;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FlameFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.AtlasManager;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FlameFeatureRenderer.class)
abstract class FlameMix {
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FlameFeatureRenderer;renderFlame(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lorg/joml/Quaternionf;Lnet/minecraft/client/resources/model/AtlasManager;)V"))
    private void fg$mask(FlameFeatureRenderer self, PoseStack.Pose pose, MultiBufferSource buffers, EntityRenderState state, Quaternionf rot, AtlasManager atlas, Operation<Void> op) {
        RenderType m = Bloom.want(Fires.MOB) ? Gpu.maskType(Sheets.cutoutBlockSheet()) : null;
        if (m == null) {
            op.call(self, pose, buffers, state, rot, atlas);
            return;
        }
        op.call(self, pose.copy(), buffers, state, rot, atlas);
        op.call(self, pose, (MultiBufferSource) t -> buffers.getBuffer(m), state, rot, atlas);
        Bloom.touch();
    }
}
