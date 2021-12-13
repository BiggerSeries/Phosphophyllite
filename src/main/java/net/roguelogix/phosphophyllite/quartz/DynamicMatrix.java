package net.roguelogix.phosphophyllite.quartz;

import net.roguelogix.phosphophyllite.repack.org.joml.Matrix4fc;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3fc;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3ic;

import javax.annotation.Nullable;

public interface DynamicMatrix {
    void write(Matrix4fc matrixData);
    
    interface UpdateFunc {
        void accept(DynamicMatrix matrix, long nanoSinceLastFrame, float partialTicks, Vector3ic playerBlock, Vector3fc playerPartialBlock);
    }
    
    interface Manager {
        default DynamicMatrix createMatrix(UpdateFunc updateFunc) {
            return createMatrix(updateFunc, null);
        }
        
        DynamicMatrix createMatrix(@Nullable UpdateFunc updateFunc, @Nullable DynamicMatrix parent);
        
        boolean owns(@Nullable DynamicMatrix dynamicMatrix);
    }
}
