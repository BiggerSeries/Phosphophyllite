package net.roguelogix.phosphophyllite.mixinhelpers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.roguelogix.phosphophyllite.quartz.internal.QuartzCore;

public class LevelRenderStages {
    public static void renderFirst(PoseStack pMatrixStack, float pPartialTicks, long pFinishTimeNano, boolean pDrawBlockOutline, Camera pActiveRenderInfo, GameRenderer pGameRenderer, LightTexture pLightmap, Matrix4f pProjection) {
        QuartzCore.instance().drawFirst(pMatrixStack, pPartialTicks, pFinishTimeNano, pDrawBlockOutline, pActiveRenderInfo, pGameRenderer, pLightmap, pProjection);
    }
}
