package net.roguelogix.phosphophyllite.quartz;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.roguelogix.phosphophyllite.quartz.internal.MatrixViews;
import net.roguelogix.phosphophyllite.repack.org.joml.Matrix3f;
import net.roguelogix.phosphophyllite.repack.org.joml.Matrix3fc;
import net.roguelogix.phosphophyllite.repack.org.joml.Matrix4f;
import net.roguelogix.phosphophyllite.repack.org.joml.Matrix4fc;

import java.util.ArrayList;
import java.util.function.BiConsumer;

public class QuartzMatrixStack extends MatrixStack {
    
    private final Entry entry = new Entry();
    
    public static final class Entry extends MatrixStack.Entry {
        private Matrix4f matrix;
        private Matrix3f normal;
        private final MatrixViews.Matrix4fView matrixView;
        private final MatrixViews.Matrix3fView normalView;
        
        private Entry() {
            this(new MatrixViews.Matrix4fView(), new MatrixViews.Matrix3fView());
        }
        
        private Entry(MatrixViews.Matrix4fView matrixView, MatrixViews.Matrix3fView normalView) {
            super(matrixView, normalView);
            this.matrixView = matrixView;
            this.normalView = normalView;
        }
        
        private void setMatrix(Matrix4f matrix) {
            this.matrix = matrix;
            matrixView.setMatrix(matrix);
        }
        
        private void setNormal(Matrix3f normal) {
            this.normal = normal;
            normalView.setMatrix(normal);
        }
        
        @Override
        public net.minecraft.util.math.vector.Matrix4f getMatrix() {
            matrixView.update();
            return matrixView;
        }
        
        @Override
        public net.minecraft.util.math.vector.Matrix3f getNormal() {
            return normalView;
        }
        
        public Matrix4fc matrix() {
            return this.matrix;
        }
        
        public Matrix3fc normal() {
            return this.normal;
        }
    }
    
    private int currentIndex = 0;
    private final ArrayList<Matrix4f> matrixStack = new ArrayList<>();
    private final ArrayList<Matrix3f> normalStack = new ArrayList<>();
    private final ArrayList<Matrix4fc> dynamicMatrixList = new ArrayList<>();
    private BiConsumer<Matrix4fc, Matrix4fc> dynamicPushCallback;
    private Runnable dynamicPopCallback;
    
    QuartzMatrixStack() {
        matrixStack.add(new Matrix4f());
        normalStack.add(new Matrix3f());
    }
    
    /**
     * Pushes a dynamic matrix onto the stack
     * translation and rotations done at this stack level will be performed to a matrix multiplied on the right side of this matrix
     *
     * @param matrix: the matrix object to be updated later, instance will be tracked inside Quartz
     */
    public void push(Matrix4f matrix) {
        currentIndex++;
        matrixStack.ensureCapacity(currentIndex + 1);
        normalStack.ensureCapacity(currentIndex + 1);
        while (matrixStack.size() <= currentIndex) {
            matrixStack.add(new Matrix4f());
            normalStack.add(new Matrix3f());
        }
        dynamicMatrixList.ensureCapacity(currentIndex + 1);
        while (dynamicMatrixList.size() <= currentIndex) {
            dynamicMatrixList.add(null);
        }
        // because we are tracking a dynamic matrix, all transforms need to be relative to it
        // previous static transforms will be flattened into it at render time
        matrixStack.get(currentIndex).identity();
        normalStack.get(currentIndex).identity();
        dynamicMatrixList.set(currentIndex, matrix);
        dynamicPushCallback.accept(matrix, matrixStack.get(currentIndex - 1));
    }
    
    /**
     * Pushes a static matrix onto the stack
     * works identically to MatrixStack#push
     */
    public void push() {
        currentIndex++;
        matrixStack.ensureCapacity(currentIndex + 1);
        normalStack.ensureCapacity(currentIndex + 1);
        while (matrixStack.size() <= currentIndex) {
            matrixStack.add(new Matrix4f());
            normalStack.add(new Matrix3f());
        }
        matrixStack.get(currentIndex).set(matrixStack.get(currentIndex - 1));
        normalStack.get(currentIndex).set(normalStack.get(currentIndex - 1));
    }
    
    public void pop() {
        if (currentIndex <= 0) {
            currentIndex = 0;
            return;
        }
        currentIndex--;
        if (dynamicMatrixList.set(currentIndex, null) != null) {
            dynamicPopCallback.run();
        }
    }
    
    @Override
    public MatrixStack.Entry getLast() {
        entry.setMatrix(matrixStack.get(currentIndex));
        return entry;
    }
    
    void dynamicPushCallback(BiConsumer<Matrix4fc, Matrix4fc> callback) {
        dynamicPushCallback = callback;
    }
    
    void dynamicPopCallback(Runnable callback) {
        dynamicPopCallback = callback;
    }
    
    public boolean clear() {
        return currentIndex == 0;
    }
}