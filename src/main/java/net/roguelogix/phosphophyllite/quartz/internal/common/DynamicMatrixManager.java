package net.roguelogix.phosphophyllite.quartz.internal.common;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.roguelogix.phosphophyllite.quartz.Quartz;
import net.roguelogix.phosphophyllite.quartz.QuartzDynamicMatrix;
import net.roguelogix.phosphophyllite.quartz.internal.QuartzCore;
import net.roguelogix.phosphophyllite.repack.org.joml.Matrix4f;
import net.roguelogix.phosphophyllite.repack.org.joml.Matrix4fc;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3f;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;
import net.roguelogix.phosphophyllite.util.MethodsReturnNonnullByDefault;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DynamicMatrixManager implements GLDeletable {
    
    public class DynamicMatrix implements QuartzDynamicMatrix {
        
        final GLBuffer.Allocation allocation;
        final Matrix4f transformMatrix = new Matrix4f();
        final Matrix4f normalMatrix = new Matrix4f();
        
        final DynamicMatrix parent;
        final ObjectArrayList<DynamicMatrix> childMatrices = new ObjectArrayList<>();
        InternalMatrixUpdateCall updateCall;
        
        private DynamicMatrix(GLBuffer.Allocation alloc, @Nullable DynamicMatrix parent) {
            allocation = alloc;
            this.parent = parent;
            if (parent != null) {
                parent.childMatrices.add(this);
            } else {
                rootMatrices.add(this);
            }
        }
        
        @Override
        public void dispose() {
            allocation.delete();
            if (parent != null) {
                parent.childMatrices.remove(this);
            } else {
                rootMatrices.remove(this);
            }
            var removed = updateCalls.pop();
            if (removed != updateCall) {
                int index = updateCalls.indexOf(updateCall);
                if (index != -1) {
                    updateCalls.set(index, removed);
                } else {
                    updateCalls.add(removed);
                }
            }
        }
        
        @Override
        public void write(Matrix4fc matrixData) {
            transformMatrix.set(matrixData);
        }
        
        public void propagateToChildren() {
            if (parent != null) {
                // transform = parent.transform * transform
                parent.transformMatrix.mul(transformMatrix, transformMatrix);
            }
            transformMatrix.normal(normalMatrix);
            transformMatrix.get(0, allocation.buffer());
            normalMatrix.get(MagicNumbers.MATRIX_4F_BYTE_SIZE, allocation.buffer());
            for (int i = 0; i < childMatrices.size(); i++) {
                childMatrices.get(i).propagateToChildren();
            }
        }
        
        public int id() {
            return allocation.offset() / MagicNumbers.MATRIX_4F_BYTE_SIZE_2;
        }
        
        private DynamicMatrixManager manager() {
            return DynamicMatrixManager.this;
        }
    }
    
    private interface InternalMatrixUpdateCall {
        void accept(long nanos, float partialTicks, Vector3i playerBlock, Vector3f playerPartialBlock);
    }
    
    private final GLBuffer glBuffer;
    private final ObjectArrayList<InternalMatrixUpdateCall> updateCalls = new ObjectArrayList<>();
    private final ObjectArrayList<DynamicMatrix> rootMatrices = new ObjectArrayList<>();
    
    public DynamicMatrixManager() {
        this.glBuffer = QuartzCore.instance().allocBuffer(true, MagicNumbers.MATRIX_4F_BYTE_SIZE_2);
    }
    
    @Override
    public void delete() {
        glBuffer.delete();
    }
    
    public DynamicMatrix alloc(@Nullable QuartzDynamicMatrix parentTransform, @Nullable Quartz.DynamicMatrixUpdateFunc updateFunc) {
        DynamicMatrix parentMatrix = null;
        if (parentTransform != null) {
            if (parentTransform instanceof DynamicMatrix parent && parent.manager() == this) {
                parentMatrix = parent;
            } else {
                throw new IllegalArgumentException("Parent matrix must be from the same manager");
            }
        }
        final var matrix = new DynamicMatrix(glBuffer.alloc(MagicNumbers.MATRIX_4F_BYTE_SIZE_2, MagicNumbers.MATRIX_4F_BYTE_SIZE_2), parentMatrix);
        if (updateFunc != null) {
            InternalMatrixUpdateCall updateCall = (nanos, partialTicks, block, subBlock) -> updateFunc.accept(matrix, nanos, partialTicks, block, subBlock);
            matrix.updateCall = updateCall;
            updateCalls.add(updateCall);
        }
        return matrix;
    }
    
    public void updateAll(long nanos, float partialTicks, Vector3i playerBlock, Vector3f playerPartialBlock) {
        for (int i = 0; i < updateCalls.size(); i++) {
            updateCalls.get(i).accept(nanos, partialTicks, playerBlock, playerPartialBlock);
        }
        for (int i = 0; i < rootMatrices.size(); i++) {
            rootMatrices.get(i).propagateToChildren();
        }
        glBuffer.flushAll();
    }
    
    public GLBuffer buffer() {
        return glBuffer;
    }
    
    public boolean owns(DynamicMatrix matrix) {
        return matrix.manager() == this;
    }
}
