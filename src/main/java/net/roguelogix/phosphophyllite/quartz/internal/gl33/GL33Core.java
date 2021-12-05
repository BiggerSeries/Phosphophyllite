package net.roguelogix.phosphophyllite.quartz.internal.gl33;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.roguelogix.phosphophyllite.quartz.*;
import net.roguelogix.phosphophyllite.quartz.internal.common.DrawInfo;
import net.roguelogix.phosphophyllite.quartz.internal.QuartzCore;
import net.roguelogix.phosphophyllite.quartz.internal.common.gl.GLBuffer;
import net.roguelogix.phosphophyllite.quartz.internal.common.MagicNumbers;
import net.roguelogix.phosphophyllite.repack.org.joml.Matrix4fc;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3ic;
import net.roguelogix.phosphophyllite.util.MethodsReturnNonnullByDefault;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;

import static org.lwjgl.opengl.ARBSeparateShaderObjects.glBindProgramPipeline;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL11C.GL_LEQUAL;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.glActiveTexture;
import static org.lwjgl.opengl.GL20C.glUseProgram;
import static org.lwjgl.opengl.GL30C.glBindVertexArray;
import static org.lwjgl.opengl.GL31C.GL_TEXTURE_BUFFER;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class GL33Core extends QuartzCore {
    
    private final GL33MainProgram renderPassProgram = new GL33MainProgram();
    private final GL33InstancedRendering instancedRendering = new GL33InstancedRendering(staticMeshManager, dynamicMatrixManager, dynamicLightManager, renderPassProgram);
    private final ObjectArrayList<GL33DrawBatcher> batchers = new ObjectArrayList<>();
    
    @SubscribeEvent
    public void onResourceLoad(QuartzEvent.ResourcesLoaded event) {
        try {
            renderPassProgram.reload();
        } catch (IllegalStateException e) {
            QuartzCore.LOGGER.warn("Failed to reload shader " + renderPassProgram.baseResourceLocation);
        }
    }
    
    @Override
    public void delete() {
        instancedRendering.delete();
        renderPassProgram.delete();
        super.delete();
    }
    
    @Override
    public void drawFirst(DrawInfo drawInfo) {
    
        // for debugging, this shouldn't be here  for a release build
//        long window = Minecraft.getInstance().getWindow().getWindow();
//        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
    
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
    
        glUseProgram(0);
        glBindProgramPipeline(renderPassProgram.pipelineHandle());
        renderPassProgram.setupDrawInfo(drawInfo);
        renderPassProgram.clearAtlas();
    
        glActiveTexture(MagicNumbers.GL33.LIGHTMAP_TEXTURE_UNIT_GL);
        glBindTexture(GL_TEXTURE_2D, Minecraft.getInstance().gameRenderer.lightTexture().lightTexture.getId());
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        
        instancedRendering.draw();
    
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < batchers.size(); i++) {
            batchers.get(i).drawOpaque(drawInfo);
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
    public void registerRenderType(RenderType type) {
        instancedRendering.registerRenderType(type);
    }
    
    @Override
    public Collection<RenderType> registeredRenderTypes() {
        return instancedRendering.registeredRenderTypes();
    }
    
    @Override
    public int registerStaticMeshInstance(QuartzStaticMesh mesh, Vector3ic position, @Nullable QuartzDynamicMatrix dynamicMatrix, Matrix4fc staticTransform, QuartzDynamicLight light) {
        return instancedRendering.addInstance(mesh, position, dynamicMatrix == null ? DYNAMIC_MATRIX_0 : dynamicMatrix, staticTransform, light);
    }
    
    @Override
    public void unregisterStaticMeshInstance(int handle) {
        instancedRendering.removeInstance(handle);
    }
    
    @Override
    public GLBuffer allocBuffer(boolean dynamic) {
        return new GL33Buffer(dynamic);
    }
    
    @Override
    public GLBuffer allocBuffer(boolean dynamic, int size) {
        return new GL33Buffer(dynamic, size);
    }
    
    public QuartzDrawBatcher createDrawBatcher() {
        var batcher = new GL33DrawBatcher(staticMeshManager, lightEngine, renderPassProgram, batchers::remove);
        batchers.add(batcher);
        return batcher;
    }
}
