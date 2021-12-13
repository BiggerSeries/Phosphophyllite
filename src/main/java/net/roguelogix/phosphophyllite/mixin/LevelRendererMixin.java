package net.roguelogix.phosphophyllite.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.roguelogix.phosphophyllite.quartz.internal.QuartzCore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    
    // line 1124-1125
    @Inject(method = "renderLevel", at = @At(value = "HEAD"))
    public void frameStart(PoseStack pMatrixStack, float pPartialTicks, long pFinishTimeNano, boolean pDrawBlockOutline, Camera pActiveRenderInfo, GameRenderer pGameRenderer, LightTexture pLightmap, Matrix4f pProjection, CallbackInfo ci) {
        QuartzCore.INSTANCE.frameStart(pMatrixStack, pPartialTicks, pFinishTimeNano, pDrawBlockOutline, pActiveRenderInfo, pGameRenderer, pLightmap, pProjection);
    }
    
    // line 1133-1134
    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;getPosition()Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 0
            )
    )
    public void lightUpdated(PoseStack pMatrixStack, float pPartialTicks, long pFinishTimeNano, boolean pDrawBlockOutline, Camera pActiveRenderInfo, GameRenderer pGameRenderer, LightTexture pLightmap, Matrix4f pProjection, CallbackInfo ci) {
        QuartzCore.INSTANCE.lightUpdated();
    }
    
    // line 1169-1170
    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V",
                    ordinal = 0
            )
    )
    public void preTerrainSetup(PoseStack pMatrixStack, float pPartialTicks, long pFinishTimeNano, boolean pDrawBlockOutline, Camera pActiveRenderInfo, GameRenderer pGameRenderer, LightTexture pLightmap, Matrix4f pProjection, CallbackInfo ci) {
        QuartzCore.INSTANCE.preTerrainSetup();
    }
    
    // line 1173-1174
    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLcom/mojang/math/Matrix4f;)V",
                    ordinal = 0
            )
    )
    public void preOpaque(PoseStack pMatrixStack, float pPartialTicks, long pFinishTimeNano, boolean pDrawBlockOutline, Camera pActiveRenderInfo, GameRenderer pGameRenderer, LightTexture pLightmap, Matrix4f pProjection, CallbackInfo ci) {
        QuartzCore.INSTANCE.preOpaque();
    }
    
    // line 1291-1292
    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V",
                    ordinal = 0
            )
    )
    public void endOpaque(PoseStack pMatrixStack, float pPartialTicks, long pFinishTimeNano, boolean pDrawBlockOutline, Camera pActiveRenderInfo, GameRenderer pGameRenderer, LightTexture pLightmap, Matrix4f pProjection, CallbackInfo ci) {
        QuartzCore.INSTANCE.endOpaque();
    }
    
    // line 1182-1183
    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPoseMatrix(Lcom/mojang/math/Matrix4f;)V",
                    ordinal = 1
            )
    )
    public void endTranslucent(PoseStack pMatrixStack, float pPartialTicks, long pFinishTimeNano, boolean pDrawBlockOutline, Camera pActiveRenderInfo, GameRenderer pGameRenderer, LightTexture pLightmap, Matrix4f pProjection, CallbackInfo ci) {
        QuartzCore.INSTANCE.endTranslucent();
    }
    
    // line 2414-2415
    @Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"))
    public void setSectionDirty(int x, int y, int z, boolean updateNow, CallbackInfo ci) {
        QuartzCore.INSTANCE.lightEngine.sectionDirty(x, y, z);
    }
}
