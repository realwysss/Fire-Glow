package hits.fire.mix;

import hits.fire.Bloom;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class LevelMix {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void fg$begin(CallbackInfo ci) {
        Bloom.begin();
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"))
    private void fg$hand(CallbackInfo ci) {
        Bloom.hand();
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void fg$end(CallbackInfo ci) {
        Bloom.end();
    }
}
