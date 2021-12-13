package net.roguelogix.phosphophyllite.quartz.internal.gl;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.roguelogix.phosphophyllite.quartz.DrawBatch;
import net.roguelogix.phosphophyllite.quartz.DynamicLight;
import net.roguelogix.phosphophyllite.quartz.DynamicMatrix;
import net.roguelogix.phosphophyllite.quartz.StaticMesh;
import net.roguelogix.phosphophyllite.quartz.internal.Buffer;
import net.roguelogix.phosphophyllite.quartz.internal.QuartzCore;
import net.roguelogix.phosphophyllite.quartz.internal.common.*;
import net.roguelogix.phosphophyllite.repack.org.joml.AABBi;
import net.roguelogix.phosphophyllite.repack.org.joml.Matrix4f;
import net.roguelogix.phosphophyllite.repack.org.joml.Matrix4fc;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3ic;
import org.lwjgl.opengl.GL;

import javax.annotation.Nullable;

import java.lang.ref.WeakReference;

import static net.roguelogix.phosphophyllite.quartz.internal.MagicNumbers.GL.*;
import static net.roguelogix.phosphophyllite.quartz.internal.MagicNumbers.*;
import static org.lwjgl.opengl.ARBBaseInstance.glDrawArraysInstancedBaseInstance;
import static org.lwjgl.opengl.ARBBaseInstance.glDrawElementsInstancedBaseVertexBaseInstance;
import static org.lwjgl.opengl.ARBVertexAttribBinding.*;
import static org.lwjgl.opengl.ARBInstancedArrays.*;
import static org.lwjgl.opengl.GL32C.*;

public class GLDrawBatch implements DrawBatch {
    
    private class MeshInstanceManager {
        private class DrawComponent {
            private final GLRenderPass renderPass;
            private final boolean QUAD;
            public final int GL_MODE;
            private int drawIndex;
            
            private final int baseVertex;
            private final int elementCount;
            
            private final boolean BASE_INSTANCE = GLDrawBatch.this.BASE_INSTANCE;
            private final boolean ATTRIB_BINDING = GLDrawBatch.this.ATTRIB_BINDING;
            
            private DrawComponent(RenderType renderType, Mesh.Manager.TrackedMesh.Component component) {
                renderPass = renderPasses.computeIfAbsent(renderType, GLRenderPass::new);
                QUAD = renderPass.QUAD;
                GL_MODE = renderPass.GL_MODE;
                
                baseVertex = component.vertexOffset();
                int elementCountTemp = component.vertexCount();
                if (QUAD) {
                    elementCountTemp *= 6;
                    elementCountTemp /= 4;
                    GLCore.INSTANCE.ensureElementBufferLength(elementCountTemp / 6);
                }
                elementCount = elementCountTemp;
                var drawComponents = (renderPass.ALPHA_DISCARD ? cutoutDrawComponents : opaqueDrawComponents).computeIfAbsent(renderPass, e -> new ObjectArrayList<>());
                drawIndex = drawComponents.size();
                drawComponents.add(this);
            }
            
