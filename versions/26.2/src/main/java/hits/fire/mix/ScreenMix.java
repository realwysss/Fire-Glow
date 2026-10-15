package hits.fire.mix;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import hits.fire.Bloom;
import hits.fire.Fires;
import hits.fire.Gpu;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ScreenEffectRenderer.class)
abstract class ScreenMix {
    @WrapOperation(method = "submitFire", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitCustomGeometry(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeCollector$CustomGeometryRenderer;)V"))
    private static void fg$mask(SubmitNodeCollector out, PoseStack pose, RenderType type, SubmitNodeCollector.CustomGeometryRenderer quads, Operation<Void> op) {
        op.call(out, pose, type, quads);
        if (!Bloom.want(Fires.SCREEN)) return;
        RenderType flat = Gpu.flatType(type);
        if (flat == null) return;
        out.submitCustomGeometry(pose, flat, quads);
        Bloom.touch();
    }
}
