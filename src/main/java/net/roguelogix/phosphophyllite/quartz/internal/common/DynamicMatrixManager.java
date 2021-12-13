package net.roguelogix.phosphophyllite.quartz.internal.common;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.roguelogix.phosphophyllite.quartz.DynamicMatrix;
import net.roguelogix.phosphophyllite.quartz.internal.Buffer;
import net.roguelogix.phosphophyllite.quartz.internal.MagicNumbers;
import net.roguelogix.phosphophyllite.quartz.internal.QuartzCore;
import net.roguelogix.phosphophyllite.repack.org.joml.Matrix4f;
import net.roguelogix.phosphophyllite.repack.org.joml.Matrix4fc;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3f;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;
import net.roguelogix.phosphophyllite.util.MethodsReturnNonnullByDefault;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DynamicMatrixManager implements DynamicMatrix.Manager {
    
    public static class Matrix implements DynamicMatrix {
        private final Buffer.Allocation allocation;
        private final Matrix4f transformMatrix = new Matrix4f();
        private final Matrix4f normalMatrix = new Matrix4f();
        private final ObjectArrayList<WeakReference<Matrix>> childMatrices = new ObjectArrayList<>();
        @Nullable
        private final DynamicMatrix.UpdateFunc updateFunc;
        
        public Matrix(Buffer.Allocation allocation, @Nullable UpdateFunc updateFunc, ObjectArrayList<WeakReference<Matrix>> matrixList) {
            this.allocation = allocation;
            this.updateFunc = updateFunc;
            final var ref = new WeakReference<>(this);
            matrixList.add(ref);
            QuartzCore.CLEANER.register(this, () -> {
                synchronized (matrixList) {
                    matrixList.remove(ref);
                }
            });
        }
        
        @Override
        public void write(Matrix4fc matrixData) {
            transformMatrix.set(matrixData);
        }
        
        public void update(long nanos, float partialTicks, Vector3i playerBlock, Vector3f playerPartialBlock) {
            if (updateFunc != null) {
                updateFunc.accept(this, nanos, partialTicks, playerBlock, playerPartialBlock);
            }
            transformMatrix.normal(normalMatrix);
            transformMatrix.get(0, allocation.buffer());
            normalMatrix.get(MagicNumbers.MATRIX_4F_BYTE_SIZE, allocation.buffer());
            synchronized (childMatrices) {
                for (int i = 0; i < childMatrices.size(); i++) {
                    var mat = childMatrices.get(i).get();
                    if (mat != null) {
                        mat.update(nanos, partialTicks, playerBlock, playerPartialBlock);
                    }
                }
            }
        }
    
        public int id() {
            return allocation.offset() / MagicNumbers.MATRIX_4F_BYTE_SIZE_2;
        }
    }
    
    private final Buffer buffer;
    private final ObjectArrayList<WeakReference<Matrix>> rootMatrices = new ObjectArrayList<>();
    
    public DynamicMatrixManager(Buffer buffer) {
        this.buffer = buffer;
    }
    
    @Override
    public DynamicMatrix createMatrix(@Nullable DynamicMatrix.UpdateFunc updateFunc, @Nullable DynamicMatrix parent) {
        Matrix parentMatrix = null;
        if (parent != null) {
            if (parent instanceof Matrix parentMat && owns(parentMatrix)) {
                parentMatrix = parentMat;
            } else {
                throw new IllegalArgumentException("Parent matrix must be from the same manager");
            }
        }
        final var list = parentMatrix == null ? rootMatrices : ((Matrix) parent).childMatrices;
        return new Matrix(buffer.alloc(MagicNumbers.MATRIX_4F_BYTE_SIZE_2, MagicNumbers.MATRIX_4F_BYTE_SIZE_2), updateFunc, list);
    }
    
    @Override
    public boolean owns(@Nullable DynamicMatrix dynamicMatrix) {
        if (dynamicMatrix instanceof Matrix mat) {
            return mat.allocation.allocator() == buffer;
        }
        return false;
    }
    
    public void updateAll(long nanos, float partialTicks, Vector3i playerBlock, Vector3f playerPartialBlock) {
        synchronized (rootMatrices) {
            for (int i = 0; i < rootMatrices.size(); i++) {
                var mat = rootMatrices.get(i).get();
                if (mat != null) {
                    mat.update(nanos, partialTicks, playerBlock, playerPartialBlock);
                }
            }
        }
        buffer.dirtyAll();
    }
}
