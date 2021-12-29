package net.roguelogix.phosphophyllite.quartz.internal.common;

import com.mojang.blaze3d.vertex.BufferUploader;
import net.roguelogix.phosphophyllite.util.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Field;

import static org.lwjgl.opengl.GL33C.*;

/**
 * Blaze3d caches some state, sometimes i override this state, so, need to make sure it will set it back after i modify it
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class B3DStateHelper {
    
    private static final Field lastVertexBufferObjectField;
    private static final Field lastIndexBufferObjectField;
    private static final Field lastVertexArrayObjectField;
    private static final Field lastFormatField;
    
    static {
        Field lastVertexBufferObjectField1;
        Field lastIndexBufferObjectField1;
        Field lastVertexArrayObjectField1;
        Field lastFormatField1;
        try {
            lastVertexBufferObjectField1 = BufferUploader.class.getDeclaredField("lastVertexBufferObject");
            lastIndexBufferObjectField1 = BufferUploader.class.getDeclaredField("lastIndexBufferObject");
            lastVertexArrayObjectField1 = BufferUploader.class.getDeclaredField("lastVertexArrayObject");
            lastFormatField1 = BufferUploader.class.getDeclaredField("lastFormat");
        } catch (NoSuchFieldException e) {
            lastVertexBufferObjectField1 = null;
            lastIndexBufferObjectField1 = null;
            lastVertexArrayObjectField1 = null;
            lastFormatField1 = null;
            e.printStackTrace();
        }
        lastVertexBufferObjectField = lastVertexBufferObjectField1;
        lastIndexBufferObjectField = lastIndexBufferObjectField1;
        lastVertexArrayObjectField = lastVertexArrayObjectField1;
        lastFormatField = lastFormatField1;
    }
    
    public static void bindArrayBuffer(int buffer) {
        glBindBuffer(GL_ARRAY_BUFFER, buffer);
        try {
            BufferUploader.lastVertexBufferObject = 0;
        } catch (IllegalAccessError e) {
            try {
                lastVertexBufferObjectField.set(null, 0);
            } catch (IllegalAccessException ignored) {
            }
        }
    }
    
    public static void bindElementBuffer(int buffer) {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffer);
        try {
            BufferUploader.lastIndexBufferObject = 0;
        } catch (IllegalAccessError e) {
            try {
                lastIndexBufferObjectField.set(null, 0);
            } catch (IllegalAccessException ignored) {
            }
        }
    }
    
    public static void bindVertexArray(int vertexArray) {
        glBindVertexArray(vertexArray);
        try {
            BufferUploader.lastVertexArrayObject = 0;
            BufferUploader.lastFormat = null;
        } catch (IllegalAccessError e) {
            try {
                lastVertexArrayObjectField.set(null, 0);
                lastFormatField.set(null, null);
            } catch (IllegalAccessException ignored) {
            }
        }
    }
}
