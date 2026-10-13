package hits.fire.mix;

import hits.fire.Fires;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
abstract class BlockMix {
    @Inject(method = "sendBlockUpdated", at = @At("HEAD"))
    private void fg$block(BlockPos pos, BlockState old, BlockState now, int flags, CallbackInfo ci) {
        Fires.update((ClientLevel) (Object) this, pos, now);
    }
}
