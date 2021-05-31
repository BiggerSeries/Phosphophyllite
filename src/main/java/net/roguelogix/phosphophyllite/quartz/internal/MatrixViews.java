package net.roguelogix.phosphophyllite.quartz.internal;


import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;
import net.roguelogix.phosphophyllite.repack.org.joml.Matrix3f;
import net.roguelogix.phosphophyllite.repack.org.joml.Matrix4f;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.FloatBuffer;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MatrixViews {
    
    public static class Matrix4fView extends net.minecraft.util.math.vector.Matrix4f {
        private Matrix4f matrix;
        
        public void setMatrix(Matrix4f matrix) {
            this.matrix = matrix;
        }
        
        public void update() {
            // Mojang directly access the values, so i need to have them mirror the underlying matrix
            // Mojang/MCP is Row major JOML is column major, hence these numbers dont directly line up
            this.m00 = matrix.m00();
            this.m01 = matrix.m10();
            this.m02 = matrix.m20();
            this.m03 = matrix.m30();
            this.m10 = matrix.m01();
            this.m11 = matrix.m11();
            this.m12 = matrix.m21();
            this.m13 = matrix.m31();
            this.m20 = matrix.m02();
            this.m21 = matrix.m12();
            this.m22 = matrix.m22();
            this.m23 = matrix.m32();
            this.m30 = matrix.m03();
            this.m31 = matrix.m13();
            this.m32 = matrix.m23();
            this.m33 = matrix.m33();
        }
        
        private void copyDown() {
            matrix.m00(this.m00);
            matrix.m10(this.m01);
            matrix.m20(this.m02);
            matrix.m30(this.m03);
            matrix.m01(this.m10);
            matrix.m11(this.m11);
            matrix.m21(this.m12);
            matrix.m31(this.m13);
            matrix.m02(this.m20);
            matrix.m12(this.m21);
            matrix.m22(this.m22);
            matrix.m32(this.m23);
            matrix.m03(this.m30);
            matrix.m13(this.m31);
            matrix.m23(this.m32);
            matrix.m33(this.m33);
        }
        
        public boolean equals(Object object) {
            if (object == this) {
                return true;
            }
            if (object instanceof Matrix4fView) {
                return matrix.equals(((Matrix4fView) object).matrix);
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            return matrix.hashCode();
        }
    
        @Override
        public String toString() {
            update();
            return super.toString();
        }
    
        @Override
        public void write(FloatBuffer floatBufferIn) {
            update();
            super.write(floatBufferIn);
        }
        
        @Override
        public void setIdentity() {
            matrix.identity();
            super.setIdentity();
        }
        
        @Override
        public float adjugateAndDet() {
            update();
            float det = super.adjugateAndDet();
            copyDown();
            return det;
        }
        
        @Override
        public void transpose() {
            matrix.transpose();
            update();
        }
        
        @Override
        public boolean invert() {
            update();
            matrix.invert();
            return super.invert();
        }
        
        @Override
        public void mul(net.minecraft.util.math.vector.Matrix4f matrix) {
            update();
            super.mul(matrix);
            copyDown();
        }
        
        @Override
        public void mul(Quaternion quaternion) {
            update();
            super.mul(quaternion);
            copyDown();
        }
        
        @Override
        public void mul(float scale) {
            matrix.scale(scale);
            update();
        }
        
        @Override
        public void translate(Vector3f vector) {
            matrix.translate(vector.getX(), vector.getY(), vector.getY());
            update();
        }
        
        @Override
        public void set(net.minecraft.util.math.vector.Matrix4f mat) {
            super.set(mat);
            copyDown();
        }
        
        @Override
        public void add(net.minecraft.util.math.vector.Matrix4f other) {
            super.add(other);
        }
        
        @Override
        public void multiplyBackward(net.minecraft.util.math.vector.Matrix4f other) {
            update();
            super.multiplyBackward(other);
            copyDown();
        }
        
        @Override
        public void setTranslation(float x, float y, float z) {
            update();
            super.setTranslation(x, y, z);
            copyDown();
        }
        
        @Override
        public net.minecraft.util.math.vector.Matrix4f copy() {
            update();
            return super.copy();
        }
    }
    
    public static class Matrix3fView extends net.minecraft.util.math.vector.Matrix3f {
        private Matrix3f matrix;
        
        public void setMatrix(Matrix3f matrix) {
            this.matrix = matrix;
        }
        
        public void update() {
            // Mojang directly access the values, so i need to have them mirror the underlying matrix
            // Mojang/MCP is Row major JOML is column major, hence these numbers dont directly line up
            this.m00 = matrix.m00();
            this.m01 = matrix.m10();
            this.m02 = matrix.m20();
            this.m10 = matrix.m01();
            this.m11 = matrix.m11();
            this.m12 = matrix.m21();
            this.m20 = matrix.m02();
            this.m21 = matrix.m12();
            this.m22 = matrix.m22();
        }
        
        private void copyDown() {
            matrix.m00(this.m00);
            matrix.m10(this.m01);
            matrix.m20(this.m02);
            matrix.m01(this.m10);
            matrix.m11(this.m11);
            matrix.m21(this.m12);
            matrix.m02(this.m20);
            matrix.m12(this.m21);
            matrix.m22(this.m22);
        }
        
        public void transpose() {
            matrix.transpose();
            update();
        }
        
        @Override
        public Triple<Quaternion, Vector3f, Quaternion> svdDecompose() {
            update();
            Triple<Quaternion, Vector3f, Quaternion> tripple = super.svdDecompose();
            copyDown();
            return tripple;
        }
        
        public boolean equals(Object object) {
            if (object == this) {
                return true;
            }
            if (object instanceof Matrix3fView) {
                return matrix.equals(((Matrix3fView) object).matrix);
            }
            return false;
        }
    
        @Override
        public int hashCode() {
            return matrix.hashCode();
        }
    
        @Override
        public void set(net.minecraft.util.math.vector.Matrix3f p_226114_1_) {
            super.set(p_226114_1_);
            copyDown();
        }
    
        @Override
        public String toString() {
            update();
            return super.toString();
        }
    
        // its some weird set function
        @Override
        public void func_232605_a_(int p_232605_1_, int p_232605_2_, float p_232605_3_) {
            update();
            super.func_232605_a_(p_232605_1_, p_232605_2_, p_232605_3_);
            copyDown();
        }
    
        @Override
        public void mul(net.minecraft.util.math.vector.Matrix3f p_226118_1_) {
            update();
            super.mul(p_226118_1_);
            copyDown();
        }
    
        @Override
        public void mul(Quaternion p_226115_1_) {
            update();
            super.mul(p_226115_1_);
            copyDown();
        }
    
        @Override
        public void mul(float scale) {
            matrix.scale(scale);
            update();
        }
    
        @Override
        public net.minecraft.util.math.vector.Matrix3f copy() {
            update();
            return super.copy();
        }
    
        @Override
        public void multiplyBackward(net.minecraft.util.math.vector.Matrix3f other) {
            update();
            super.multiplyBackward(other);
            copyDown();
        }
    }
}
