package net.roguelogix.phosphophyllite.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.roguelogix.phosphophyllite.mixinhelpers.LevelRenderStages;
import net.roguelogix.phosphophyllite.quartz.internal.common.light.LightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLcom/mojang/math/Matrix4f;)V",
                    ordinal = 0
            )
    )
    public void renderFirst(PoseStack pMatrixStack, float pPartialTicks, long pFinishTimeNano, boolean pDrawBlockOutline, Camera pActiveRenderInfo, GameRenderer pGameRenderer, LightTexture pLightmap, Matrix4f pProjection, CallbackInfo ci) {
        LevelRenderStages.renderFirst(pMatrixStack, pPartialTicks, pFinishTimeNano, pDrawBlockOutline, pActiveRenderInfo, pGameRenderer, pLightmap, pProjection);
    }
    
    @Inject(
            method = "setSectionDirty(IIIZ)V",
            at = @At("HEAD")
    )
    
    public void setSectionDirty(int x, int y, int z, boolean updateNow, CallbackInfo ci){
        // all rebuilds here are lazy, *fite me*
        LightEngine.sectionDirty(x, y, z);
    }
}
