package hits.fire.mix;

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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
abstract class ScreenMix {
    @Unique
    private static boolean fg$inside;

    @Shadow
    private static void renderFire(PoseStack pose, MultiBufferSource buffers, TextureAtlasSprite sprite) {
    }

    @Inject(method = "renderFire", at = @At("HEAD"))
    private static void fg$mask(PoseStack pose, MultiBufferSource buffers, TextureAtlasSprite sprite, CallbackInfo ci) {
        if (fg$inside || !Bloom.want(Fires.SCREEN)) return;
        RenderType flat = Gpu.flatType(RenderTypes.fireScreenEffect(sprite.atlasLocation()));
        if (flat == null) return;
        fg$inside = true;
        renderFire(pose, t -> buffers.getBuffer(flat), sprite);
        fg$inside = false;
        Bloom.touch();
    }
}