            private void draw() {
                if (!BASE_INSTANCE) {
                    if (ATTRIB_BINDING) {
                        glBindVertexBuffer(1, instanceDataBuffer.handle(), instanceDataOffset, INSTANCE_DATA_BYTE_SIZE);
                    } else {
                        int offset = 0;
                        glVertexAttribIPointer(WORLD_POSITION_LOCATION, 3, GL_INT, INSTANCE_DATA_BYTE_SIZE, offset);
                        offset += IVEC4_BYTE_SIZE;
                        glVertexAttribIPointer(DYNAMIC_MATRIX_ID_LOCATION, 1, GL_INT, INSTANCE_DATA_BYTE_SIZE, offset);
                        offset += INT_BYTE_SIZE;
                        glVertexAttribIPointer(DYNAMIC_LIGHT_ID_LOCATION, 1, GL_INT, INSTANCE_DATA_BYTE_SIZE, offset);
                        offset += INT_BYTE_SIZE;
                        glVertexAttribPointer(STATIC_MATRIX_LOCATION, 4, GL_FLOAT, false, INSTANCE_DATA_BYTE_SIZE, offset);
                        offset += VEC4_BYTE_SIZE;
                        glVertexAttribPointer(STATIC_MATRIX_LOCATION + 1, 4, GL_FLOAT, false, INSTANCE_DATA_BYTE_SIZE, offset);
                        offset += VEC4_BYTE_SIZE;
                        glVertexAttribPointer(STATIC_MATRIX_LOCATION + 2, 4, GL_FLOAT, false, INSTANCE_DATA_BYTE_SIZE, offset);
                        offset += VEC4_BYTE_SIZE;
                        glVertexAttribPointer(STATIC_MATRIX_LOCATION + 3, 4, GL_FLOAT, false, INSTANCE_DATA_BYTE_SIZE, offset);
                        offset += VEC4_BYTE_SIZE;
                        glVertexAttribPointer(STATIC_NORMAL_MATRIX_LOCATION, 4, GL_FLOAT, false, INSTANCE_DATA_BYTE_SIZE, offset);
                        offset += VEC4_BYTE_SIZE;
                        glVertexAttribPointer(STATIC_NORMAL_MATRIX_LOCATION + 1, 4, GL_FLOAT, false, INSTANCE_DATA_BYTE_SIZE, offset);
                        offset += VEC4_BYTE_SIZE;
                        glVertexAttribPointer(STATIC_NORMAL_MATRIX_LOCATION + 2, 4, GL_FLOAT, false, INSTANCE_DATA_BYTE_SIZE, offset);
                        offset += VEC4_BYTE_SIZE;
                        glVertexAttribPointer(STATIC_NORMAL_MATRIX_LOCATION + 3, 4, GL_FLOAT, false, INSTANCE_DATA_BYTE_SIZE, offset);
                        offset += VEC4_BYTE_SIZE;
                    }
                    if (QUAD) {
                        glDrawElementsInstancedBaseVertex(GL_TRIANGLES, elementCount, GL_UNSIGNED_INT, 0, instanceCount, baseVertex);
                    } else {
                        glDrawArraysInstanced(GL_MODE, baseVertex, elementCount, instanceCount);
                    }
                } else {
                    if (QUAD) {
                        glDrawElementsInstancedBaseVertexBaseInstance(GL_TRIANGLES, elementCount, GL_UNSIGNED_INT, 0, instanceCount, baseVertex, instanceDataOffset / INSTANCE_DATA_BYTE_SIZE);
                    } else {
                        glDrawArraysInstancedBaseInstance(GL_MODE, baseVertex, elementCount, instanceCount, instanceDataOffset / INSTANCE_DATA_BYTE_SIZE);
                    }
                }
            }
        }
        
        private final Mesh staticMesh;
        private final Mesh.Manager.TrackedMesh trackedMesh;
        private final ObjectArrayList<DrawComponent> components = new ObjectArrayList<>();
        private Buffer.Allocation instanceDataAlloc;
        private int instanceDataOffset;
        
        private final ObjectArrayList<Instance.Location> liveInstances = new ObjectArrayList<>();
        private int instanceCount = 0;
        
        private MeshInstanceManager(Mesh mesh) {
            staticMesh = mesh;
            trackedMesh = QuartzCore.INSTANCE.meshManager.getMeshInfo(mesh);
            if (trackedMesh == null) {
                throw new IllegalArgumentException("Unable to find mesh in mesh registry");
            }
            
            onRebuild();
            trackedMesh.addBuildCallback(this::onRebuild);
            instanceDataAlloc = instanceDataBuffer.alloc(INSTANCE_DATA_BYTE_SIZE);
            final var ref = new WeakReference<>(this);
            instanceDataAlloc.addReallocCallback(alloc ->{
                final var manager = ref.get();
                if (manager != null) {
                    manager.instanceDataOffset = manager.instanceDataAlloc.offset();
                }
            });
        }
        
