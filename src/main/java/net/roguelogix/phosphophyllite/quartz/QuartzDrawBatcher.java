package net.roguelogix.phosphophyllite.quartz;

import net.roguelogix.phosphophyllite.repack.org.joml.AABBi;
import net.roguelogix.phosphophyllite.repack.org.joml.Matrix4fc;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3ic;

import javax.annotation.Nullable;

public interface QuartzDrawBatcher extends QuartzDisposable {
    
    interface Instance extends QuartzDisposable {
        void updateDynamicMatrix(@Nullable QuartzDynamicMatrix newDynamicMatrix);
        
        void updateStaticMatrix(Matrix4fc newStaticMatrix);
        
        void updateDynamicLight(@Nullable QuartzDynamicLight newDynamicLight);
    }
    
    /**
     * DynamicMatrix and DynamicLight must be instances created by this draw batcher
     *
     * @param position
     * @param mesh
     * @param dynamicMatrix
     * @param staticMatrix
     * @param light
     * @return
     */
    @Nullable
    Instance createInstance(Vector3ic position, QuartzStaticMesh mesh, @Nullable QuartzDynamicMatrix dynamicMatrix, Matrix4fc staticMatrix, @Nullable QuartzDynamicLight light,  @Nullable QuartzDynamicLight.Type lightType);
    
    /**
     * Matrix must be manually disposed of
     * Non-disposed matrices are automatically disposed of when batcher is disposed
     * @return
     */
    QuartzDynamicMatrix createDynamicMatrix(@Nullable QuartzDynamicMatrix parentTransform, @Nullable Quartz.DynamicMatrixUpdateFunc updateFunc);
    
    default QuartzDynamicMatrix createDynamicMatrix(@Nullable Quartz.DynamicMatrixUpdateFunc updateFunc){
        return createDynamicMatrix(null, updateFunc);
    }
    
    QuartzDynamicLight createLight(Vector3ic lightPosition, QuartzDynamicLight.Type lightType);
    
    void setCullAABB(AABBi aabb);
    
    void setEnabled(boolean enabled);
}
