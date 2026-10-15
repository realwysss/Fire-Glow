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
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class FireMix {
    @Unique
    private static final int[] NO_TINT = new int[0];
    @Unique
    private final RandomSource fg$random = RandomSource.create();
    @Unique
    private final BlockPos.MutableBlockPos fg$pos = new BlockPos.MutableBlockPos();
    @Shadow
    private ClientLevel level;

    @Inject(method = "submitBlockEntities", at = @At("TAIL"))
    private void fg$fires(PoseStack pose, LevelRenderState state, SubmitNodeStorage out, CallbackInfo ci) {
        int kinds = Bloom.kinds();
        if (kinds == 0 || level == null) return;
        RenderType mask = Gpu.maskType(Sheets.cutoutBlockSheet());
        if (mask == null) return;
        Vec3 cam = state.cameraRenderState.pos;
        Frustum frustum = state.cameraRenderState.cullFrustum;
        BlockStateModelSet models = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        Fires.near(cam.x, cam.y, cam.z, Fires.RANGE, kinds, (x, y, z, k) -> {
            BlockState s = level.getBlockState(fg$pos.set(x, y, z));
            if (Fires.kind(s) == 0 || !frustum.isVisible(new AABB(x, y, z, x + 1, y + 1, z + 1))) return;
            List<BlockStateModelPart> parts = new ArrayList<>();
            fg$random.setSeed(s.getSeed(fg$pos));
            models.get(s).collectParts(fg$random, parts);
            if (parts.isEmpty()) return;
            pose.pushPose();
            pose.translate(x - cam.x, y - cam.y, z - cam.z);
            out.submitBlockModel(pose, mask, parts, NO_TINT, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
            Bloom.touch();
        });
    }
}