        private void onRebuild() {
            for (int i = 0; i < components.size(); i++) {
                var component = components.get(i);
                var renderPass = component.renderPass;
                final var componentMap = renderPass.ALPHA_DISCARD ? cutoutDrawComponents : opaqueDrawComponents;
                final var drawComponents = componentMap.get(renderPass);
                if (drawComponents == null) {
                    component.drawIndex = -1;
                    continue;
                }
                if (component.drawIndex != -1) {
                    var removed = drawComponents.pop();
                    if (component.drawIndex < drawComponents.size()) {
                        removed.drawIndex = component.drawIndex;
                        drawComponents.set(component.drawIndex, removed);
                    }
                    component.drawIndex = -1;
                }
                if (drawComponents.isEmpty()) {
                    renderPasses.remove(renderPass.renderType);
                    componentMap.remove(renderPass);
                }
            }
            components.clear();
            for (RenderType renderType : trackedMesh.usedRenderTypes()) {
                var component = trackedMesh.renderTypeComponent(renderType);
                if (component == null) {
                    continue;
                }
                var drawComponent = new DrawComponent(renderType, component);
                components.add(drawComponent);
            }
        }
        
        private void instanceDataMoved(Buffer.Allocation allocation) {
            instanceDataOffset = allocation.offset();
        }
        
        Instance createInstance(Vector3ic worldPosition, DynamicMatrixManager.Matrix dynamicMatrix, Matrix4fc staticMatrix, DynamicLightManager.Light dynamicLight) {
            if (instanceDataAlloc.size() < (instanceCount + 1) * INSTANCE_DATA_BYTE_SIZE) {
                instanceDataAlloc = instanceDataBuffer.realloc(instanceDataAlloc, instanceDataAlloc.size() * 2, INSTANCE_DATA_BYTE_SIZE);
            }
            
            
            final var byteBuf = instanceDataAlloc.buffer();
            int baseOffset = instanceCount * INSTANCE_DATA_BYTE_SIZE;
            worldPosition.get(baseOffset + WORLD_POSITION_OFFSET, byteBuf);
            byteBuf.putInt(baseOffset + DYNAMIC_MATRIX_ID_OFFSET, Integer.reverseBytes(dynamicMatrix.id()));
            byteBuf.putInt(baseOffset + DYNAMIC_LIGHT_ID_OFFSET, Integer.reverseBytes(dynamicLight.id()));
            staticMatrix.get(baseOffset + STATIC_MATRIX_OFFSET, byteBuf);
            staticMatrix.normal(SCRATCH_NORMAL_MATRIX).get(baseOffset + STATIC_NORMAL_MATRIX_OFFSET, byteBuf);
//            instanceDataAlloc.dirtyRange(baseOffset, INSTANCE_DATA_BYTE_SIZE);
            instanceDataBuffer.dirtyAll();
            
            var instance = new Instance(instanceCount++, dynamicMatrix, dynamicLight);
            liveInstances.add(instance.location);
            return instance;
        }
        
        void removeInstance(Instance.Location instance) {
            if (instance.location == -1) {
                return;
            }
            instanceCount--;
            var endInstance = liveInstances.pop();
            if (instance != endInstance) {
                // swapping time!
                instanceDataAlloc.copy(endInstance.location * INSTANCE_DATA_BYTE_SIZE, instance.location * INSTANCE_DATA_BYTE_SIZE, INSTANCE_DATA_BYTE_SIZE);
//                instanceDataAlloc.dirtyRange(instance.location * INSTANCE_DATA_BYTE_SIZE, INSTANCE_DATA_BYTE_SIZE);
//                instanceDataBuffer.dirtyAll();
                
                liveInstances.set(instance.location, endInstance);
                endInstance.location = instance.location;
            }
            instance.location = -1;
            if (instanceCount == 0) {
                delete();
            }
        }
        
        public void delete() {
            while (!liveInstances.isEmpty()) {
                removeInstance(liveInstances.peek(0));
            }
            for (int i = 0; i < components.size(); i++) {
                var component = components.get(i);
                var renderPass = component.renderPass;
                final var componentMap = renderPass.ALPHA_DISCARD ? cutoutDrawComponents : opaqueDrawComponents;
                final var drawComponents = componentMap.get(renderPass);
                if (drawComponents == null) {
                    component.drawIndex = -1;
                    continue;
                }
                if (component.drawIndex != -1) {
                    var removed = drawComponents.pop();
                    if (component.drawIndex < drawComponents.size()) {
                        removed.drawIndex = component.drawIndex;
                        drawComponents.set(component.drawIndex, removed);
                    }
                    component.drawIndex = -1;
                }
                if (drawComponents.isEmpty()) {
                    renderPasses.remove(renderPass.renderType);
                    componentMap.remove(renderPass);
                }
            }
            instanceManagers.remove(staticMesh);
        }
        
