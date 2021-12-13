package net.roguelogix.phosphophyllite.quartz.internal.common;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.Object2LongArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.roguelogix.phosphophyllite.quartz.StaticMesh;
import net.roguelogix.phosphophyllite.quartz.internal.Buffer;
import net.roguelogix.phosphophyllite.quartz.internal.QuartzCore;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3f;
import net.roguelogix.phosphophyllite.util.MethodsReturnNonnullByDefault;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.roguelogix.phosphophyllite.quartz.internal.MagicNumbers.VERTEX_BYTE_SIZE;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class Mesh implements StaticMesh {
    
    public Consumer<StaticMesh.Builder> buildFunc;
    
    public Mesh(Consumer<StaticMesh.Builder> buildFunc) {
        this.buildFunc = buildFunc;
    }
    
    public Object2LongArrayMap<RenderType> build(Function<Integer, ByteBuffer> bufferCreator) {
        Builder builder = new Builder();
        buildFunc.accept(builder);
        var buffer = bufferCreator.apply(builder.bytesRequired());
        return builder.build(buffer);
    }
    
    private static class Builder implements StaticMesh.Builder, MultiBufferSource {
        private static class Vertex {
            
            Vertex() {
            }
            
            Vertex(Vertex toCopy) {
                x = toCopy.x;
                y = toCopy.y;
                z = toCopy.z;
                normalX = toCopy.normalX;
                normalY = toCopy.normalY;
                normalZ = toCopy.normalZ;
                rgba = toCopy.rgba;
                texU = toCopy.texU;
                texV = toCopy.texV;
                lightmapU = toCopy.lightmapU;
                lightmapV = toCopy.lightmapV;
            }
            
            float x = 0, y = 0, z = 0;
            float normalX = 0, normalY = 0, normalZ = 0;
            int rgba = -1;
            float texU = 0, texV = 0;
            
            int lightmapU = 0, lightmapV = 0;
        }
        
        private static class BufferBuilder implements VertexConsumer {
            Vertex currentVertex = new Vertex();
            final LinkedList<Vertex> vertices = new LinkedList<>();
            
            private boolean defaultColorSet = false;
            private int drgba;
            
            @Override
            public VertexConsumer vertex(double x, double y, double z) {
                // its uploaded to GL as a float, so its cased here
                currentVertex.x = (float) x;
                currentVertex.y = (float) y;
                currentVertex.z = (float) z;
                return this;
            }
            
            @Override
            public VertexConsumer color(int r, int g, int b, int a) {
                // assumes each value is <= 255, if i need to put an "& 0xFF" on these, im going to find you
                // as a side note, that means you can pass in an RGBA value in a
                currentVertex.rgba = (r << 24) | (g << 16) | (b << 8) | a;
                return this;
            }
            
            @Override
            public VertexConsumer uv(float u, float v) {
                currentVertex.texU = u;
                currentVertex.texV = v;
                return this;
            }
            
            @Override
            public VertexConsumer overlayCoords(int oU, int oV) {
                // ignored, the fuck is this for?
                return this;
            }
            
            @Override
            public VertexConsumer uv2(int u2, int v2) {
                currentVertex.lightmapU = u2;
                currentVertex.lightmapV = v2;
                return this;
            }
            
            @Override
            public VertexConsumer normal(float nx, float ny, float nz) {
                currentVertex.normalX = nx;
                currentVertex.normalY = ny;
                currentVertex.normalZ = nz;
                return this;
            }
            
            @Override
            public void endVertex() {
                vertices.add(currentVertex);
                
                currentVertex = new Vertex(currentVertex);
                if (defaultColorSet) {
                    currentVertex.rgba = drgba;
                }
            }
            
            @Override
            public void defaultColor(int r, int g, int b, int a) {
                // assumes each value is <= 255, if i need to put an "& 0xFF" on these, im going to find you
                // as a side note, that means you can pass in an RGBA value in a
                drgba = (r << 24) | (g << 16) | (b << 8) | a;
                defaultColorSet = true;
            }
            
            @Override
            public void unsetDefaultColor() {
                defaultColorSet = false;
            }
            
        }
        
        private final PoseStack poseStack = new PoseStack();
        private final HashMap<RenderType, BufferBuilder> buffers = new HashMap<>();
        
        Builder() {
        }
        
        @Override
        public MultiBufferSource bufferSource() {
            return this;
        }
        
        @Override
        public PoseStack matrixStack() {
            return poseStack;
        }
        
        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            return buffers.computeIfAbsent(renderType, e -> new BufferBuilder());
        }
        
        int bytesRequired() {
            int totalVertices = 0;
            for (var entry : buffers.entrySet()) {
                var renderType = entry.getKey();
                var bufferBuilder = entry.getValue();
                int vertexCount = bufferBuilder.vertices.size() - bufferBuilder.vertices.size() % renderType.mode().primitiveLength;
                if (vertexCount == 0) {
                    continue;
                }
                totalVertices += vertexCount;
            }
            return totalVertices * VERTEX_BYTE_SIZE;
        }
        
        Object2LongArrayMap<RenderType> build(ByteBuffer masterBuffer) {
            Object2LongArrayMap<RenderType> drawInfoMap = new Object2LongArrayMap<>();
            
            int currentByteIndex = 0;
            for (var entry : buffers.entrySet()) {
                RenderType renderType = entry.getKey();
                BufferBuilder bufferBuilder = entry.getValue();
                int vertexCount = bufferBuilder.vertices.size() - bufferBuilder.vertices.size() % renderType.mode().primitiveLength;
                if (vertexCount == 0) {
                    continue;
                }
                final long offsetAndSize = (long) (currentByteIndex / VERTEX_BYTE_SIZE) << 32 | (long) vertexCount;
                final var byteBuf = masterBuffer.slice(currentByteIndex, vertexCount * VERTEX_BYTE_SIZE);
                currentByteIndex += vertexCount * VERTEX_BYTE_SIZE;
                
                Vector3f tempNormalVec = new Vector3f();
                
                final boolean quadType = renderType.mode() == VertexFormat.Mode.QUADS;
                if (!quadType) {
                    int vertexIndex = 0;
                    for (Vertex vertex : bufferBuilder.vertices) {
                        if (vertexIndex > vertexCount) {
                            break;
                        }
                        byteBuf.putFloat(vertex.x); // 4
                        byteBuf.putFloat(vertex.y); // 8
                        byteBuf.putFloat(vertex.z); // 12
                        byteBuf.putInt(vertex.rgba); // 16
                        byteBuf.putFloat(vertex.texU); // 20
                        byteBuf.putFloat(vertex.texV); // 24
                        
                        int packedA = 0;
                        int packedB = 0;
                        
                        packedA |= (vertex.lightmapU & 0xFF) << 24;
                        packedA |= (vertex.lightmapV & 0xFF) << 16;
                        
                        tempNormalVec.set(vertex.normalX, vertex.normalY, vertex.normalZ);
                        tempNormalVec.normalize(Short.MAX_VALUE);
                        
                        packedA |= (((int) tempNormalVec.x) & 0xFFFF);
                        packedB |= (((int) tempNormalVec.y) & 0xFFFF) << 16;
                        packedB |= (((int) tempNormalVec.z) & 0xFFFF);
                        
                        byteBuf.putInt(packedA);
                        byteBuf.putInt(packedB);
                        vertexIndex++;
                    }
                } else {
                    int vertexIndex = 0;
                    var iter = bufferBuilder.vertices.iterator();
                    Vertex[] currentVertices = new Vertex[4];
                    mainLoop:
                    while (true) {
                        for (int i = 0; i < 4; i++) {
                            if (vertexIndex > vertexCount) {
                                break mainLoop;
                            }
                            if (!iter.hasNext()) {
                                break mainLoop;
                            }
                            currentVertices[i] = iter.next();
                            vertexIndex++;
                        }
                        
                        int packedLightA = 0;
                        int packedLightB = 0;
                        // this might be the wrong order, ill need to check that
                        packedLightA |= (currentVertices[0].lightmapU & 0x3F);
                        packedLightA |= (currentVertices[0].lightmapV & 0x3F) << 6;
                        packedLightA |= (currentVertices[1].lightmapU & 0x3F) << 12;
                        packedLightA |= (currentVertices[1].lightmapV & 0x3F) << 18;
                        packedLightB |= (currentVertices[2].lightmapU & 0x3F);
                        packedLightB |= (currentVertices[2].lightmapV & 0x3F) << 6;
                        packedLightB |= (currentVertices[3].lightmapU & 0x3F) << 12;
                        packedLightB |= (currentVertices[3].lightmapV & 0x3F) << 18;
                        
                        for (int i = 0; i < 4; i++) {
                            var vertex = currentVertices[i];
                            byteBuf.putInt(Integer.reverseBytes(Float.floatToIntBits(vertex.x))); // 4
                            byteBuf.putInt(Integer.reverseBytes(Float.floatToIntBits(vertex.y))); // 8
                            byteBuf.putInt(Integer.reverseBytes(Float.floatToIntBits(vertex.z))); // 12
                            byteBuf.putInt(Integer.reverseBytes(vertex.rgba)); // 16
                            byteBuf.putInt(Integer.reverseBytes(Float.floatToIntBits(vertex.texU))); // 20
                            byteBuf.putInt(Integer.reverseBytes(Float.floatToIntBits(vertex.texV))); // 24
                            
                            int packedA = packedLightA;
                            int packedB = packedLightB;
                            
                            tempNormalVec.set(vertex.normalX, vertex.normalY, vertex.normalZ);
                            tempNormalVec.normalize(7);
                            
                            packedA |= packInt((int) tempNormalVec.x, 24, 4);
                            packedA |= packInt((int) tempNormalVec.y, 28, 4);
                            packedB |= packInt((int) tempNormalVec.z, 24, 4);
                            
                            packedB |= (i & 0x3) << 28;
                            
                            byteBuf.putInt(Integer.reverseBytes(packedA));
                            byteBuf.putInt(Integer.reverseBytes(packedB));
                        }
                    }
                }
                drawInfoMap.put(renderType, offsetAndSize);
            }
            return drawInfoMap;
        }
        
        private static int packInt(int value, int position, int width) {
            int signBitMask = 1 << (width - 1);
            int bitMask = signBitMask - 1;
            int returnVal = value & bitMask;
            returnVal |= (value >> (32 - width)) & signBitMask;
            return returnVal << position;
        }
        
        private static int extractInt(int packed, int pos, int width) {
            packed >>= pos;
            int signBitMask = 1 << (width - 1);
            int bitMask = signBitMask - 1;
            int val = ~bitMask * (((signBitMask & packed) != 0) ? 1 : 0);
            val |= packed & bitMask;
            return val;
        }
    }
    
    public static class Manager {
        public static class TrackedMesh {
            public record Component(int vertexOffset, int vertexCount) {
            }
            
            public final WeakReference<Mesh> meshRef;
            private final Buffer vertexBuffer;
            private Buffer.Allocation vertexAllocation;
            private final Object2ObjectArrayMap<RenderType, Component> drawInfo = new Object2ObjectArrayMap<>();
            private final ObjectArrayList<Consumer<TrackedMesh>> buildCallbacks = new ObjectArrayList<>();
            
            public TrackedMesh(WeakReference<Mesh> meshRef, Buffer vertexBuffer) {
                this.meshRef = meshRef;
                this.vertexBuffer = vertexBuffer;
            }
            
            void rebuild() {
                var mesh = meshRef.get();
                if (mesh == null) {
                    return;
                }
                Object2LongArrayMap<RenderType> rawDrawInfo;
                drawInfo.clear();
                try {
                    rawDrawInfo = mesh.build(this::allocBuffer);
                    vertexAllocation.dirty();
                } finally {
                    if (vertexAllocation != null) {
                        vertexAllocation.unlock();
                    }
                }
                for (var renderTypeEntry : rawDrawInfo.object2LongEntrySet()) {
                    var renderType = renderTypeEntry.getKey();
                    var drawLong = renderTypeEntry.getLongValue();
                    var drawComponent = new Component((int) (drawLong >> 32) + vertexAllocation.offset() / VERTEX_BYTE_SIZE, (int) drawLong);
                    drawInfo.put(renderType, drawComponent);
                }
                for (int i = 0; i < buildCallbacks.size(); i++) {
                    buildCallbacks.get(i).accept(this);
                }
            }
            
            private ByteBuffer allocBuffer(int size) {
                if (vertexAllocation != null) {
                    vertexAllocation = vertexBuffer.realloc(vertexAllocation, size, VERTEX_BYTE_SIZE);
                } else {
                    vertexAllocation = vertexBuffer.alloc(size, VERTEX_BYTE_SIZE);
                }
                vertexAllocation.lock();
                return vertexAllocation.buffer();
            }
            
            public Collection<RenderType> usedRenderTypes() {
                return drawInfo.keySet();
            }
            
            @Nullable
            public Component renderTypeComponent(RenderType renderType) {
                return drawInfo.get(renderType);
            }
            
            public void addBuildCallback(Consumer<TrackedMesh> consumer) {
                buildCallbacks.add(consumer);
            }
            
            public void addBuildCallback(Runnable runnable) {
                addBuildCallback(e -> runnable.run());
            }
        }
        
        private final ObjectArrayList<TrackedMesh> trackedMeshes = new ObjectArrayList<TrackedMesh>();
        public final Buffer vertexBuffer;
        
        public Manager(Buffer vertexBuffer) {
            this.vertexBuffer = vertexBuffer;
        }
        
        public Mesh createMesh(Consumer<StaticMesh.Builder> buildFunc) {
            final var staticMesh = new Mesh(buildFunc);
            final var trackedMesh = new TrackedMesh(new WeakReference<>(staticMesh), vertexBuffer);
            synchronized (trackedMeshes) {
                trackedMeshes.add(trackedMesh);
            }
            QuartzCore.CLEANER.register(staticMesh, () -> {
                synchronized (trackedMeshes) {
                    trackedMeshes.remove(trackedMesh);
                }
            });
            return staticMesh;
        }
        
        @Nullable
        public TrackedMesh getMeshInfo(StaticMesh mesh) {
            for (int i = 0; i < trackedMeshes.size(); i++) {
                var trackedMesh = trackedMeshes.get(i);
                if (trackedMesh.meshRef.get() == mesh) {
                    return trackedMesh;
                }
            }
            return null;
        }
        
        public void buildAllMeshes() {
            for (TrackedMesh value : trackedMeshes) {
                buildTrackedMesh(value);
            }
        }
        
        public void buildMesh(StaticMesh mesh) {
            var trackedMesh = getMeshInfo(mesh);
            if (trackedMesh != null) {
                buildTrackedMesh(trackedMesh);
            }
        }
        
        private void buildTrackedMesh(TrackedMesh trackedMesh) {
            trackedMesh.rebuild();
        }
        
    }
    
}

