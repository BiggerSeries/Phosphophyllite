package net.roguelogix.phosphophyllite.quartz.internal.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.CrashReport;
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
import org.lwjgl.opengl.GL;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.lwjgl.opengl.ARBDrawIndirect.GL_DRAW_INDIRECT_BUFFER;
import static org.lwjgl.opengl.ARBSeparateShaderObjects.glBindProgramPipeline;
import static org.lwjgl.opengl.GL32C.*;

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
            LOGGER.error("Unable to initialize Quartz GLCore due to missing GL 3.2 capabilities, this shouldn't be possible");
            return null;
        }
        if (!capabilities.GL_ARB_separate_shader_objects || !capabilities.GL_ARB_explicit_attrib_location || !capabilities.GL_ARB_instanced_arrays) {
            final var builder = new StringBuilder();
            builder.append("Unable to initialize Quartz GLCore due to missing GL extension, report this error!\n");
            builder.append("ISSUE URL -> https://github.com/BiggerSeries/Phosphophyllite/issues\n");
            builder.append("GL_ARB_separate_shader_objects : ").append(capabilities.GL_ARB_separate_shader_objects).append('\n');
            builder.append("GL_ARB_explicit_attrib_location : ").append(capabilities.GL_ARB_explicit_attrib_location).append('\n');
            builder.append("GL_ARB_instanced_arrays : ").append(capabilities.GL_ARB_instanced_arrays).append('\n');
            builder.append("GL_VENDOR : ").append(glGetString(GL_VENDOR)).append('\n');
            builder.append("GL_RENDERER : ").append(glGetString(GL_RENDERER)).append('\n');
            builder.append("GL_VERSION : ").append(glGetString(GL_VERSION)).append('\n');
            builder.append("GL_SHADING_LANGUAGE_VERSION : ").append(glGetString(GL_SHADING_LANGUAGE_VERSION)).append('\n');
            
            final var extensionCount = glGetInteger(GL_NUM_EXTENSIONS);
            builder.append("Supported OpenGL Extensions : ").append(extensionCount).append('\n');
            for (int i = 0; i < extensionCount; i++) {
                builder.append(glGetStringi(GL_EXTENSIONS, i)).append('\n');
            }
            
            // this is the backup impl, so this is ok to do
            Minecraft.crash(new CrashReport("Quartz startup failed", new IllegalStateException(builder.toString())));
            return null;
        }
        LOGGER.info("Quartz initializing GLCore");
        return new GLCore();
    }
    
    private final boolean DRAW_INDIRECT = GL.getCapabilities().GL_ARB_draw_indirect && GLConfig.INSTANCE.ALLOW_MULTIDRAW_INDIRECT;
    
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
        GLRenderPass.resourcesReloaded();
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
                byteBuffer.putInt(element);
                byteBuffer.putInt(element + 1);
                byteBuffer.putInt(element + 3);
                byteBuffer.putInt(element + 3);
                byteBuffer.putInt(element + 1);
                byteBuffer.putInt(element + 2);
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
        long deltaNano = timeNanos - lastTimeNano;
        lastTimeNano = timeNanos;
        if (lastTimeNano == 0) {
            deltaNano = 0;
        }
        
        vertexBuffer.flush();
        
        var playerPosition = pActiveRenderInfo.getPosition();
        drawInfo.playerPosition.set((int) playerPosition.x, (int) playerPosition.y, (int) playerPosition.z);
        drawInfo.playerSubBlock.set(playerPosition.x - (int) playerPosition.x, playerPosition.y - (int) playerPosition.y, playerPosition.z - (int) playerPosition.z);
        pProjection.store(drawInfo.projectionMatrixFloatBuffer);
        drawInfo.projectionMatrix.set(drawInfo.projectionMatrixFloatBuffer);
        pMatrixStack.last().pose().store(drawInfo.projectionMatrixFloatBuffer);
        drawInfo.projectionMatrix.mul(new net.roguelogix.phosphophyllite.repack.org.joml.Matrix4f().set(drawInfo.projectionMatrixFloatBuffer));
        drawInfo.projectionMatrix.get(drawInfo.projectionMatrixFloatBuffer);
        drawInfo.deltaNano = deltaNano;
        drawInfo.partialTicks = pPartialTicks;
    }
    
    @Override
    public void lightUpdated() {
        lightEngine.update(Minecraft.getInstance().level);
    }
    
    @Override
    public void preTerrainSetup() {
        drawInfo.fogStart = RenderSystem.getShaderFogStart();
        drawInfo.fogEnd = drawInfo.fogStart == Float.MAX_VALUE ? Float.MAX_VALUE : RenderSystem.getShaderFogEnd();
        drawInfo.fogColor.set(RenderSystem.getShaderFogColor());
        
        mainProgram.setupDrawInfo(drawInfo);
        
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
        if (DRAW_INDIRECT) {
            glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
        }
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDisable(GL_DEPTH_TEST);
    }
    
    @Override
    public void endOpaque() {
    
    }
    
    @Override
    public void endTranslucent() {
    
    }
}
