package net.roguelogix.phosphophyllite.quartz;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.sun.jna.ptr.ByteByReference;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.Atlases;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.roguelogix.phosphophyllite.repack.org.joml.Matrix3f;
import net.roguelogix.phosphophyllite.repack.org.joml.Matrix4f;
import net.roguelogix.phosphophyllite.repack.org.joml.Matrix4fc;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;


@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class QuartzMesh {
    
    // VERTEX DATA FORMAT
    // float32 X, float32 Y, float32 Z
    // int32 matrixID
    // int32 R, int32 G, int32 B, int32 A
    // float32 texU, float32 texV
    // int32 lightU, int32 lightV
    // float32 normalX, float32 normalY, float32 normalZ
    // 4 bytes padding to 64 byte total
    private final int BYTES_PER_VERTEX = 64;
    private final ByteBuffer vertexData;
    private final FloatBuffer translationMatrixData;
    private final FloatBuffer normalMatrixData;
    
    private class MatrixTreeNode {
        final int localID;
        final Matrix4fc staticMatrix;
        final Matrix4fc dynamicMatrix;
        final ArrayList<MatrixTreeNode> subNodes = new ArrayList<>();
        
        private final Matrix4f outputMatrix = new Matrix4f();
        private final Matrix3f outputNormalMatrix = new Matrix3f();
    
        private MatrixTreeNode(int localID, Matrix4fc staticMatrix, Matrix4fc dynamicMatrix) {
            this.localID = localID;
            this.staticMatrix = staticMatrix;
            this.dynamicMatrix = dynamicMatrix;
        }
    
        void update(Matrix4fc baseTransform) {
            staticMatrix.mul(dynamicMatrix, outputMatrix);
            outputMatrix.mulLocal(baseTransform);
            outputMatrix.normal(outputNormalMatrix);
            outputMatrix.get(localID * 16, translationMatrixData);
            outputNormalMatrix.get(localID * 9, normalMatrixData);
            subNodes.forEach(o -> o.update(outputMatrix));
        }
    }
    
    final MatrixTreeNode headNode;
    
    public QuartzMesh(Builder.MeshSection mainSection) {
        int meshCount = 0;
        int meshSize = 0;
        {
            ArrayList<Builder.MeshSection> sections = new ArrayList<>();
            sections.add(mainSection);
            while (!sections.isEmpty()) {
                Builder.MeshSection section = sections.remove(0);
                meshCount++;
                meshSize += section.vertexData.size();
                sections.addAll(section.subSections);
            }
        }
    
        translationMatrixData = MemoryUtil.memAllocFloat(meshCount * 16);
        normalMatrixData = MemoryUtil.memAllocFloat(meshCount * 9);
        vertexData = MemoryUtil.memAlloc(meshSize * BYTES_PER_VERTEX);
        
        int[] nextID = new int[2];
        Function<?, ?>[] builderFunc = new Function<?, ?>[1];
        builderFunc[0] = (Builder.MeshSection section) -> {
            final int matrixID = nextID[0]++;
            MatrixTreeNode node = new MatrixTreeNode(matrixID, section.staticMatrix, section.staticMatrix);
            
            final int baseVertexPosition = nextID[1];
            nextID[1] += section.vertexData.size() * BYTES_PER_VERTEX;
    
            int currentVertexPosition = baseVertexPosition;
            for (Builder.MeshSection.VertexData vertex : section.vertexData) {
                // see vertex data format description at the top of the file
                //noinspection PointlessArithmeticExpression
                vertexData.putFloat(currentVertexPosition + 0, vertex.x);
                vertexData.putFloat(currentVertexPosition + 4, vertex.y);
                vertexData.putFloat(currentVertexPosition + 12, vertex.z);
                vertexData.putInt  (currentVertexPosition + 16, matrixID);
                vertexData.putInt  (currentVertexPosition + 20, vertex.r);
                vertexData.putInt  (currentVertexPosition + 24, vertex.g);
                vertexData.putInt  (currentVertexPosition + 28, vertex.b);
                vertexData.putInt  (currentVertexPosition + 32, vertex.a);
                vertexData.putFloat(currentVertexPosition + 36, vertex.u);
                vertexData.putFloat(currentVertexPosition + 40, vertex.v);
                vertexData.putInt  (currentVertexPosition + 44, vertex.lu);
                vertexData.putInt  (currentVertexPosition + 48, vertex.lv);
                vertexData.putFloat(currentVertexPosition + 52, vertex.nx);
                vertexData.putFloat(currentVertexPosition + 56, vertex.ny);
                vertexData.putFloat(currentVertexPosition + 60, vertex.nz);
                currentVertexPosition += BYTES_PER_VERTEX;
            }
            
            for (Builder.MeshSection subSection : section.subSections) {
                //noinspection unchecked
                node.subNodes.add(((Function<Builder.MeshSection, MatrixTreeNode>)builderFunc[0]).apply(subSection));
            }
            
            return node;
        };
        //noinspection unchecked
        Function<Builder.MeshSection, MatrixTreeNode> func = (Function<Builder.MeshSection, MatrixTreeNode>) builderFunc[0];
        headNode = func.apply(mainSection);
    }
    
    @Override
    protected void finalize() {
        MemoryUtil.memFree(vertexData);
        MemoryUtil.memFree(translationMatrixData);
        MemoryUtil.memFree(normalMatrixData);
    }
    
    public static class Builder implements IRenderTypeBuffer {
        
        private final QuartzMatrixStack matrixStack = new QuartzMatrixStack();
        
        private final HashMap<RenderType, IVertexBuilder> builders = new HashMap<>();
        
        {
            builders.put(RenderType.getSolid(), new QuartzVertexBuilder(this, RenderType.getSolid()));
            builders.put(RenderType.getCutout(), new QuartzVertexBuilder(this, RenderType.getCutout()));
            builders.put(RenderType.getCutoutMipped(), new QuartzVertexBuilder(this, RenderType.getCutoutMipped()));
            builders.put(Atlases.getCutoutBlockType(), new QuartzVertexBuilder(this, Atlases.getCutoutBlockType()));
        }
        
        ArrayList<MeshSection> meshSections = new ArrayList<>();
        MeshSection currentMeshSection = new MeshSection();
        
        {
            meshSections.add(currentMeshSection);
            currentMeshSection.staticMatrix = new Matrix4f();
        }
        
        Builder(Matrix4f baseDynamicMatrix) {
            matrixStack.dynamicPushCallback(this::onDynamicPush);
            matrixStack.dynamicPopCallback(this::onDynamicPop);
            currentMeshSection.dynamicMatrix = baseDynamicMatrix;
        }
        
        public static class MeshSection {
            Matrix4fc staticMatrix;
            // this is passed in via QuartzMatrixStack#push(Matrix4f)
            Matrix4fc dynamicMatrix;
            
            public static class VertexData {
                private float x, y, z;
                private int r, g, b, a;
                private float u, v;
                private int lu, lv;
                private float nx, ny, nz;
            }
            
            ArrayList<VertexData> vertexData = new ArrayList<>();
            ArrayList<MeshSection> subSections = new ArrayList<>();
        }
        
        void onDynamicPush(Matrix4fc dynamicMatrix, Matrix4fc previousMatrix) {
            MeshSection subsection = new MeshSection();
            subsection.staticMatrix = new Matrix4f(previousMatrix);
            subsection.dynamicMatrix = dynamicMatrix;
            meshSections.add(subsection);
            currentMeshSection.subSections.add(subsection);
            currentMeshSection = subsection;
        }
        
        void onDynamicPop() {
            meshSections.remove(meshSections.size() - 1);
            currentMeshSection = meshSections.get(meshSections.size() - 1);
        }
        
        public void addVertex(@Nullable RenderType type, float x, float y, float z, int r, int g, int b, int a, float texU, float texV, int lightU, int lightV, float normalX, float normalY, float normalZ) {
            // despite it being passed through, effectively only cutout mipped is used
            // its able to simulate standard cutout or solid, and means i dont need to separate the data into different buffers for rendering
            assert (builders.containsKey(type));
            
            MeshSection.VertexData vertexData = new MeshSection.VertexData();
            vertexData.x = x;
            vertexData.y = y;
            vertexData.z = z;
            vertexData.r = r;
            vertexData.g = g;
            vertexData.b = b;
            vertexData.a = a;
            vertexData.u = texU;
            vertexData.v = texV;
            vertexData.lu = lightU;
            vertexData.lv = lightV;
            vertexData.nx = normalX;
            vertexData.ny = normalY;
            vertexData.nz = normalZ;
            
            currentMeshSection.vertexData.add(vertexData);
        }
        
        private static class QuartzVertexBuilder implements IVertexBuilder {
            
            private final Builder builder;
            private final RenderType type;
            
            public QuartzVertexBuilder(Builder builder, RenderType type) {
                this.builder = builder;
                this.type = type;
            }
            
            private float x, y, z;
            private int r, g, b, a;
            private float u, v;
            private int lu, lv;
            private float nx, ny, nz;
            
            @Override
            public IVertexBuilder pos(double x, double y, double z) {
                this.x = (float) x;
                this.y = (float) y;
                this.z = (float) z;
                return this;
            }
            
            @Override
            public IVertexBuilder color(int red, int green, int blue, int alpha) {
                r = red;
                g = green;
                b = blue;
                a = alpha;
                return this;
            }
            
            @Override
            public IVertexBuilder tex(float u, float v) {
                this.u = u;
                this.v = v;
                return this;
            }
            
            @Override
            public IVertexBuilder overlay(int u, int v) {
                return this;
            }
            
            @Override
            public IVertexBuilder lightmap(int u, int v) {
                lu = u;
                lv = v;
                return this;
            }
            
            @Override
            public IVertexBuilder normal(float x, float y, float z) {
                nx = x;
                ny = y;
                nz = z;
                return this;
            }
            
            @Override
            public void endVertex() {
                builder.addVertex(type, x, y, z, r, g, b, a, u, v, lu, lv, nx, ny, nz);
            }
        }
        
        @Override
        public IVertexBuilder getBuffer(RenderType renderType) {
            IVertexBuilder builder = builders.get(renderType);
            if (builder != null) {
                return builder;
            }
            throw new IllegalArgumentException("Unsupported RenderType");
        }
        
        public QuartzMatrixStack getMatrixStack() {
            return matrixStack;
        }
        
        QuartzMesh build() {
            return new QuartzMesh(meshSections.get(0));
        }
    }
}
