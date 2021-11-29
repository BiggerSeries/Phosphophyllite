package net.roguelogix.phosphophyllite.quartz.internal.gl33;

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.roguelogix.phosphophyllite.quartz.internal.common.DrawInfo;
import net.roguelogix.phosphophyllite.quartz.QuartzDynamicLight;
import net.roguelogix.phosphophyllite.quartz.QuartzDynamicMatrix;
import net.roguelogix.phosphophyllite.quartz.QuartzStaticMesh;
import net.roguelogix.phosphophyllite.quartz.internal.common.StaticMesh;
import net.roguelogix.phosphophyllite.quartz.internal.common.*;
import net.roguelogix.phosphophyllite.repack.org.joml.Matrix4f;
import net.roguelogix.phosphophyllite.repack.org.joml.Matrix4fc;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3ic;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;

import static net.roguelogix.phosphophyllite.quartz.internal.common.MagicNumbers.*;
import static org.lwjgl.opengl.GL33C.*;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GL33InstancedRendering implements GLDeletable {
    private class MeshDrawManager {
        
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
        
        private final Int2IntMap idToLocationMap = new Int2IntOpenHashMap();
        private final Int2IntMap locationToIDMap = new Int2IntOpenHashMap();
        
        private class DrawComponent implements GLDeletable {
            private final RenderType renderType;
            private final boolean QUAD;
            public final int GL_MODE;
            private int drawIndex;
            
            private final int baseVertex;
            private final int elementCount;
            
            private DrawComponent(RenderType renderType, StaticMeshManager.TrackedMesh.Component component) {
                this.renderType = renderType;
                GL33RenderPass renderPass = renderPasses.get(renderType);
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
                var drawComponents = renderTypeDrawComponents.get(renderType);
                drawIndex = drawComponents.size();
                drawComponents.add(this);
                
            }
            
            private void draw() {
                if (instanceCount == 0) {
                    return;
                }
                glUniform1i(program.WORLD_POSITION_ID_OFFSET_UNIFORM_LOCATION, worldPosBaseID);
                glUniform1i(program.STATIC_MATRIX_BASE_ID_UNIFORM_LOCATION, staticMatrixBaseID);
                glUniform1i(program.DYNAMIC_MATRIX_ID_OFFSET_UNIFORM_LOCATION, dynamicMatrixIdOffset);
                glUniform1i(program.DYNAMIC_LIGHT_ID_OFFSET_UNIFORM_LOCATION, lightIDOffset);
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
            idToLocationMap.defaultReturnValue(-1);
            locationToIDMap.defaultReturnValue(-1);
            
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
        
        void addInstance(int id, Vector3ic worldPosition, int dynamicMatrixID, Matrix4fc staticMatrix, int lightID) {
            idToLocationMap.put(id, instanceCount);
            locationToIDMap.put(instanceCount, id);
            
            if (worldPosAllocation.size() < (instanceCount + 1) * VEC4_BYTE_SIZE) {
                worldPosAllocation = worldPosBuffer.realloc(worldPosAllocation, worldPosAllocation.size() * 2, VEC4_BYTE_SIZE);
            }
            worldPosition.get(instanceCount * VEC4_BYTE_SIZE, worldPosAllocation.buffer());
//            worldPosAllocation.flush();
            worldPosAllocation.flushRange(instanceCount * VEC4_BYTE_SIZE, VEC4_BYTE_SIZE);
            
            if (dynamicMatrixIDAllocation.size() < (instanceCount + 1) * INT_BYTE_SIZE) {
                dynamicMatrixIDAllocation = dynamicMatrixIDBuffer.realloc(dynamicMatrixIDAllocation, dynamicMatrixIDAllocation.size() * 2, INT_BYTE_SIZE);
            }
            dynamicMatrixIDAllocation.buffer().putInt(instanceCount * INT_BYTE_SIZE, Integer.reverseBytes(dynamicMatrixID));
//            dynamicMatrixIDAllocation.flush();
            dynamicMatrixIDAllocation.flushRange(instanceCount * INT_BYTE_SIZE, INT_BYTE_SIZE);
            
            if (staticMatrixAllocation.size() < (instanceCount + 1) * MATRIX_4F_BYTE_SIZE_2) {
                staticMatrixAllocation = staticMatrixBuffer.realloc(staticMatrixAllocation, staticMatrixAllocation.size() * 2, MATRIX_4F_BYTE_SIZE_2);
            }
            staticMatrix.get(instanceCount * MATRIX_4F_BYTE_SIZE_2, staticMatrixAllocation.buffer());
            staticMatrix.normal(new Matrix4f()).get(instanceCount * MATRIX_4F_BYTE_SIZE_2 + MATRIX_4F_BYTE_SIZE, staticMatrixAllocation.buffer());
//            staticMatrixAllocation.flush();
            staticMatrixAllocation.flushRange(instanceCount * MATRIX_4F_BYTE_SIZE_2, MATRIX_4F_BYTE_SIZE_2);
            
            if (lightIDAllocation.size() < (instanceCount + 1) * INT_BYTE_SIZE) {
                lightIDAllocation = lightIDBuffer.realloc(lightIDAllocation, lightIDAllocation.size() * 2, INT_BYTE_SIZE);
            }
            lightIDAllocation.buffer().putInt(instanceCount * INT_BYTE_SIZE, Integer.reverseBytes(lightID));
//            lightIDAllocation.flush();
            lightIDAllocation.flushRange(instanceCount * INT_BYTE_SIZE, INT_BYTE_SIZE);
            
            instanceCount++;
        }
        
        void removeInstance(int id) {
            var location = idToLocationMap.remove(id);
            if (location == -1) {
                return;
            }
            instanceCount--;
            var endID = locationToIDMap.remove(instanceCount);
            if (location == instanceCount) {
                return;
            }
            // swapping time!
            locationToIDMap.put(location, endID);
            idToLocationMap.put(endID, location);
            worldPosAllocation.copy(instanceCount * VEC4_BYTE_SIZE, location * VEC4_BYTE_SIZE, VEC4_BYTE_SIZE);
            staticMatrixAllocation.copy(instanceCount * MATRIX_4F_BYTE_SIZE_2, location * MATRIX_4F_BYTE_SIZE_2, MATRIX_4F_BYTE_SIZE_2);
            dynamicMatrixIDAllocation.copy(instanceCount * INT_BYTE_SIZE, location * INT_BYTE_SIZE, INT_BYTE_SIZE);
            lightIDAllocation.copy(instanceCount * INT_BYTE_SIZE, location * INT_BYTE_SIZE, INT_BYTE_SIZE);
        }
    }
    
    private final GL33MainProgram program;
    private final DynamicMatrixManager dynamicMatrixManager;
    private final StaticMeshManager meshManager;
    private final DynamicLightManager lightManager;
    
    private final GLBuffer worldPosBuffer = new GL33Buffer(false);
    private final GLBuffer dynamicMatrixIDBuffer = new GL33Buffer(false);
    private final GLBuffer staticMatrixBuffer = new GL33Buffer(false);
    private final GLBuffer lightIDBuffer = new GL33Buffer(false);
    private final int worldPosTexture;
    private final int dynamicMatrixIDTexture;
    private final int dynamicMatrixTexture;
    private final int staticMatrixTexture;
    private final int lightIDTexture;
    private final int lightsTexture;
    private final GLBuffer elementBuffer = new GL33Buffer(false);
    private GLBuffer.Allocation elementBufferAllocation;
    private final int VAO;
    
    private final Object2ObjectMap<StaticMesh, MeshDrawManager> drawManagers = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectMap<RenderType, GL33RenderPass> renderPasses = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectMap<RenderType, ObjectArrayList<MeshDrawManager.DrawComponent>> renderTypeDrawComponents = new Object2ObjectArrayMap<>();
    private final Int2ObjectMap<MeshDrawManager> instanceDrawManagers = new Int2ObjectOpenHashMap<>();
    private final IntArrayList freeIds = new IntArrayList();
    private int nextID = 0;
    
    public GL33InstancedRendering(StaticMeshManager meshManager, DynamicMatrixManager matrixManager, DynamicLightManager lightManager, GL33MainProgram program) {
        this.meshManager = meshManager;
        this.lightManager = lightManager;
        dynamicMatrixManager = matrixManager;
        this.program = program;
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
    public void delete() {
        glDeleteTextures(staticMatrixTexture);
        glDeleteTextures(dynamicMatrixTexture);
        glDeleteTextures(dynamicMatrixIDTexture);
        glDeleteVertexArrays(VAO);
    }
    
    public void registerRenderType(RenderType renderType) {
        if (renderPasses.containsKey(renderType)) {
            return;
        }
        var renderPass = new GL33RenderPass(renderType, program);
        renderPasses.put(renderType, renderPass);
        renderTypeDrawComponents.put(renderType, new ObjectArrayList<>());
    }
    
    public Collection<RenderType> registeredRenderTypes() {
        return renderTypeDrawComponents.keySet();
    }
    
    public int addInstance(QuartzStaticMesh quartzMesh, Vector3ic worldPosition, QuartzDynamicMatrix quartzDynamicMatrix, Matrix4fc staticTransform, QuartzDynamicLight quartzDynamicLight) {
        if (!(quartzDynamicMatrix instanceof DynamicMatrixManager.DynamicMatrix dynamicMatrix) || !(quartzMesh instanceof StaticMesh mesh) || !(quartzDynamicLight instanceof DynamicLightManager.DynamicLight light)) {
            return -1;
        }
        
        final int id = nextID();
        
        var meshManager = drawManagers.computeIfAbsent(mesh, MeshDrawManager::new);
        meshManager.addInstance(id, worldPosition, dynamicMatrix.id(), staticTransform, light.id());
        instanceDrawManagers.put(id, meshManager);
        
        return id;
    }
    
    public void removeInstance(int id) {
        var drawManager = freeID(id);
        if (drawManager != null) {
            drawManager.removeInstance(id);
        }
        freeIds.add(id);
    }
    
    private int nextID() {
        if (!freeIds.isEmpty()) {
            return freeIds.popInt();
        }
        return nextID++;
    }
    
    @Nullable
    private MeshDrawManager freeID(int id) {
        return instanceDrawManagers.remove(id);
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
    
    public void draw(DrawInfo drawInfo) {
        // for debugging, this shouldn't be here  for a release build
//        long window = Minecraft.getInstance().getWindow().getWindow();
//        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        
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
        
        glActiveTexture(GL33.LIGHTMAP_TEXTURE_UNIT_GL);
        glBindTexture(GL_TEXTURE_2D, Minecraft.getInstance().gameRenderer.lightTexture().lightTexture.getId());
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        
        glActiveTexture(GL33.ATLAS_TEXTURE_UNIT_GL);
        
        glUseProgram(program.handle());
        glUniformMatrix4fv(program.PROJECTION_MATRIX_UNIFORM_LOCATION, false, drawInfo.projectionMatrixFloatBuffer);
        glUniform3i(program.PLAYER_BLOCK_UNIFORM_LOCATION, drawInfo.playerPosition.x, drawInfo.playerPosition.y, drawInfo.playerPosition.z);
        glUniform3f(program.PLAYER_SUB_BLOCK_UNIFORM_LOCATION, drawInfo.playerSubBlock.x, drawInfo.playerSubBlock.y, drawInfo.playerSubBlock.z);
        glUniform2f(program.FOG_START_END_UNIFORM_LOCATION, drawInfo.fogStart, drawInfo.fogEnd);
        glUniform4f(program.FOG_COLOR_UNIFORM_LOCATION, drawInfo.fogColor.x, drawInfo.fogColor.y, drawInfo.fogColor.z, 1);
        glBindVertexArray(VAO);
        program.clearAtlas();
        for (var entry : renderTypeDrawComponents.entrySet()) {
            renderPasses.get(entry.getKey()).setUniforms();
            var drawComponents = entry.getValue();
            for (int i = 0; i < drawComponents.size(); i++) {
                drawComponents.get(i).draw();
            }
        }
        glBindVertexArray(0);
        glUseProgram(0);
        for (int i = 0; i < 16; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_BUFFER, 0);
        }
        glActiveTexture(GL_TEXTURE0);
        glDisable(GL_DEPTH_TEST);
    }
}
