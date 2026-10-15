package hits.fire.mix;

import hits.fire.Frost;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class FrostMix {
    @Inject(method = "processBlurEffect", at = @At("HEAD"), cancellable = true)
    private void fg$frost(CallbackInfo ci) {
        if (Frost.run()) ci.cancel();
    }
}
