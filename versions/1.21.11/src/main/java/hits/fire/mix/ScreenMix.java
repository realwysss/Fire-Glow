package hits.fire.mix;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import hits.fire.Bloom;
import hits.fire.Fires;
import hits.fire.Gpu;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ScreenEffectRenderer.class)
abstract class ScreenMix {
    @WrapOperation(method = "renderScreenEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;renderFire(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V"))
    private void fg$mask(PoseStack pose, MultiBufferSource buffers, TextureAtlasSprite sprite, Operation<Void> op) {
        op.call(pose, buffers, sprite);
        if (!Bloom.want(Fires.SCREEN)) return;
        RenderType m = Gpu.flatType(RenderTypes.fireScreenEffect(sprite.atlasLocation()));
        if (m == null) return;
        op.call(pose, (MultiBufferSource) t -> buffers.getBuffer(m), sprite);
        Bloom.touch();
    }
}
