package net.roguelogix.phosphophyllite.quartz;

import net.roguelogix.phosphophyllite.repack.org.joml.AABBi;
import net.roguelogix.phosphophyllite.repack.org.joml.Matrix4fc;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3ic;

import javax.annotation.Nullable;

public interface DrawBatch {
    
    interface Instance {
        void updateDynamicMatrix(@Nullable DynamicMatrix newDynamicMatrix);
        
        void updateStaticMatrix(@Nullable Matrix4fc newStaticMatrix);
        
        void updateDynamicLight(@Nullable DynamicLight newDynamicLight);
        
        void delete();
    }
    
    /**
     * DynamicMatrix and DynamicLight must be instances created by this draw batch
     */
    @Nullable
    Instance createInstance(Vector3ic position, StaticMesh mesh, @Nullable DynamicMatrix dynamicMatrix, @Nullable Matrix4fc staticMatrix, @Nullable DynamicLight light, @Nullable DynamicLight.Type lightType);
    
    interface InstanceBatch {
        void updateMesh(StaticMesh mesh);
        
        @Nullable
        Instance createInstance(Vector3ic position, @Nullable DynamicMatrix dynamicMatrix, @Nullable Matrix4fc staticMatrix, @Nullable DynamicLight light, @Nullable DynamicLight.Type lightType);
    }
    
    @Nullable
    InstanceBatch createInstanceBatch(StaticMesh mesh);
    
    DynamicMatrix createDynamicMatrix(@Nullable DynamicMatrix parentTransform, @Nullable DynamicMatrix.UpdateFunc updateFunc);
    
    default DynamicMatrix createDynamicMatrix(@Nullable DynamicMatrix.UpdateFunc updateFunc) {
        return createDynamicMatrix(null, updateFunc);
    }
    
    DynamicLight createLight(Vector3ic lightPosition, DynamicLight.Type lightType);
    
    void setCullAABB(AABBi aabb);
    
    void setEnabled(boolean enabled);
    
    boolean isEmpty();
}
