package net.roguelogix.phosphophyllite.quartz.internal.common;

import net.roguelogix.phosphophyllite.util.MethodsReturnNonnullByDefault;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface GLBuffer extends GLObject {
    
    interface Allocation extends GLDeletable {
        
        /**
         * @apiNote bytebuffer is write only, reading is undefined
         *
         * @return sliced bytebuffer for this allocation only
         */
        ByteBuffer buffer();
        
        /**
         * @return offset (in bytes) into OpenGL buffer
         */
        int offset();
        
        /**
         * @return allocation size (in bytes)
         */
        int size();
        
        /**
         * flush the buffer section to OpenGL
         * May change OpenGL state, potentially writes to GL_ARRAY_BUFFER
         * writes not visible to GL until this is called
         */
        void flush();
        
        /**
         * flush the buffer section to OpenGL
         * May change OpenGL state, potentially writes to GL_ARRAY_BUFFER
         * writes not visible to GL until this is called
         */
        void flushRange(int offset, int size);
        
        /**
         * @return allocator used to allocate this allocation
         */
        GLBuffer allocator();
        
        /**
         * Copies buffer data internally
         * May change OpenGL state, potentially writes to GL_ARRAY_BUFFER
         */
        void copy(int srcOffset, int dstOffset, int size);
        
        /**
         * Called when this allocation is reallocated, allocation fed to consumer is the new allocation
         * Callback is fed this allocation immediately at add when added
         * @param consumer: callback
         */
        void addReallocCallback(Consumer<Allocation> consumer);
        
        /**
         * Called when this allocation's ByteBuffer is re-sliced, ByteBuffer fed to consumer is new ByteBuffer
         * Callback is fed current ByteBuffer immediately at add when added
         * @param consumer: callback
         */
        void addBufferSliceCallback(Consumer<GLBuffer.Allocation> consumer);
    }
    
    /**
     * Size of the entire buffer, allocations are not considered
     * @return size of OpenGL buffer
     */
    int size();
    
    /**
     * Allocates a block of the buffer of at least the size specified
     * Will not change OpenGL state
     */
    default Allocation alloc(int size) {
        return alloc(size, 1);
    }
    
    /**
     * Allocates a block of the buffer of at least the size specified with specified minimum alignment
     * Will not change OpenGL state
     * Alignment must be a power of 2
     */
    Allocation alloc(int size, int alignment);
    
    /**
     * Resizes an allocation to a new size
     * May change OpenGL state, potentially writes to GL_ARRAY_BUFFER
     * May return a different allocation object than what was passed in
     * NOTE: may have different alignment than current allocation
     * If allocation is null, creates a new allocation
     */
    default Allocation realloc(@Nullable Allocation allocation, int newSize) {
        return realloc(allocation, newSize, 1);
    }
    
    /**
     * Resizes an allocation to a new size with the specified alignment
     * May change OpenGL state, potentially writes to GL_ARRAY_BUFFER
     * May return a different allocation object than what was passed in
     * Alignment may differ from the current allocation alignment
     * If allocation is null, creates a new allocation
     */
    Allocation realloc(@Nullable Allocation allocation, int newSize, int alignment);
    
    void free(Allocation allocation);
    
    void flushAll();
    
    /**
     * Creates a fence for sync
     * returns DUMMY_FENCE if syncing is not required
     * @return created fence
     */
    GLFence createFence();
    
    /**
     * Called when the CPU side buffer is changed
     * @param consumer: callback
     */
    void addCPUReallocCallback(Consumer<GLBuffer> consumer);
    
    /**
     * Called when a new GL buffer is allocated, changing the result of calling handle()
     * @param consumer: callback
     */
    void addGPUReallocCallback(Consumer<GLBuffer> consumer);
}