        private class Instance implements DrawBatch.Instance {
            
            private static class Location {
                private int location;
                
                private Location(int location) {
                    this.location = location;
                }
            }
            
            private final Location location;
            private DynamicMatrixManager.Matrix dynamicMatrix;
            private DynamicLightManager.Light dynamicLight;
            
            private Instance(int initialLocation, DynamicMatrixManager.Matrix dynamicMatrix, DynamicLightManager.Light dynamicLight) {
                final var manager = MeshInstanceManager.this;
                final var location = new Location(initialLocation);
                QuartzCore.CLEANER.register(this, () -> GLCore.deletionQueue.enqueue(() -> manager.removeInstance(location)));
                this.location = location;
                this.dynamicMatrix = dynamicMatrix;
                this.dynamicLight = dynamicLight;
            }
            
            @Override
            public void updateDynamicMatrix(@Nullable DynamicMatrix newDynamicMatrix) {
                if (dynamicMatrix == newDynamicMatrix) {
                    return;
                }
                if (newDynamicMatrix == null) {
                    newDynamicMatrix = IDENTITY_DYNAMIC_MATRIX;
                }
                if (newDynamicMatrix instanceof DynamicMatrixManager.Matrix dynamicMatrix && dynamicMatrixManager.owns(dynamicMatrix)) {
                    this.dynamicMatrix = dynamicMatrix;
                    final var offset = location.location * INSTANCE_DATA_BYTE_SIZE + DYNAMIC_MATRIX_ID_OFFSET;
                    instanceDataAlloc.buffer().putInt(offset, Integer.reverseBytes(dynamicMatrix.id()));
                    instanceDataAlloc.dirtyRange(offset, INT_BYTE_SIZE);
                }
            }
            
            @Override
            public void updateStaticMatrix(@Nullable Matrix4fc newStaticMatrix) {
                if (newStaticMatrix == null) {
                    newStaticMatrix = IDENTITY_MATRIX;
                }
                final var transformOffset = location.location * INSTANCE_DATA_BYTE_SIZE + STATIC_MATRIX_OFFSET;
                final var normalOffset = location.location * INSTANCE_DATA_BYTE_SIZE + STATIC_NORMAL_MATRIX_OFFSET;
                newStaticMatrix.get(transformOffset, instanceDataAlloc.buffer());
                newStaticMatrix.normal(SCRATCH_NORMAL_MATRIX).get(normalOffset, instanceDataAlloc.buffer());
                instanceDataAlloc.dirtyRange(transformOffset, MATRIX_4F_BYTE_SIZE_2);
            }
            
            @Override
            public void updateDynamicLight(@Nullable DynamicLight newDynamicLight) {
                if (dynamicLight == newDynamicLight) {
                    return;
                }
                if (newDynamicLight == null) {
                    newDynamicLight = ZERO_LEVEL_LIGHT;
                }
                if (newDynamicLight instanceof DynamicLightManager.Light dynamicLight && lightManager.owns(dynamicLight)) {
                    this.dynamicLight = dynamicLight;
                    int newLightID = dynamicLight.id();
                    final var offset = location.location * INSTANCE_DATA_BYTE_SIZE + DYNAMIC_LIGHT_ID_OFFSET;
                    instanceDataAlloc.buffer().putInt(offset, Integer.reverseBytes(newLightID));
                    instanceDataAlloc.dirtyRange(offset, INT_BYTE_SIZE);
                }
            }
            
            @Override
            public void delete() {
                removeInstance(location);
            }
        }
    }
    
    private static final Matrix4fc IDENTITY_MATRIX = new Matrix4f();
    private static final Matrix4f SCRATCH_NORMAL_MATRIX = new Matrix4f();
    
    private final GLBuffer instanceDataBuffer = new GLBuffer(false);
    
    private final GLBuffer dynamicMatrixBuffer = new GLBuffer(false);
    private final DynamicMatrixManager dynamicMatrixManager = new DynamicMatrixManager(dynamicMatrixBuffer);
    private final DynamicMatrix IDENTITY_DYNAMIC_MATRIX = dynamicMatrixManager.createMatrix((matrix, nanoSinceLastFrame, partialTicks, playerBlock, playerPartialBlock) -> matrix.write(IDENTITY_MATRIX), null);
    private final GLBuffer dynamicLightBuffer = new GLBuffer(false);
    private final DynamicLightManager lightManager = new DynamicLightManager(dynamicLightBuffer);
    private final DynamicLightManager.Light ZERO_LEVEL_LIGHT = lightManager.createLight((light, blockAndTintGetter) -> light.write((byte) 0, (byte) 0, (byte) 0));
    
