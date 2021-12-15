package net.roguelogix.phosphophyllite.quartz.internal.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.roguelogix.phosphophyllite.quartz.DrawBatch;
import net.roguelogix.phosphophyllite.quartz.internal.MagicNumbers;
import net.roguelogix.phosphophyllite.quartz.internal.QuartzCore;
import net.roguelogix.phosphophyllite.quartz.internal.common.DrawInfo;
import net.roguelogix.phosphophyllite.threading.WorkQueue;
import net.roguelogix.phosphophyllite.util.MethodsReturnNonnullByDefault;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.lwjgl.opengl.ARBSeparateShaderObjects.glBindProgramPipeline;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL11C.GL_LEQUAL;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.glActiveTexture;
import static org.lwjgl.opengl.GL20C.glUseProgram;
import static org.lwjgl.opengl.GL30C.glBindVertexArray;
import static org.lwjgl.opengl.GL31C.GL_TEXTURE_BUFFER;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GLCore extends QuartzCore {
    
    // its fine, in the event its null, nothing will need it to not be null
    @SuppressWarnings("ConstantConditions")
    @Nonnull
    public static final GLCore INSTANCE = attemptCreate();
    
    @Nullable
    public static GLCore attemptCreate() {
        var capabilities = GL.getCapabilities();
        if (!capabilities.OpenGL32) {
            return null;
        }
        if (!capabilities.GL_ARB_separate_shader_objects) {
            return null;
        }
        if (!capabilities.GL_ARB_explicit_attrib_location) {
            return null;
        }
        if (!capabilities.GL_ARB_instanced_arrays) {
            return null;
        }
        return new GLCore();
    }
    
    public static final WorkQueue deletionQueue = new WorkQueue();
    
    public GLMainProgram mainProgram = new GLMainProgram();
    public GLBuffer vertexBuffer = meshManager.vertexBuffer.as(GLBuffer.class);
    public GLBuffer elementBuffer = allocBuffer();
    public GLBuffer.Allocation elementBufferAllocation = elementBuffer.alloc(1);
    
    private final ObjectArrayList<WeakReference<GLDrawBatch>> batchers = new ObjectArrayList<>();
    
    public static final DrawInfo drawInfo = new DrawInfo();
    private long lastTimeNano = 0;
    
    @Override
    protected void startupInternal() {
    }
    
    @Override
    protected void shutdownInternal() {
        // *everything is final*
        // OH NO, anyway
        for (Field declaredField : GLCore.class.getDeclaredFields()) {
            declaredField.setAccessible(true);
            if (!Modifier.isStatic(declaredField.getModifiers()) && Object.class.isAssignableFrom(declaredField.getType())) {
                try {
                    declaredField.set(this, null);
                } catch (IllegalAccessException ignored) {
                }
            }
        }
        // clean everything up, hopefully
        do {
            System.gc();
        } while (deletionQueue.runAll());
        System.gc();
    }
    
    @Override
    protected void resourcesReloadedInternal() {
        mainProgram.reload();
    }
    
    @Override
    public DrawBatch createDrawBatch() {
        var batcher = new GLDrawBatch();
        batchers.add(new WeakReference<>(batcher));
        return batcher;
    }
    
    @Override
    public GLBuffer allocBuffer() {
        return new GLBuffer(false, 1);
    }
    
    public void ensureElementBufferLength(int faceCount) {
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
            elementBufferAllocation.dirty();
            elementBuffer.flush();
        }
    }
    
    @Override
    public void frameStart(PoseStack pMatrixStack, float pPartialTicks, long pFinishTimeNano, boolean pDrawBlockOutline, Camera pActiveRenderInfo, GameRenderer pGameRenderer, LightTexture pLightmap, Matrix4f pProjection) {
//        GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        
        deletionQueue.runAll();
        
        long timeNanos = System.nanoTime();
        long deltaNano = lastTimeNano - timeNanos;
        lastTimeNano = timeNanos;
        if (lastTimeNano == 0) {
            deltaNano = 0;
        }
        
        var playerPosition = pActiveRenderInfo.getPosition();
        drawInfo.playerPosition.set((int) playerPosition.x, (int) playerPosition.y, (int) playerPosition.z);
        drawInfo.playerSubBlock.set(playerPosition.x - (int) playerPosition.x, playerPosition.y - (int) playerPosition.y, playerPosition.z - (int) playerPosition.z);
        pProjection.store(drawInfo.projectionMatrixFloatBuffer);
        drawInfo.projectionMatrix.set(drawInfo.projectionMatrixFloatBuffer);
        pMatrixStack.last().pose().store(drawInfo.projectionMatrixFloatBuffer);
        drawInfo.projectionMatrix.mul(new net.roguelogix.phosphophyllite.repack.org.joml.Matrix4f().set(drawInfo.projectionMatrixFloatBuffer));
        drawInfo.projectionMatrix.get(drawInfo.projectionMatrixFloatBuffer);
        drawInfo.fogStart = RenderSystem.getShaderFogStart();
        drawInfo.fogEnd = drawInfo.fogStart == Float.MAX_VALUE ? Float.MAX_VALUE : RenderSystem.getShaderFogEnd();
        drawInfo.fogColor.set(RenderSystem.getShaderFogColor());
        drawInfo.deltaNano = deltaNano;
        drawInfo.partialTicks = pPartialTicks;
        
        mainProgram.setupDrawInfo(drawInfo);
        
        vertexBuffer.flush();
    }
    
    @Override
    public void lightUpdated() {
        lightEngine.update(Minecraft.getInstance().level);
    }
    
    @Override
    public void preTerrainSetup() {
        for (int i = 0; i < batchers.size(); i++) {
            var batch = batchers.get(i).get();
            if (batch != null) {
                batch.updateAndCull(drawInfo);
            }
        }
    }
    
    @Override
    public void preOpaque() {
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        
        glUseProgram(0);
        
        glActiveTexture(MagicNumbers.GL.LIGHTMAP_TEXTURE_UNIT_GL);
        glBindTexture(GL_TEXTURE_2D, Minecraft.getInstance().gameRenderer.lightTexture().lightTexture.getId());
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        
        for (int i = 0; i < batchers.size(); i++) {
            var batch = batchers.get(i).get();
            if (batch != null) {
                batch.drawOpaque();
            }
        }
        for (int i = 0; i < batchers.size(); i++) {
            var batch = batchers.get(i).get();
            if (batch != null) {
                batch.drawCutout();
            }
        }
        
        glBindVertexArray(0);
        glBindProgramPipeline(0);
        for (int i = 0; i < 16; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_BUFFER, 0);
        }
        glActiveTexture(GL_TEXTURE0);
        glDisable(GL_DEPTH_TEST);
    }
    
    @Override
    public void endOpaque() {
    
    }
    
    @Override
    public void endTranslucent() {
    
    }
}
