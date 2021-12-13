package net.roguelogix.phosphophyllite.quartz.internal;

import net.roguelogix.phosphophyllite.util.MethodsReturnNonnullByDefault;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

/**
 * Generic GPU side buffer
 * use GL or VK specific implementations for more details
 * GL implementation is NOT thread safe, and may alter GL state
 * VK is unimplemented
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface Buffer {
    
    interface Allocation {
        
        /**
         * @apiNote byte buffer reflects CPU side of allocation, not GPU side, does not reflect GPU writes
         *          reading is allowed
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
         * marks entire allocation as dirty
         */
        void dirty();
        
        /**
         * marks range in allocation as dirty
         * writes are not visible until API specific flush is called
         */
        void dirtyRange(int offset, int size);
        
        /**
         * @return allocator used to allocate this allocation
         */
        Buffer allocator();
        
        /**
         * Copies buffer data internally
         * CPU side only, marks range dirty for next flush
         */
        void copy(int srcOffset, int dstOffset, int size);
        
        /**
         * Called when this allocation is reallocated, allocation fed to consumer is the new allocation
         * Callback is fed this allocation immediately at add when added
         *
         * WARNING: these callbacks must only weakly refer to this allocation object, else you will cause a memory leak
         *
         * @param consumer: callback
         */
        void addReallocCallback(Consumer<Allocation> consumer);
        
        /**
         * Called when this allocation's ByteBuffer is re-sliced, ByteBuffer fed to consumer is new ByteBuffer
         * Callback is fed current ByteBuffer immediately at add when added
         *
         * WARNING: these callbacks must only weakly refer to this allocation object, else you will cause a memory leak
         *
         * @param consumer: callback
         */
        void addBufferSliceCallback(Consumer<Allocation> consumer);
        
        void lock();
        
        void unlock();
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
    
    void dirtyAll();

    /**
     * Called when the CPU side buffer is changed
     *
     * WARNING: these callbacks must only weakly refer to this buffer object, else you will cause a memory leak
     *
     * @param consumer: callback
     */
    void addCPUReallocCallback(Consumer<Buffer> consumer);
    
    /**
     * Called when a new GL buffer is allocated, changing the result of calling handle()
     *
     *      * WARNING: these callbacks must only weakly refer to this buffer object, else you will cause a memory leak
     *
     * @param consumer: callback
     */
    void addGPUReallocCallback(Consumer<Buffer> consumer);
    
    default <T extends Buffer> T as(Class<T> ignored){
        //noinspection unchecked
        return (T)this;
    }
}