    private final int VAO;
    private final int dynamicMatrixTexture;
    private final int dynamicLightTexture;
    
    private final boolean BASE_INSTANCE = GL.getCapabilities().GL_ARB_base_instance; // TODO: Config disable
    private final boolean ATTRIB_BINDING = GL.getCapabilities().GL_ARB_vertex_attrib_binding; // TODO: Config disable
    
    private final Object2ObjectMap<Mesh, MeshInstanceManager> instanceManagers = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectMap<RenderType, GLRenderPass> renderPasses = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectMap<GLRenderPass, ObjectArrayList<MeshInstanceManager.DrawComponent>> opaqueDrawComponents = new Object2ObjectArrayMap<>();
    private final Object2ObjectMap<GLRenderPass, ObjectArrayList<MeshInstanceManager.DrawComponent>> cutoutDrawComponents = new Object2ObjectArrayMap<>();
    
    private AABBi cullAABB = null;
    private boolean enabled = true;
    
    public GLDrawBatch() {
        final int VAO = glGenVertexArrays();
        B3DStateHelper.bindVertexArray(VAO);
        
        B3DStateHelper.bindElementBuffer(GLCore.INSTANCE.elementBuffer.handle());
        
        glEnableVertexAttribArray(POSITION_LOCATION);
        glEnableVertexAttribArray(COLOR_LOCATION);
        glEnableVertexAttribArray(TEX_COORD_LOCATION);
        glEnableVertexAttribArray(LIGHTINFO_LOCATION);
        
        glEnableVertexAttribArray(WORLD_POSITION_LOCATION);
        glEnableVertexAttribArray(DYNAMIC_MATRIX_ID_LOCATION);
        glEnableVertexAttribArray(DYNAMIC_LIGHT_ID_LOCATION);
        glEnableVertexAttribArray(STATIC_MATRIX_LOCATION);
        glEnableVertexAttribArray(STATIC_MATRIX_LOCATION + 1);
        glEnableVertexAttribArray(STATIC_MATRIX_LOCATION + 2);
        glEnableVertexAttribArray(STATIC_MATRIX_LOCATION + 3);
        glEnableVertexAttribArray(STATIC_NORMAL_MATRIX_LOCATION);
        glEnableVertexAttribArray(STATIC_NORMAL_MATRIX_LOCATION + 1);
        glEnableVertexAttribArray(STATIC_NORMAL_MATRIX_LOCATION + 2);
        glEnableVertexAttribArray(STATIC_NORMAL_MATRIX_LOCATION + 3);
        
        if (ATTRIB_BINDING) {
            glBindVertexBuffer(0, QuartzCore.INSTANCE.meshManager.vertexBuffer.as(GLBuffer.class).handle(), 0, VERTEX_BYTE_SIZE);
            
            glVertexAttribBinding(POSITION_LOCATION, 0);
            glVertexAttribBinding(COLOR_LOCATION, 0);
            glVertexAttribBinding(TEX_COORD_LOCATION, 0);
            glVertexAttribBinding(LIGHTINFO_LOCATION, 0);
            
            glVertexAttribFormat(POSITION_LOCATION, 3, GL_FLOAT, false, 0);
            glVertexAttribIFormat(COLOR_LOCATION, 1, GL_INT, 12);
            glVertexAttribFormat(TEX_COORD_LOCATION, 2, GL_FLOAT, false, 16);
            glVertexAttribIFormat(LIGHTINFO_LOCATION, 2, GL_INT, 24);
            
            // when base instance is unavailable, this must be setup per draw
            if (BASE_INSTANCE) {
                glBindVertexBuffer(1, instanceDataBuffer.handle(), 0, INSTANCE_DATA_BYTE_SIZE);
            }
            glVertexBindingDivisor(1, 1);
            
            glVertexAttribBinding(WORLD_POSITION_LOCATION, 1);
            glVertexAttribBinding(DYNAMIC_MATRIX_ID_LOCATION, 1);
            glVertexAttribBinding(DYNAMIC_LIGHT_ID_LOCATION, 1);
            glVertexAttribBinding(STATIC_MATRIX_LOCATION, 1);
            glVertexAttribBinding(STATIC_MATRIX_LOCATION + 1, 1);
            glVertexAttribBinding(STATIC_MATRIX_LOCATION + 2, 1);
            glVertexAttribBinding(STATIC_MATRIX_LOCATION + 3, 1);
            glVertexAttribBinding(STATIC_NORMAL_MATRIX_LOCATION, 1);
            glVertexAttribBinding(STATIC_NORMAL_MATRIX_LOCATION + 1, 1);
            glVertexAttribBinding(STATIC_NORMAL_MATRIX_LOCATION + 2, 1);
            glVertexAttribBinding(STATIC_NORMAL_MATRIX_LOCATION + 3, 1);
            
            int offset = 0;
            glVertexAttribIFormat(WORLD_POSITION_LOCATION, 3, GL_INT, offset);
            offset += IVEC4_BYTE_SIZE;
            glVertexAttribIFormat(DYNAMIC_MATRIX_ID_LOCATION, 1, GL_INT, offset);
            offset += INT_BYTE_SIZE;
            glVertexAttribIFormat(DYNAMIC_LIGHT_ID_LOCATION, 1, GL_INT, offset);
            offset += INT_BYTE_SIZE;
            glVertexAttribFormat(STATIC_MATRIX_LOCATION, 4, GL_FLOAT, false, offset);
            offset += VEC4_BYTE_SIZE;
            glVertexAttribFormat(STATIC_MATRIX_LOCATION + 1, 4, GL_FLOAT, false, offset);
            offset += VEC4_BYTE_SIZE;
            glVertexAttribFormat(STATIC_MATRIX_LOCATION + 2, 4, GL_FLOAT, false, offset);
            offset += VEC4_BYTE_SIZE;
            glVertexAttribFormat(STATIC_MATRIX_LOCATION + 3, 4, GL_FLOAT, false, offset);
            offset += VEC4_BYTE_SIZE;
            glVertexAttribFormat(STATIC_NORMAL_MATRIX_LOCATION, 4, GL_FLOAT, false, offset);
            offset += VEC4_BYTE_SIZE;
            glVertexAttribFormat(STATIC_NORMAL_MATRIX_LOCATION + 1, 4, GL_FLOAT, false, offset);
            offset += VEC4_BYTE_SIZE;
            glVertexAttribFormat(STATIC_NORMAL_MATRIX_LOCATION + 2, 4, GL_FLOAT, false, offset);
            offset += VEC4_BYTE_SIZE;
            glVertexAttribFormat(STATIC_NORMAL_MATRIX_LOCATION + 3, 4, GL_FLOAT, false, offset);
            offset += VEC4_BYTE_SIZE;
            
            
        } else {
            B3DStateHelper.bindArrayBuffer(QuartzCore.INSTANCE.meshManager.vertexBuffer.as(GLBuffer.class).handle());
            glVertexAttribPointer(POSITION_LOCATION, 3, GL_FLOAT, false, VERTEX_BYTE_SIZE, 0);
            glVertexAttribIPointer(COLOR_LOCATION, 1, GL_INT, VERTEX_BYTE_SIZE, 12);
            glVertexAttribPointer(TEX_COORD_LOCATION, 2, GL_FLOAT, false, VERTEX_BYTE_SIZE, 16);
            glVertexAttribIPointer(LIGHTINFO_LOCATION, 2, GL_INT, VERTEX_BYTE_SIZE, 24);
            
            glVertexAttribDivisorARB(WORLD_POSITION_LOCATION, 1);
            glVertexAttribDivisorARB(DYNAMIC_MATRIX_ID_LOCATION, 1);
            glVertexAttribDivisorARB(DYNAMIC_LIGHT_ID_LOCATION, 1);
            glVertexAttribDivisorARB(STATIC_MATRIX_LOCATION, 1);
            glVertexAttribDivisorARB(STATIC_MATRIX_LOCATION + 1, 1);
            glVertexAttribDivisorARB(STATIC_MATRIX_LOCATION + 2, 1);
            glVertexAttribDivisorARB(STATIC_MATRIX_LOCATION + 3, 1);
            glVertexAttribDivisorARB(STATIC_NORMAL_MATRIX_LOCATION, 1);
            glVertexAttribDivisorARB(STATIC_NORMAL_MATRIX_LOCATION + 1, 1);
            glVertexAttribDivisorARB(STATIC_NORMAL_MATRIX_LOCATION + 2, 1);
            glVertexAttribDivisorARB(STATIC_NORMAL_MATRIX_LOCATION + 3, 1);
            
            // when base instance is unavailable, this must be setup per draw
            if (BASE_INSTANCE) {
                B3DStateHelper.bindArrayBuffer(instanceDataBuffer.handle());
                int offset = 0;
                glVertexAttribIPointer(WORLD_POSITION_LOCATION, 3, GL_INT, INSTANCE_DATA_BYTE_SIZE, offset);
                offset += IVEC4_BYTE_SIZE;
                glVertexAttribIPointer(DYNAMIC_MATRIX_ID_LOCATION, 1, GL_INT, INSTANCE_DATA_BYTE_SIZE, offset);
                offset += INT_BYTE_SIZE;
                glVertexAttribIPointer(DYNAMIC_LIGHT_ID_LOCATION, 1, GL_INT, INSTANCE_DATA_BYTE_SIZE, offset);
                offset += INT_BYTE_SIZE;
                glVertexAttribPointer(STATIC_MATRIX_LOCATION, 4, GL_FLOAT, false, INSTANCE_DATA_BYTE_SIZE, offset);
                offset += VEC4_BYTE_SIZE;
                glVertexAttribPointer(STATIC_MATRIX_LOCATION + 1, 4, GL_FLOAT, false, INSTANCE_DATA_BYTE_SIZE, offset);
                offset += VEC4_BYTE_SIZE;
                glVertexAttribPointer(STATIC_MATRIX_LOCATION + 2, 4, GL_FLOAT, false, INSTANCE_DATA_BYTE_SIZE, offset);
                offset += VEC4_BYTE_SIZE;
                glVertexAttribPointer(STATIC_MATRIX_LOCATION + 3, 4, GL_FLOAT, false, INSTANCE_DATA_BYTE_SIZE, offset);
                offset += VEC4_BYTE_SIZE;
                glVertexAttribPointer(STATIC_NORMAL_MATRIX_LOCATION, 4, GL_FLOAT, false, INSTANCE_DATA_BYTE_SIZE, offset);
                offset += VEC4_BYTE_SIZE;
                glVertexAttribPointer(STATIC_NORMAL_MATRIX_LOCATION + 1, 4, GL_FLOAT, false, INSTANCE_DATA_BYTE_SIZE, offset);
                offset += VEC4_BYTE_SIZE;
                glVertexAttribPointer(STATIC_NORMAL_MATRIX_LOCATION + 2, 4, GL_FLOAT, false, INSTANCE_DATA_BYTE_SIZE, offset);
                offset += VEC4_BYTE_SIZE;
                glVertexAttribPointer(STATIC_NORMAL_MATRIX_LOCATION + 3, 4, GL_FLOAT, false, INSTANCE_DATA_BYTE_SIZE, offset);
                offset += VEC4_BYTE_SIZE;
            }
        }
        glBindVertexArray(0);
        
        final int dynamicMatrixTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_BUFFER, dynamicMatrixTexture);
        glTexBuffer(GL_TEXTURE_BUFFER, GL_RGBA32F, dynamicMatrixBuffer.handle());
        
