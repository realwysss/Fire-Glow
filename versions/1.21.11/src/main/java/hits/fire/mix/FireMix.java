package hits.fire.mix;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import hits.fire.Bloom;
import hits.fire.Fires;
import hits.fire.Gpu;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class FireMix {
    private static final Direction[] SIDES = Direction.values();

    @Shadow
    private ClientLevel level;

    @Inject(method = "submitBlockEntities", at = @At("TAIL"))
    private void fg$fires(PoseStack pose, LevelRenderState state, SubmitNodeStorage out, CallbackInfo ci) {
        int kinds = Bloom.kinds();
        if (kinds == 0 || level == null) return;
        RenderType m = Gpu.maskType(Sheets.cutoutBlockSheet());
        if (m == null) return;
        Vec3 cam = state.cameraRenderState.pos;
        RandomSource random = RandomSource.create();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos side = new BlockPos.MutableBlockPos();
        Fires.near(cam.x, cam.y, cam.z, Fires.RANGE, kinds, (x, y, z, kind) -> {
            at.set(x, y, z);
            BlockState block = level.getBlockState(at);
            if (Fires.kind(block) == 0 || block.getRenderShape() != RenderShape.MODEL) return;
            BlockStateModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(block);
            random.setSeed(block.getSeed(at));
            List<BlockModelPart> parts = model.collectParts(random);
            if (parts.isEmpty()) return;
            int open = 0;
            for (Direction d : SIDES) {
                if (Block.shouldRenderFace(block, level.getBlockState(side.setWithOffset(at, d)), d)) open |= 1 << d.ordinal();
            }
            int faces = open;
            Vec3 offset = block.getOffset(at);
            pose.pushPose();
            pose.translate(x - cam.x + offset.x, y - cam.y + offset.y, z - cam.z + offset.z);
            out.submitCustomGeometry(pose, m, (p, c) -> {
                for (BlockModelPart part : parts) {
                    for (Direction d : SIDES) {
                        if ((faces & 1 << d.ordinal()) != 0) emit(p, c, part.getQuads(d));
                    }
                    emit(p, c, part.getQuads(null));
                }
            });
            pose.popPose();
            Bloom.touch();
        });
    }

    private static void emit(PoseStack.Pose p, VertexConsumer c, List<BakedQuad> quads) {
        for (BakedQuad q : quads) c.putBulkData(p, q, 1f, 1f, 1f, 1f, 0xF000F0, OverlayTexture.NO_OVERLAY);
    }
}
