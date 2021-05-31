package net.roguelogix.phosphophyllite.blocks;

import mcp.MethodsReturnNonnullByDefault;
import net.roguelogix.phosphophyllite.quartz.QuartzMatrixStack;
import net.roguelogix.phosphophyllite.quartz.QuartzMesh;
import net.roguelogix.phosphophyllite.quartz.QuartzTESR;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PhosphophylliteOreQuartzTESR extends QuartzTESR.Renderer {
    
    @Override
    public void build(QuartzMesh.Builder builder) {
        QuartzMatrixStack stack = builder.getMatrixStack();
        builder.addVertex(null, 0, 0 , 0, 255, 255, 255, 255, 0, 0, 1, 0, 1, 0, 0);
        builder.addVertex(null, 0, 1 , 0, 255, 255, 255, 255, 0, 0, 1, 0, 1, 0, 0);
        builder.addVertex(null, 0, 0 , 1, 255, 255, 255, 255, 0, 0, 1, 0, 1, 0, 0);
    }
    
    @Override
    public void updateMatrices(float partialTicks) {
    
    }
}
