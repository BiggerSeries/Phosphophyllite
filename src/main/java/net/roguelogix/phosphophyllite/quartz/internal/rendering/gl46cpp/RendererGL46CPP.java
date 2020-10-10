package net.roguelogix.phosphophyllite.quartz.internal.rendering.gl46cpp;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.util.math.vector.Vector3d;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.quartz.internal.rendering.Renderer;
import net.roguelogix.phosphophyllite.quartz.internal.BlockRenderInfo;
import net.roguelogix.phosphophyllite.robn.ROBN;
import org.apache.commons.lang3.SystemUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import sun.nio.ch.DirectBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * its almost like the rest of this is in C++
 * oh wait, it is
 * go to src/main/quartzpp/src/gl46 for the rest here
 */
public class RendererGL46CPP extends Renderer {
    public RendererGL46CPP() {
        
        // Verify that we have the required OpenGL capabilities
        GLCapabilities capabilities = GL.getCapabilities();
        if (!(capabilities.OpenGL46
                && capabilities.GL_ARB_bindless_texture
//                && capabilities.GL_ARB_sparse_buffer
                && (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_WINDOWS)
                && System.getProperty("sun.arch.data.model").equals("64")
        )) {
            Phosphophyllite.LOGGER.fatal("Quartz GL46_CPP requirements not met, all requirements below must be true");
            Phosphophyllite.LOGGER.fatal("Windows or Linux: " + (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_WINDOWS));
            Phosphophyllite.LOGGER.fatal("64bit: " + System.getProperty("sun.arch.data.model").equals("64"));
            Phosphophyllite.LOGGER.fatal("OpenGL 4.6: " + capabilities.OpenGL46);
            Phosphophyllite.LOGGER.fatal("GL_ARB_bindless_texture: " + capabilities.GL_ARB_bindless_texture);
//            Phosphophyllite.LOGGER.fatal("GL_ARB_sparse_buffer: " + capabilities.GL_ARB_sparse_buffer);
            throw new IllegalStateException("Chosen Quartz mode unable to be run on this computer, required capability info printed above");
        }
        
        try {
            Phosphophyllite.LOGGER.warn("Loading Quartz++, if there is a JVM crash immediately following this, it was probably Quartz++ (module of the Phosphophyllite library)");
            String librarySuffix = SystemUtils.IS_OS_LINUX ? "so" : "dll";
            NativeUtils.loadLibraryFromJar("phosphophyllite:libs/libroguelib_threading." + librarySuffix);
            NativeUtils.loadLibraryFromJar("phosphophyllite:libs/libroguelib_exceptions." + librarySuffix);
            NativeUtils.loadLibraryFromJar("phosphophyllite:libs/libroguelib_logging." + librarySuffix);
            NativeUtils.loadLibraryFromJar("phosphophyllite:libs/libquartzpp." + librarySuffix);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Quartz++ failed to load, see IOException above for more details");
        }
    }
    
    private Thread secondaryThread;
    private long secondaryWindow;
    
    
    @Override
    public void GLSetup() {
        JNI.setupGL(GLFW.Functions.GetProcAddress);
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 6);
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        secondaryWindow = GLFW.glfwCreateWindow(1, 1, "quartzpp gl45 secondary thread", 0, Minecraft.getInstance().getMainWindow().getHandle());
        secondaryThread = new Thread(() -> {
            GLFW.glfwMakeContextCurrent(secondaryWindow);
            JNI.captureSecondaryThread();
            GLFW.glfwMakeContextCurrent(0);
        });
        secondaryThread.start();
    }
    
    @Override
    public void draw() {
        if (!secondaryThread.isAlive()) {
            throw new RuntimeException("Secondary thread died");
        }
        ActiveRenderInfo renderInfo = Minecraft.getInstance().gameRenderer.getActiveRenderInfo();
        Vector3d playerPosition = renderInfo.getProjectedView();
        JNI.drawGL(playerPosition.x, playerPosition.y, playerPosition.z, renderInfo.getYaw(), renderInfo.getPitch());
    }
    
    @Override
    public void GLShutdown() {
        JNI.shutdownGL();
        GLFW.glfwDestroyWindow(secondaryWindow);
    }
    
    
    ThreadLocal<ByteBuffer> buffer = new ThreadLocal<>();
    ThreadLocal<Integer> bufferSize = new ThreadLocal<>();
    
    @Override
    public void setBlockRenderInfo(ArrayList<BlockRenderInfo> info) {
        ArrayList<Byte> robnBuf = ROBN.toROBN(info);
        ByteBuffer byteBuf = buffer.get();
        if(byteBuf == null){
            bufferSize.set(robnBuf.size());
            buffer.set(BufferUtils.createByteBuffer(robnBuf.size()));
            byteBuf = buffer.get();
        }
        if(bufferSize.get() < robnBuf.size()){
            bufferSize.set(robnBuf.size());
            buffer.set(BufferUtils.createByteBuffer(robnBuf.size()));
            byteBuf = buffer.get();
        }
        byteBuf.rewind();
        for (Byte aByte : robnBuf) {
            byteBuf.put(aByte);
        }
        JNI.updateBlockRenderInfo((DirectBuffer) byteBuf);
    }
    
    @Override
    public Map<String, Integer> loadTextures(List<String> textures) {
        // being fair to me here, this *should* only be called once, sooo, yes it shits out a bunch of garbage, its *fine*
        // well, twice technically, *anyway*
        ArrayList<Byte> robnBuf = ROBN.toROBN(textures);
        ByteBuffer input = BufferUtils.createByteBuffer(robnBuf.size());
        // i just *happen* to know the side of the buffer ill be getting back
        // thx ROBN for being a defined size, oh wait, no, its not, but i know what it will be
        // *anyway*
        ByteBuffer output = BufferUtils.createByteBuffer((textures.size() * 4) + (11));
        for (Byte aByte : robnBuf) {
            input.put(aByte);
        }
        JNI.loadTextures((DirectBuffer) input, (DirectBuffer) output);
        output.rewind();
        robnBuf.clear();
        for (int i = 0; i < (textures.size() * 4) + (11); i++) {
            robnBuf.add(output.get());
        }
        List<Integer> ids = (List<Integer>) ROBN.fromROBN(robnBuf);
        Map<String, Integer> idMap = new HashMap<>();
        for (int i = 0; i < textures.size(); i++) {
            idMap.put(textures.get(i), ids.get(i));
        }
        return idMap;
    }
}
