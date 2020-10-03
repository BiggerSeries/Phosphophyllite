package net.roguelogix.phosphophyllite.quartz.internal.gl46cpp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.util.math.vector.Vector3d;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.quartz.internal.Renderer;
import net.roguelogix.phosphophyllite.quartz.internal.BlockRenderInfo;
import net.roguelogix.phosphophyllite.robn.ROBN;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import sun.nio.ch.DirectBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * its almost like the rest of this is in C++
 * oh wait, it is
 * go to src/main/quartzpp/src/gl45 for the rest here
 */
public class RendererGL46CPP extends Renderer {
    public RendererGL46CPP() {
        try {
            Phosphophyllite.LOGGER.warn("Loading Quartz++, if there is a JVM crash immediately following this, it was probably Quartz++ (module of the Phosphophyllite library)");
            NativeUtils.loadLibraryFromJar("phosphophyllite:libs/libroguelib_threading.so");
            NativeUtils.loadLibraryFromJar("phosphophyllite:libs/libroguelib_exceptions.so");
            NativeUtils.loadLibraryFromJar("phosphophyllite:libs/libroguelib_logging.so");
            NativeUtils.loadLibraryFromJar("phosphophyllite:libs/libquartzpp.so");
        } catch (IOException e) {
            e.printStackTrace();
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
        if(!secondaryThread.isAlive()){
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
    
    @Override
    public void setBlockRenderInfo(ArrayList<BlockRenderInfo> info) {
        ArrayList<Byte> robnBuf = ROBN.toROBN(info);
        ByteBuffer buffer = BufferUtils.createByteBuffer(robnBuf.size());
        for (Byte aByte : robnBuf) {
            buffer.put(aByte);
        }
        JNI.updateBlockRenderInfo((DirectBuffer) buffer);
    }
    
    @Override
    public int loadTexture(String textureLocation) {
        return JNI.loadTexture(textureLocation);
    }
}
