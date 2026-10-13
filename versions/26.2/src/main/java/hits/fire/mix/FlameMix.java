package hits.fire.mix;

import com.mojang.blaze3d.vertex.VertexConsumer;
import hits.fire.Bloom;
import hits.fire.Fires;
import hits.fire.Gpu;
import java.util.List;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FlameFeatureRenderer;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FlameFeatureRenderer.class)
abstract class FlameMix extends RenderTypeFeatureRenderer<FlameFeatureRenderer.Submit> {
    @Shadow
    private void prepare(FlameFeatureRenderer.Submit submit, VertexConsumer buffer, TextureAtlasSprite fire1, TextureAtlasSprite fire2) {
    }

    @Inject(method = "buildGroup", at = @At("HEAD"))
    private void fg$mask(FeatureFrameContext context, List<FlameFeatureRenderer.Submit> submits, CallbackInfo ci) {
        if (!Bloom.want(Fires.MOB)) return;
        RenderType mask = Gpu.maskType(RenderTypes.entityCutoutCull(TextureAtlas.LOCATION_BLOCKS));
        if (mask == null) return;
        VertexConsumer out = this.getVertexBuilder(mask);
        TextureAtlasSprite a = context.atlasManager().get(ModelBakery.FIRE_0);
        TextureAtlasSprite b = context.atlasManager().get(ModelBakery.FIRE_1);
        for (FlameFeatureRenderer.Submit s : submits) {
            prepare(new FlameFeatureRenderer.Submit(s.pose().copy(), s.entityRenderState(), s.rotation()), out, a, b);
        }
        Bloom.touch();
    }
}
