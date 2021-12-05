package net.roguelogix.phosphophyllite.quartz.internal.gl33;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.RenderType;
import net.roguelogix.phosphophyllite.quartz.*;
import net.roguelogix.phosphophyllite.quartz.internal.common.*;
import net.roguelogix.phosphophyllite.quartz.internal.common.gl.GLBuffer;
import net.roguelogix.phosphophyllite.quartz.internal.common.gl.GLDeletable;
import net.roguelogix.phosphophyllite.quartz.internal.common.light.DynamicLightManager;
import net.roguelogix.phosphophyllite.quartz.internal.common.light.LightEngine;
import net.roguelogix.phosphophyllite.quartz.internal.common.mesh.StaticMesh;
import net.roguelogix.phosphophyllite.quartz.internal.common.mesh.StaticMeshManager;
import net.roguelogix.phosphophyllite.repack.org.joml.*;
import net.roguelogix.phosphophyllite.util.MethodsReturnNonnullByDefault;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import java.util.function.Consumer;

import static net.roguelogix.phosphophyllite.quartz.internal.common.MagicNumbers.*;
import static org.lwjgl.opengl.GL33C.*;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GL33DrawBatcher implements QuartzDrawBatcher {
    
    private static final Matrix4fc IDENTITY_MATRIX = new Matrix4f();
    private static final Matrix4f SCRATCH_NORMAL_MATRIX = new Matrix4f();
    
    private class MeshDrawManager implements GLDeletable {
        
        private final StaticMeshManager.TrackedMesh trackedMesh;
        private final ObjectArrayList<DrawComponent> components = new ObjectArrayList<>();
        
        private GLBuffer.Allocation worldPosAllocation;
        private int worldPosBaseID = 0;
        private GLBuffer.Allocation staticMatrixAllocation;
        private int staticMatrixBaseID = 0;
        private GLBuffer.Allocation dynamicMatrixIDAllocation;
        private int dynamicMatrixIdOffset = 0;
        private GLBuffer.Allocation lightIDAllocation;
        private int lightIDOffset = 0;
        private int instanceCount = 0;
        
        private final ObjectArrayList<Instance> liveInstances = new ObjectArrayList<>();
        
        private class DrawComponent implements GLDeletable {
            private final RenderType renderType;
            private final boolean QUAD;
            public final int GL_MODE;
            private int drawIndex;
            
            private final int baseVertex;
            private final int elementCount;
            
            private DrawComponent(RenderType renderType, StaticMeshManager.TrackedMesh.Component component) {
                this.renderType = renderType;
                GL33RenderPass renderPass = renderPasses.computeIfAbsent(renderType, ignored -> new GL33RenderPass(renderType));
                QUAD = renderPass.QUAD;
                GL_MODE = renderPass.GL_MODE;
                
                baseVertex = component.vertexOffset();
                int elementCountTemp = component.vertexCount();
                if (QUAD) {
                    elementCountTemp *= 6;
                    elementCountTemp /= 4;
                    ensureElementBufferLength(elementCountTemp / 6);
                }
                elementCount = elementCountTemp;
                var drawComponents = renderTypeDrawComponents.computeIfAbsent(renderType, e -> new ObjectArrayList<>());
                drawIndex = drawComponents.size();
                drawComponents.add(this);
                
            }
            
            private void draw() {
                if (instanceCount == 0) {
                    return;
                }
                program.setupDrawComponent(worldPosBaseID, staticMatrixBaseID, dynamicMatrixIdOffset, lightIDOffset);
                if (QUAD) {
                    glDrawElementsInstancedBaseVertex(GL_TRIANGLES, elementCount, GL_UNSIGNED_INT, 0, instanceCount, baseVertex);
                } else {
                    glDrawArraysInstanced(GL_MODE, baseVertex, elementCount, instanceCount);
                }
            }
            
            @Override
            public void delete() {
                if (drawIndex == -1) {
                    return;
                }
                var drawComponents = renderTypeDrawComponents.get(renderType);
                var removed = drawComponents.pop();
                if (drawIndex < drawComponents.size()) {
                    removed.drawIndex = drawIndex;
                    drawComponents.set(drawIndex, removed);
                }
                drawIndex = -1;
            }
        }
        
        private MeshDrawManager(StaticMesh mesh) {
            trackedMesh = meshManager.getMeshInfo(mesh);
            if (trackedMesh == null) {
                throw new IllegalArgumentException("Unable to find mesh in mesh registry");
            }
            
            worldPosAllocation = worldPosBuffer.alloc(VEC4_BYTE_SIZE, VEC4_BYTE_SIZE);
            staticMatrixAllocation = staticMatrixBuffer.alloc(MATRIX_4F_BYTE_SIZE_2, MATRIX_4F_BYTE_SIZE_2);
            dynamicMatrixIDAllocation = dynamicMatrixIDBuffer.alloc(INT_BYTE_SIZE, INT_BYTE_SIZE);
            lightIDAllocation = lightIDBuffer.alloc(INT_BYTE_SIZE, INT_BYTE_SIZE);
            staticMatrixAllocation.addReallocCallback(this::staticMatrixMoved);
            dynamicMatrixIDAllocation.addReallocCallback(this::dynamicMatrixIDMoved);
            worldPosAllocation.addReallocCallback(this::worldPosBaseIDMoved);
            lightIDAllocation.addReallocCallback(this::lightIDOffsetMoved);
            
            onRebuild();
            trackedMesh.addBuildCallback(this::onRebuild);
        }
        
        private void onRebuild() {
            for (int i = 0; i < components.size(); i++) {
                components.get(i).delete();
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
        
        private void staticMatrixMoved(GLBuffer.Allocation allocation) {
            staticMatrixBaseID = allocation.offset() / MATRIX_4F_BYTE_SIZE_2;
        }
        
        private void dynamicMatrixIDMoved(GLBuffer.Allocation allocation) {
            dynamicMatrixIdOffset = allocation.offset() / INT_BYTE_SIZE;
        }
        
        private void worldPosBaseIDMoved(GLBuffer.Allocation allocation) {
            worldPosBaseID = allocation.offset() / VEC4_BYTE_SIZE;
        }
        
        private void lightIDOffsetMoved(GLBuffer.Allocation allocation) {
            lightIDOffset = allocation.offset() / INT_BYTE_SIZE;
        }
        
        Instance createInstance(Vector3ic worldPosition, int dynamicMatrixID, Matrix4fc staticMatrix, int lightID) {
            
            if (worldPosAllocation.size() < (instanceCount + 1) * VEC4_BYTE_SIZE) {
                worldPosAllocation = worldPosBuffer.realloc(worldPosAllocation, worldPosAllocation.size() * 2, VEC4_BYTE_SIZE);
            }
            worldPosition.get(instanceCount * VEC4_BYTE_SIZE, worldPosAllocation.buffer());
            worldPosAllocation.flushRange(instanceCount * VEC4_BYTE_SIZE, VEC4_BYTE_SIZE);
            
            if (dynamicMatrixIDAllocation.size() < (instanceCount + 1) * INT_BYTE_SIZE) {
                dynamicMatrixIDAllocation = dynamicMatrixIDBuffer.realloc(dynamicMatrixIDAllocation, dynamicMatrixIDAllocation.size() * 2, INT_BYTE_SIZE);
            }
            dynamicMatrixIDAllocation.buffer().putInt(instanceCount * INT_BYTE_SIZE, Integer.reverseBytes(dynamicMatrixID));
            dynamicMatrixIDAllocation.flushRange(instanceCount * INT_BYTE_SIZE, INT_BYTE_SIZE);
            
            if (staticMatrixAllocation.size() < (instanceCount + 1) * MATRIX_4F_BYTE_SIZE_2) {
                staticMatrixAllocation = staticMatrixBuffer.realloc(staticMatrixAllocation, staticMatrixAllocation.size() * 2, MATRIX_4F_BYTE_SIZE_2);
            }
            staticMatrix.get(instanceCount * MATRIX_4F_BYTE_SIZE_2, staticMatrixAllocation.buffer());
            staticMatrix.normal(SCRATCH_NORMAL_MATRIX).get(instanceCount * MATRIX_4F_BYTE_SIZE_2 + MATRIX_4F_BYTE_SIZE, staticMatrixAllocation.buffer());
            staticMatrixAllocation.flushRange(instanceCount * MATRIX_4F_BYTE_SIZE_2, MATRIX_4F_BYTE_SIZE_2);
            
            if (lightIDAllocation.size() < (instanceCount + 1) * INT_BYTE_SIZE) {
                lightIDAllocation = lightIDBuffer.realloc(lightIDAllocation, lightIDAllocation.size() * 2, INT_BYTE_SIZE);
            }
            lightIDAllocation.buffer().putInt(instanceCount * INT_BYTE_SIZE, Integer.reverseBytes(lightID));
            lightIDAllocation.flushRange(instanceCount * INT_BYTE_SIZE, INT_BYTE_SIZE);
            
            
            Instance instance = new Instance();
            instance.location = instanceCount++;
            liveInstances.add(instance);
            return instance;
        }
        
        void removeInstance(Instance instance) {
            if (instance.location == -1) {
                return;
            }
            instanceCount--;
            Instance endInstance = liveInstances.pop();
            if (instance == endInstance) {
                instance.location = -1;
                return;
            }
            // swapping time!
            worldPosAllocation.copy(instanceCount * VEC4_BYTE_SIZE, instance.location * VEC4_BYTE_SIZE, VEC4_BYTE_SIZE);
            staticMatrixAllocation.copy(instanceCount * MATRIX_4F_BYTE_SIZE_2, instance.location * MATRIX_4F_BYTE_SIZE_2, MATRIX_4F_BYTE_SIZE_2);
            dynamicMatrixIDAllocation.copy(instanceCount * INT_BYTE_SIZE, instance.location * INT_BYTE_SIZE, INT_BYTE_SIZE);
            lightIDAllocation.copy(instanceCount * INT_BYTE_SIZE, instance.location * INT_BYTE_SIZE, INT_BYTE_SIZE);
            endInstance.location = instance.location;
            instance.location = -1;
            liveInstances.set(endInstance.location, endInstance);
        }
        
        @Override
        public void delete() {
            while (!liveInstances.isEmpty()) {
                liveInstances.peek(0).dispose();
            }
        }
        
        private class Instance implements QuartzDrawBatcher.Instance {
            
            private int location = -1;
            private DynamicLightManager.DynamicLight ownedLight;
            
            @Override
            public void dispose() {
                removeInstance(this);
                if (ownedLight != null) {
                    ownedLight.dispose();
                    ownedLight = null;
                }
            }
            
            @Override
            public void updateDynamicMatrix(@Nullable QuartzDynamicMatrix newDynamicMatrix) {
                if (newDynamicMatrix instanceof DynamicMatrixManager.DynamicMatrix dynamicMatrix && dynamicMatrixManager.owns(dynamicMatrix)) {
                    dynamicMatrixIDAllocation.buffer().putInt(location * INT_BYTE_SIZE, Integer.reverseBytes(dynamicMatrix.id()));
                    dynamicMatrixIDAllocation.flushRange(location * INT_BYTE_SIZE, INT_BYTE_SIZE);
                }
            }
            
            @Override
            public void updateStaticMatrix(Matrix4fc newStaticMatrix) {
                newStaticMatrix.get(location * MATRIX_4F_BYTE_SIZE_2, staticMatrixAllocation.buffer());
                newStaticMatrix.normal(SCRATCH_NORMAL_MATRIX).get(location * MATRIX_4F_BYTE_SIZE_2 + MATRIX_4F_BYTE_SIZE, staticMatrixAllocation.buffer());
                staticMatrixAllocation.flushRange(location * MATRIX_4F_BYTE_SIZE_2, MATRIX_4F_BYTE_SIZE_2);
            }
            
            @Override
            public void updateDynamicLight(@Nullable QuartzDynamicLight newDynamicLight) {
                int newLightID = -1;
                if (newDynamicLight == null) {
                    if (ownedLight == null) {
                        newLightID = ZERO_LEVEL_LIGHT.id();
                    } else {
                        newLightID = ownedLight.id();
                    }
                } else if (newDynamicLight instanceof DynamicLightManager.DynamicLight dynamicLight && lightManager.owns(dynamicLight)) {
                    newLightID = dynamicLight.id();
                }
                lightIDAllocation.buffer().putInt(location * INT_BYTE_SIZE, Integer.reverseBytes(newLightID));
                lightIDAllocation.flushRange(location * INT_BYTE_SIZE, INT_BYTE_SIZE);
            }
        }
    }
    
    private final StaticMeshManager meshManager;
    private final LightEngine lightEngine;
    private final GL33MainProgram program;
    private final Consumer<GL33DrawBatcher> deleteCallback;
    
    private final DynamicMatrixManager dynamicMatrixManager = new DynamicMatrixManager();
    private final DynamicMatrixManager.DynamicMatrix identityDynamicMatrix = dynamicMatrixManager.alloc(null, (matrix, nanoSinceLastFrame, partialTicks, playerBlock, playerPartialBlock) -> matrix.write(IDENTITY_MATRIX));
    private final DynamicLightManager lightManager = new DynamicLightManager();
    private final DynamicLightManager.DynamicLight ZERO_LEVEL_LIGHT = lightManager.alloc((light, blockAndTintGetter) -> light.write((byte) 0, (byte) 0, (byte) 0));
    
    private final GLBuffer worldPosBuffer = new GL33Buffer(false);
    private final GLBuffer dynamicMatrixIDBuffer = new GL33Buffer(false);
    private final GLBuffer staticMatrixBuffer = new GL33Buffer(false);
    private final GLBuffer lightIDBuffer = new GL33Buffer(false);
    
    // TODO: global element buffer, they are all identical
    private final GLBuffer elementBuffer = new GL33Buffer(false);
    private GLBuffer.Allocation elementBufferAllocation;
    
    private final int worldPosTexture;
    private final int dynamicMatrixIDTexture;
    private final int dynamicMatrixTexture;
    private final int staticMatrixTexture;
    private final int lightIDTexture;
    private final int lightsTexture;
    private final int VAO;
    
    private final Object2ObjectMap<StaticMesh, MeshDrawManager> drawManagers = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectMap<RenderType, GL33RenderPass> renderPasses = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectMap<RenderType, ObjectArrayList<MeshDrawManager.DrawComponent>> renderTypeDrawComponents = new Object2ObjectArrayMap<>();
    
    private AABBi cullAABB = null;
    private boolean enabled = true;
    
    public GL33DrawBatcher(StaticMeshManager meshManager, LightEngine lightEngine, GL33MainProgram program, Consumer<GL33DrawBatcher> deleteCallback) {
        this.meshManager = meshManager;
        this.lightEngine = lightEngine;
        this.program = program;
        this.deleteCallback = deleteCallback;
    
        elementBufferAllocation = elementBuffer.alloc(1);
    
        VAO = glGenVertexArrays();
        B3DStateHelper.bindVertexArray(VAO);
        B3DStateHelper.bindArrayBuffer(meshManager.vertexBuffer.handle());
        glVertexAttribPointer(POSITION_LOCATION, 3, GL_FLOAT, false, 32, 0);
        glVertexAttribIPointer(COLOR_LOCATION, 1, GL_INT, 32, 12);
        glVertexAttribPointer(TEX_COORD_LOCATION, 2, GL_FLOAT, false, 32, 16);
        glVertexAttribIPointer(LIGHTINFO_LOCATION, 2, GL_INT, 32, 24);
        glEnableVertexAttribArray(POSITION_LOCATION);
        glEnableVertexAttribArray(COLOR_LOCATION);
        glEnableVertexAttribArray(TEX_COORD_LOCATION);
        glEnableVertexAttribArray(LIGHTINFO_LOCATION);
        B3DStateHelper.bindElementBuffer(elementBuffer.handle());
        glBindVertexArray(0);
        
        glActiveTexture(GL_TEXTURE0);
        
        worldPosTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_BUFFER, worldPosTexture);
        glTexBuffer(GL_TEXTURE_BUFFER, GL_RGBA32I, worldPosBuffer.handle());
        
        dynamicMatrixIDTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_BUFFER, dynamicMatrixIDTexture);
        glTexBuffer(GL_TEXTURE_BUFFER, GL_R32UI, dynamicMatrixIDBuffer.handle());
        dynamicMatrixTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_BUFFER, dynamicMatrixTexture);
        glTexBuffer(GL_TEXTURE_BUFFER, GL_RGBA32F, dynamicMatrixManager.buffer().handle());
        
        staticMatrixTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_BUFFER, staticMatrixTexture);
        glTexBuffer(GL_TEXTURE_BUFFER, GL_RGBA32F, staticMatrixBuffer.handle());
        
        lightIDTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_BUFFER, lightIDTexture);
        glTexBuffer(GL_TEXTURE_BUFFER, GL_R32UI, lightIDBuffer.handle());
        lightsTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_BUFFER, lightsTexture);
        glTexBuffer(GL_TEXTURE_BUFFER, GL_RG8UI, lightManager.buffer().handle());
        
        glBindTexture(GL_TEXTURE_BUFFER, 0);
    }
    
    @Override
    public void dispose() {
        deleteCallback.accept(this);
        enabled = false;
        glDeleteTextures(lightsTexture);
        glDeleteTextures(lightIDTexture);
        glDeleteTextures(staticMatrixTexture);
        glDeleteTextures(dynamicMatrixTexture);
        glDeleteTextures(dynamicMatrixIDTexture);
        glDeleteTextures(worldPosTexture);
        glDeleteVertexArrays(VAO);
        renderTypeDrawComponents.clear();
        renderPasses.forEach((renderType, renderPass) -> renderPass.delete());
        drawManagers.forEach((staticMesh, meshDrawManager) -> meshDrawManager.delete());
        elementBufferAllocation.delete();
        elementBuffer.delete();
        lightIDBuffer.delete();
        staticMatrixBuffer.delete();
        dynamicMatrixIDBuffer.delete();
        worldPosBuffer.delete();
        lightManager.delete();
        identityDynamicMatrix.dispose();
        dynamicMatrixManager.delete();
    }
    
    @Override
    @Nullable
    public Instance createInstance(Vector3ic position, QuartzStaticMesh quartzMesh, @Nullable QuartzDynamicMatrix quartzDynamicMatrix, Matrix4fc staticMatrix, @Nullable QuartzDynamicLight quartzDynamicLight, @Nullable QuartzDynamicLight.Type lightType) {
        if (!(quartzMesh instanceof StaticMesh mesh)) {
            return null;
        }
        if (quartzDynamicMatrix == null) {
            quartzDynamicMatrix = identityDynamicMatrix;
        }
        if (!(quartzDynamicMatrix instanceof DynamicMatrixManager.DynamicMatrix dynamicMatrix) || !dynamicMatrixManager.owns(dynamicMatrix)) {
            return null;
        }
        DynamicLightManager.DynamicLight ownedLight = null;
        if (quartzDynamicLight == null) {
            // cache and reuse these for each block
            if(lightType == null){
                lightType = QuartzDynamicLight.Type.SMOOTH; // TODO: automatic
            }
            quartzDynamicLight = ownedLight = lightEngine.createLightForPos(position, lightManager, lightType);
        }
        if (!(quartzDynamicLight instanceof DynamicLightManager.DynamicLight light) || !lightManager.owns(light)) {
            return null;
        }
        var drawManager = drawManagers.computeIfAbsent(mesh, MeshDrawManager::new);
        
        var instance = drawManager.createInstance(position, dynamicMatrix.id(), staticMatrix, light.id());
        instance.ownedLight = ownedLight;
        return instance;
    }
    
    @Override
    public QuartzDynamicMatrix createDynamicMatrix(@Nullable QuartzDynamicMatrix parentTransform, @Nullable Quartz.DynamicMatrixUpdateFunc updateFunc) {
        return dynamicMatrixManager.alloc(parentTransform, updateFunc);
    }
    
    @Override
    public QuartzDynamicLight createLight(Vector3ic lightPosition, QuartzDynamicLight.Type lightType) {
        return lightEngine.createLightForPos(lightPosition, lightManager, lightType);
    }
    
    @Override
    public void setCullAABB(AABBi aabb) {
        this.cullAABB = new AABBi(aabb);
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    void ensureElementBufferLength(int faceCount) {
        if (elementBufferAllocation.size() < faceCount * 6) {
            int newFaceCount = Integer.highestOneBit(faceCount);
            if (newFaceCount < faceCount) {
                newFaceCount <<= 1;
            }
            elementBufferAllocation.allocator().free(elementBufferAllocation);
            elementBufferAllocation = elementBufferAllocation.allocator().alloc(newFaceCount * 24);
            final var byteBuffer = elementBufferAllocation.buffer();
            int element = 0;
            for (int i = 0; i < newFaceCount; i++) {
                byteBuffer.putInt(Integer.reverseBytes(element));
                byteBuffer.putInt(Integer.reverseBytes(element + 1));
                byteBuffer.putInt(Integer.reverseBytes(element + 3));
                byteBuffer.putInt(Integer.reverseBytes(element + 3));
                byteBuffer.putInt(Integer.reverseBytes(element + 1));
                byteBuffer.putInt(Integer.reverseBytes(element + 2));
                element += 4;
            }
        }
        elementBufferAllocation.flush();
    }
    
    void drawOpaque(DrawInfo drawInfo) {
        if (!enabled) {
            return;
        }
    
        dynamicMatrixManager.updateAll(drawInfo.deltaNano, drawInfo.partialTicks, drawInfo.playerPosition, drawInfo.playerSubBlock);
    
        glActiveTexture(GL33.WORLD_POSITIONS_TEXTURE_UNIT_GL);
        glBindTexture(GL_TEXTURE_BUFFER, worldPosTexture);
        
        glActiveTexture(GL33.DYNAMIC_MATRIX_ID_TEXTURE_UNIT_GL);
        glBindTexture(GL_TEXTURE_BUFFER, dynamicMatrixIDTexture);
        glActiveTexture(GL33.DYNAMIC_MATRIX_TEXTURE_UNIT_GL);
        glBindTexture(GL_TEXTURE_BUFFER, dynamicMatrixTexture);
        
        glActiveTexture(GL33.STATIC_MATRIX_TEXTURE_UNIT_GL);
        glBindTexture(GL_TEXTURE_BUFFER, staticMatrixTexture);
        
        glActiveTexture(GL33.DYNAMIC_LIGHT_ID_TEXTURE_UNIT_GL);
        glBindTexture(GL_TEXTURE_BUFFER, lightIDTexture);
        glActiveTexture(GL33.DYNAMIC_LIGHT_TEXTURE_UNIT_GL);
        glBindTexture(GL_TEXTURE_BUFFER, lightsTexture);
        
        glActiveTexture(GL33.ATLAS_TEXTURE_UNIT_GL);
        
        glBindVertexArray(VAO);
        for (var entry : renderTypeDrawComponents.entrySet()) {
            program.setupRenderPass(renderPasses.get(entry.getKey()));
            var drawComponents = entry.getValue();
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < drawComponents.size(); i++) {
                drawComponents.get(i).draw();
            }
        }
    }
}
