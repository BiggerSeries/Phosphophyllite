package net.roguelogix.phosphophyllite.quartz.internal.gl33;

import net.minecraft.client.renderer.RenderType;
import net.roguelogix.phosphophyllite.quartz.internal.common.DrawInfo;
import net.roguelogix.phosphophyllite.quartz.QuartzDynamicLight;
import net.roguelogix.phosphophyllite.quartz.QuartzDynamicMatrix;
import net.roguelogix.phosphophyllite.quartz.QuartzStaticMesh;
import net.roguelogix.phosphophyllite.quartz.internal.QuartzCore;
import net.roguelogix.phosphophyllite.quartz.internal.common.GLBuffer;
import net.roguelogix.phosphophyllite.repack.org.joml.Matrix4fc;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3ic;
import net.roguelogix.phosphophyllite.util.MethodsReturnNonnullByDefault;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class GL33Core extends QuartzCore {
    
    private final GL33MainProgram renderPassProgram = new GL33MainProgram();
    private final GL33InstancedRendering instancedRendering = new GL33InstancedRendering(staticMeshManager, dynamicMatrixManager, dynamicLightManager, renderPassProgram);
    
    @Override
    public void delete() {
        instancedRendering.delete();
        renderPassProgram.delete();
        super.delete();
    }
    
    @Override
    public void drawFirst(DrawInfo drawInfo) {
        instancedRendering.draw(drawInfo);
    }
    
    @Override
    public void registerRenderType(RenderType type) {
        instancedRendering.registerRenderType(type);
    }
    
    @Override
    public Collection<RenderType> registeredRenderTypes() {
        return instancedRendering.registeredRenderTypes();
    }
    
    @Override
    public int registerStaticMeshInstance(QuartzStaticMesh mesh, Vector3ic position, @Nullable QuartzDynamicMatrix dynamicMatrix, Matrix4fc staticTransform, QuartzDynamicLight light) {
        return instancedRendering.addInstance(mesh, position, dynamicMatrix == null ? DYNAMIC_MATRIX_0 : dynamicMatrix, staticTransform, light);
    }
    
    @Override
    public void unregisterStaticMeshInstance(int handle) {
        instancedRendering.removeInstance(handle);
    }
    
    @Override
    public GLBuffer allocBuffer(boolean dynamic) {
        return new GL33Buffer(dynamic);
    }
    
    @Override
    public GLBuffer allocBuffer(boolean dynamic, int size) {
        return new GL33Buffer(dynamic, size);
    }
}
