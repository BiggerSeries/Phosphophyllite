package net.roguelogix.phosphophyllite.quartz;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

public interface QuartzStaticMesh extends QuartzDisposable {
    interface Builder {
        MultiBufferSource bufferSource();
        
        PoseStack matrixStack();
    }
}