        final int dynamicLightTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_BUFFER, dynamicLightTexture);
        glTexBuffer(GL_TEXTURE_BUFFER, GL_RG8UI, dynamicLightBuffer.handle());
        
        glBindTexture(GL_TEXTURE_BUFFER, 0);
        
        QuartzCore.CLEANER.register(this, () -> GLCore.deletionQueue.enqueue(() -> {
            glDeleteTextures(dynamicLightTexture);
            glDeleteTextures(dynamicMatrixTexture);
            glDeleteVertexArrays(VAO);
        }));
        
        this.VAO = VAO;
        this.dynamicMatrixTexture = dynamicMatrixTexture;
        this.dynamicLightTexture = dynamicLightTexture;
    }
    
    @Nullable
    @Override
    public Instance createInstance(Vector3ic position, StaticMesh quartzMesh, @Nullable DynamicMatrix quartzDynamicMatrix, @Nullable Matrix4fc staticMatrix, @Nullable DynamicLight quartzLight, @Nullable DynamicLight.Type lightType) {
        if (!(quartzMesh instanceof Mesh mesh)) {
            return null;
        }
        if (quartzDynamicMatrix == null) {
            quartzDynamicMatrix = IDENTITY_DYNAMIC_MATRIX;
        }
        if (!(quartzDynamicMatrix instanceof DynamicMatrixManager.Matrix dynamicMatrix) || !dynamicMatrixManager.owns(dynamicMatrix)) {
            return null;
        }
        if (quartzLight == null) {
            if (lightType == null) {
                lightType = DynamicLight.Type.SMOOTH;
            }
            quartzLight = QuartzCore.INSTANCE.lightEngine.createLightForPos(position, lightManager, lightType);
        }
        if (!(quartzLight instanceof DynamicLightManager.Light light) || !lightManager.owns(light)) {
            return null;
        }
        var instanceManager = instanceManagers.computeIfAbsent(mesh, MeshInstanceManager::new);
        if (staticMatrix == null) {
            staticMatrix = IDENTITY_MATRIX;
        }
        return instanceManager.createInstance(position, dynamicMatrix, staticMatrix, light);
    }
    
    @Override
    public DynamicMatrix createDynamicMatrix(@Nullable DynamicMatrix parentTransform, @Nullable DynamicMatrix.UpdateFunc updateFunc) {
        return dynamicMatrixManager.createMatrix(updateFunc, parentTransform);
    }
    
    @Override
    public DynamicLight createLight(Vector3ic lightPosition, DynamicLight.Type lightType) {
        return QuartzCore.INSTANCE.lightEngine.createLightForPos(lightPosition, lightManager, lightType);
    }
    
    @Override
    public void setCullAABB(AABBi aabb) {
        this.cullAABB = aabb;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public boolean isEmpty() {
        return instanceManagers.isEmpty();
    }
    
    void updateAndCull(DrawInfo drawInfo) {
        dynamicMatrixManager.updateAll(drawInfo.deltaNano, drawInfo.partialTicks, drawInfo.playerPosition, drawInfo.playerSubBlock);
        lightManager.updateAll(Minecraft.getInstance().level);
        dynamicMatrixBuffer.flush();
        dynamicLightBuffer.flush();
        instanceDataBuffer.flush();
    }
    
    void drawOpaque() {
        if (!enabled) {
            return;
        }
        
        if (!BASE_INSTANCE) {
            B3DStateHelper.bindArrayBuffer(instanceDataBuffer.handle());
        }
        
        final var program = GLCore.INSTANCE.mainProgram;
        
        glActiveTexture(ATLAS_TEXTURE_UNIT_GL);
        
        glBindVertexArray(VAO);
        for (var entry : opaqueDrawComponents.entrySet()) {
            program.setupRenderPass(entry.getKey());
            var drawComponents = entry.getValue();
            for (int i = 0; i < drawComponents.size(); i++) {
                drawComponents.get(i).draw();
            }
        }
    }
    
    void drawCutout() {
        if (!enabled) {
            return;
        }
        
        glActiveTexture(DYNAMIC_MATRIX_TEXTURE_UNIT_GL);
        glBindTexture(GL_TEXTURE_BUFFER, dynamicMatrixTexture);
        glActiveTexture(DYNAMIC_LIGHT_TEXTURE_UNIT_GL);
        glBindTexture(GL_TEXTURE_BUFFER, dynamicLightTexture);
        
        if (!BASE_INSTANCE) {
            B3DStateHelper.bindArrayBuffer(instanceDataBuffer.handle());
        }
        
        final var program = GLCore.INSTANCE.mainProgram;
        
        glActiveTexture(ATLAS_TEXTURE_UNIT_GL);
        
        glBindVertexArray(VAO);
        for (var entry : cutoutDrawComponents.entrySet()) {
            program.setupRenderPass(entry.getKey());
            var drawComponents = entry.getValue();
            for (int i = 0; i < drawComponents.size(); i++) {
                drawComponents.get(i).draw();
            }
        }
    }
}
