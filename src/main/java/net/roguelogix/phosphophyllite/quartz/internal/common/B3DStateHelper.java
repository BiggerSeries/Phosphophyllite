package net.roguelogix.phosphophyllite.quartz.internal.common;

import com.mojang.blaze3d.vertex.BufferUploader;

import static org.lwjgl.opengl.GL33C.*;

/**
 * Blaze3d caches some state, sometimes i override this state, so, need to make sure it will set it back after i modify it
 */
public class B3DStateHelper {
    public static void bindArrayBuffer(int buffer) {
        glBindBuffer(GL_ARRAY_BUFFER, buffer);
        BufferUploader.lastVertexBufferObject = 0;
    }
    
    public static void bindElementBuffer(int buffer) {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffer);
        BufferUploader.lastIndexBufferObject = 0;
    }
    
    public static void bindVertexArray(int vertexArray) {
        glBindVertexArray(vertexArray);
        BufferUploader.lastVertexArrayObject = 0;
        BufferUploader.lastFormat = null;
    }
}
