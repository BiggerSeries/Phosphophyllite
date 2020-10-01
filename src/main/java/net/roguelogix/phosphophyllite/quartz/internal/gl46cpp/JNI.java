package net.roguelogix.phosphophyllite.quartz.internal.gl46cpp;

import sun.nio.ch.DirectBuffer;

public class JNI {
    
    public static native void setupGL(long glfwGetProcAddressAddress);
    
    // because GLFW doesnt work from C++, thx JNI linker bullshit, it *is* there, just statically linked in ffs
    // and im not about to go using a second GLFW, besides, i can use it from Java for the few things i need to
    public static native void captureSecondaryThread();
    
    public static native void drawGL(double x, double y, double z, double yaw, double pitch);
    
    public static native void shutdownGL();
    
    public static native void updateBlockRenderInfo(DirectBuffer dataBuff);
    
    public static native int loadTexture(String textureLocation);
    
    public static native void reloadShaders();
}
