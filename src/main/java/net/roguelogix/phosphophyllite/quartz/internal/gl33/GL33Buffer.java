package net.roguelogix.phosphophyllite.quartz.internal.gl33;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.roguelogix.phosphophyllite.quartz.internal.common.B3DStateHelper;
import net.roguelogix.phosphophyllite.quartz.internal.common.GLBuffer;
import net.roguelogix.phosphophyllite.quartz.internal.common.GLFence;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libc.LibCString;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.system.MemoryUtil.memAddress;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GL33Buffer implements GLBuffer {
    public class Allocation implements GLBuffer.Allocation, Comparable<Allocation> {
        private final int offset;
        private final int size;
        private ByteBuffer byteBuffer;
        private final ObjectArrayList<Consumer<GLBuffer.Allocation>> reallocCallbacks = new ObjectArrayList<>();
        private final ObjectArrayList<Consumer<GLBuffer.Allocation>> sliceCallbacks = new ObjectArrayList<>();
        
        protected Allocation(int offset, int size) {
            this.offset = offset;
            this.size = size;
            sliceByteBuffer();
        }
        
        @Override
        public void delete() {
            free(this);
        }
        
        @Override
        public ByteBuffer buffer() {
            return byteBuffer.rewind();
        }
        
        @Override
        public int offset() {
            return offset;
        }
        
        @Override
        public int size() {
            return size;
        }
        
        @Override
        public void flush() {
            flushRange(0, byteBuffer.limit());
        }
        
        @Override
        public void flushRange(int offset, int size) {
            byteBuffer.rewind();
            B3DStateHelper.bindArrayBuffer(buffer);
            // generally shouldn't use the ngl calls, but this works better here because I can specify size separately
            nglBufferSubData(GL_ARRAY_BUFFER, offset + this.offset, size, memAddress(byteBuffer) + offset);
        }
        
        @Override
        public GLBuffer allocator() {
            return GL33Buffer.this;
        }
        
        @Override
        public void copy(int srcOffset, int dstOffset, int size) {
            // is this safe? *no*
            // is this the fastest option? *no*
            // does LWJGL give me a better option, *no, not really*
            final long address = MemoryUtil.memAddress(byteBuffer);
            final long srcAddress = address + srcOffset;
            final long dstAddress = address + dstOffset;
            LibCString.nmemmove(dstAddress, srcAddress, size);
            flushRange(dstOffset, size);
//            if(
//                    (srcAddress < dstAddress && srcAddress + size  > dstOffset) ||
//                    (dstAddress < srcAddress && dstAddress + size  > srcAddress)
//            ){
//                LibCString.nmemmove(dstAddress, srcAddress, size);
//                flushRange(dstOffset, size);
//            } else {
//                LibCString.nmemcpy(dstAddress, srcAddress, size);
//                B3DStateHelper.bindArrayBuffer(buffer);
//                glCopyBufferSubData(GL_ARRAY_BUFFER, GL_ARRAY_BUFFER, srcOffset, dstOffset, size);
//            }
        }
        
        @Override
        public void addReallocCallback(Consumer<GLBuffer.Allocation> consumer) {
            consumer.accept(this);
            reallocCallbacks.add(consumer);
        }
    
        @Override
        public void addBufferSliceCallback(Consumer<GLBuffer.Allocation> consumer) {
            consumer.accept(this);
            sliceCallbacks.add(consumer);
        }
    
        @Override
        public int compareTo(Allocation other) {
            return Integer.compare(offset, other.offset);
        }
        
        private void sliceByteBuffer() {
            byteBuffer = GL33Buffer.this.byteBuffer.slice(offset, size);
            for (int i = 0; i < this.sliceCallbacks.size(); i++) {
                sliceCallbacks.get(i).accept(this);
            }
        }
        
        private Pair<Allocation, Allocation> split(int size) {
            if (size > this.size) {
                throw new IllegalArgumentException("Cannot split allocation to larger size");
            }
            if (size == this.size) {
                return new Pair<>(new Allocation(offset, size), null);
            }
            return new Pair<>(new Allocation(offset, size), new Allocation(offset + size, this.size - size));
        }
    }
    
    private final int buffer;
    private final int usage;
    private int size;
    private ByteBuffer byteBuffer;
    
    final ObjectArrayList<GL33Buffer.Allocation> liveAllocations = new ObjectArrayList<>();
    final ObjectArrayList<GL33Buffer.Allocation> freeAllocations = new ObjectArrayList<>() {
        @Override
        public boolean add(@Nullable GL33Buffer.Allocation allocation) {
            if (allocation == null) {
                return false;
            }
            int index = Collections.binarySearch(this, allocation);
            if (index < 0) {
                index = ~index;
                super.add(index, allocation);
            } else {
                super.set(index, allocation);
            }
            return true;
        }
    };
    
    final ObjectArrayList<Consumer<GLBuffer>> reallocCallbacks = new ObjectArrayList<>();
    
    public GL33Buffer(boolean dynamic, int initialSize) {
        if (initialSize <= 0) {
            throw new IllegalArgumentException("Initial buffer size must be greater than 0");
        }
        size = initialSize;
        usage = dynamic ? GL_DYNAMIC_DRAW : GL_STATIC_DRAW;
        buffer = glGenBuffers();
        B3DStateHelper.bindArrayBuffer(buffer);
        glBufferData(GL_ARRAY_BUFFER, initialSize, usage);
                byteBuffer = MemoryUtil.memAlloc(initialSize);
        freeAllocations.add(new GL33Buffer.Allocation(0, initialSize));
    }
    
    public GL33Buffer(boolean dynamic) {
        this(dynamic, 1);
    }
    
    public int size() {
        return size;
    }
    
    @Override
    public Allocation alloc(final int size, final int alignment) {
        final int alignmentBitmask = alignment - 1;
        collapseFreeAllocations();
        for (int i = 0; i < freeAllocations.size(); i++) {
            var freeAlloc = freeAllocations.get(i);
            // bit trickery that works because I require alignment to be a power of 2
            final int alignmentWaste = freeAlloc.offset & alignmentBitmask;
            if (freeAlloc.size - alignmentWaste < size) {
                // wont fit, *neeeeeeeeeeeext*
                continue;
            }
            freeAllocations.remove(i);
            if (alignmentWaste > 0) {
                final var newAllocs = freeAlloc.split(alignmentWaste);
                // not concurrent modification because this will always return
                freeAllocations.add(newAllocs.getFirst());
                freeAlloc = newAllocs.getSecond();
            }
            if (freeAlloc.size > size) {
                final var newAllocs = freeAlloc.split(size);
                // not concurrent modification because this will always return
                freeAlloc = newAllocs.getFirst();
                freeAllocations.add(newAllocs.getSecond());
            }
            
            liveAllocations.add(freeAlloc);
            return freeAlloc;
        }
        
        int endOffset = this.size;
        int minSize = this.size + size;
        if (!freeAllocations.isEmpty()) {
            var endAlloc = freeAllocations.get(freeAllocations.size() - 1);
            if (endAlloc.offset + endAlloc.size == this.size) {
                minSize -= endAlloc.size;
                endOffset = endAlloc.offset;
            }
        }
        int alignmentWaste = endOffset & alignmentBitmask;
        minSize += alignmentWaste;
        
        expand(minSize);
        
        var alloc = freeAllocations.pop();
        if (alignmentWaste > 0) {
            final var newAllocs = alloc.split(alignmentWaste);
            freeAllocations.add(newAllocs.getFirst());
            alloc = newAllocs.getSecond();
        }
        if (alloc.size > size) {
            final var newAllocs = alloc.split(size);
            alloc = newAllocs.getFirst();
            freeAllocations.add(newAllocs.getSecond());
        }
        liveAllocations.add(alloc);
        return alloc;
    }
    
    @Override
    public GLBuffer.Allocation realloc(@Nullable GLBuffer.Allocation glAllocation, int newSize, int alignment) {
        if (glAllocation == null) {
            return alloc(newSize, alignment);
        }
        if (!(glAllocation instanceof GL33Buffer.Allocation allocation) || glAllocation.allocator() != this) {
            // not an allocation from this buffer
            throw new IllegalArgumentException("Cannot realloc allocation from another buffer");
        }
        final int alignmentBitmask = alignment - 1;
        
        var liveIndex = liveAllocations.indexOf(allocation);
        if (liveIndex == -1) {
            throw new IllegalArgumentException("Cannot realloc non-live allocation");
        }
        if (newSize <= allocation.size && (allocation.offset & alignmentBitmask) == 0) {
            // this allocation already meets size and alignment requirements
            if (newSize == allocation.size) {
                return allocation;
            }
            var newAllocs = allocation.split(newSize);
            var newAlloc = newAllocs.getFirst();
            var freeAlloc = newAllocs.getSecond();
            freeAllocations.add(freeAlloc);
            var removed = liveAllocations.pop();
            if (liveIndex != liveAllocations.size()) {
                liveAllocations.set(liveIndex, removed);
            }
            liveAllocations.add(newAlloc);
            long dstAddress = memAddress(newAlloc.byteBuffer);
            long srcAddress = memAddress(allocation.byteBuffer);
            int size = Math.min(newAlloc.byteBuffer.remaining(), allocation.byteBuffer.remaining());
            if (srcAddress != dstAddress) {
                LibCString.nmemmove(dstAddress, srcAddress, size);
                newAlloc.flush();
            }
            for (int i = 0; i < allocation.reallocCallbacks.size(); i++) {
                newAlloc.addReallocCallback(allocation.reallocCallbacks.get(i));
            }
            for (int i = 0; i < allocation.sliceCallbacks.size(); i++) {
                newAlloc.addBufferSliceCallback(allocation.sliceCallbacks.get(i));
            }
            return newAlloc;
        }
        
        collapseFreeAllocations();
        Allocation precedingAlloc = null;
        Allocation followingAlloc = null;
        for (int i = 0; i < freeAllocations.size(); i++) {
            var freeAllocation = freeAllocations.get(i);
            if (freeAllocation.offset + freeAllocation.size == allocation.offset) {
                precedingAlloc = freeAllocation;
                continue;
            }
            if (freeAllocation.offset == allocation.offset + allocation.size) {
                followingAlloc = freeAllocation;
                break;
            }
            if (freeAllocation.offset > allocation.offset) {
                break;
            }
        }
        int fullBlockOffset = precedingAlloc == null ? allocation.offset : precedingAlloc.offset;
        int alignmentWaste = fullBlockOffset & alignmentBitmask;
        int fullBlockSize = allocation.size;
        if (precedingAlloc != null) {
            fullBlockSize += precedingAlloc.size;
        }
        if (followingAlloc != null) {
            fullBlockSize += followingAlloc.size;
        } else if (allocation.offset + allocation.size == size) {
            // end allocation, so I can resize it to whatever is needed
            freeAllocations.remove(precedingAlloc);
            int minSize = fullBlockOffset + alignmentWaste + newSize;
            expand(minSize);
            followingAlloc = freeAllocations.get(freeAllocations.size() - 1);
            fullBlockSize += followingAlloc.size;
        }
        
        if (fullBlockSize - alignmentWaste >= newSize) {
            // ok, available memory exists around where the data currently is
            freeAllocations.remove(precedingAlloc);
            freeAllocations.remove(followingAlloc);
            var newAlloc = new Allocation(fullBlockOffset, fullBlockSize);
            if (alignmentWaste > 0) {
                final var newAllocs = newAlloc.split(alignmentWaste);
                freeAllocations.add(newAllocs.getFirst());
                newAlloc = newAllocs.getSecond();
            }
            if (newAlloc.size > newSize) {
                final var newAllocs = newAlloc.split(newSize);
                newAlloc = newAllocs.getFirst();
                freeAllocations.add(newAllocs.getSecond());
            }
            var removed = liveAllocations.pop();
            if (liveIndex != liveAllocations.size()) {
                liveAllocations.set(liveIndex, removed);
            }
            liveAllocations.add(newAlloc);
            long dstAddress = memAddress(newAlloc.byteBuffer);
            long srcAddress = memAddress(allocation.byteBuffer);
            int size = Math.min(newAlloc.byteBuffer.remaining(), allocation.byteBuffer.remaining());
            if (srcAddress != dstAddress) {
                LibCString.nmemmove(dstAddress, srcAddress, size);
                newAlloc.flush();
            }
            for (int i = 0; i < allocation.reallocCallbacks.size(); i++) {
                newAlloc.addReallocCallback(allocation.reallocCallbacks.get(i));
            }
            for (int i = 0; i < allocation.sliceCallbacks.size(); i++) {
                newAlloc.addBufferSliceCallback(allocation.sliceCallbacks.get(i));
            }
            return newAlloc;
        }
        
        // SOL, allocate a new block and use that
        var newAlloc = alloc(newSize, alignment);
        long dstAddress = memAddress(newAlloc.byteBuffer);
        long srcAddress = memAddress(allocation.byteBuffer);
        int size = Math.min(newAlloc.byteBuffer.remaining(), allocation.byteBuffer.remaining());
        LibCString.nmemcpy(dstAddress, srcAddress, size);
        newAlloc.flush();
        free(allocation);
        for (int i = 0; i < allocation.reallocCallbacks.size(); i++) {
            newAlloc.addReallocCallback(allocation.reallocCallbacks.get(i));
        }
        for (int i = 0; i < allocation.sliceCallbacks.size(); i++) {
            newAlloc.addBufferSliceCallback(allocation.sliceCallbacks.get(i));
        }
        return newAlloc;
    }
    
    @Override
    public void free(GLBuffer.Allocation glAllocation) {
        if (glAllocation instanceof GL33Buffer.Allocation allocation) {
            var index = liveAllocations.indexOf(allocation);
            if (index == -1) {
                return;
            }
            var removed = liveAllocations.pop();
            if (index != liveAllocations.size()) {
                liveAllocations.set(index, removed);
            }
            freeAllocations.add(allocation);
            index = freeAllocations.indexOf(allocation);
            collapseFreeAllocationWithNext(index - 1);
            collapseFreeAllocationWithNext(index);
        }
    }
    
    @Override
    public void flushAll() {
        B3DStateHelper.bindArrayBuffer(buffer);
        glBufferSubData(GL_ARRAY_BUFFER, 0, byteBuffer);
            }
    
    @Override
    public GLFence createFence() {
        return GLFence.DUMMY_FENCE;
    }
    
    @Override
    public void addCPUReallocCallback(Consumer<GLBuffer> consumer) {
        reallocCallbacks.add(consumer);
    }
    
    @Override
    public void addGPUReallocCallback(Consumer<GLBuffer> consumer) {
        // GL buffer handle never changes with GL33
    }
    
    @Override
    public void delete() {
        MemoryUtil.memFree(byteBuffer);
        glDeleteBuffers(buffer);
    }
    
    @Override
    public int handle() {
        return buffer;
    }
    
    private void expand(int minSize) {
        if (size >= minSize) {
            return;
        }
        
        int oldSize = size;
        size = Integer.highestOneBit(minSize);
        if (size < minSize) {
            size <<= 1;
        }
        
        byteBuffer = MemoryUtil.memRealloc(byteBuffer, size);
        B3DStateHelper.bindArrayBuffer(buffer);
        glBufferData(GL_ARRAY_BUFFER, byteBuffer, usage);
                
        freeAllocations.add(new GL33Buffer.Allocation(oldSize, size - oldSize));
        
        collapseFreeAllocationWithNext(freeAllocations.size() - 2);
        
        liveAllocations.forEach(GL33Buffer.Allocation::sliceByteBuffer);
        freeAllocations.forEach(GL33Buffer.Allocation::sliceByteBuffer);
        reallocCallbacks.forEach(c -> c.accept(this));
    }
    
    private boolean collapseFreeAllocationWithNext(int freeAllocationIndex) {
        if (freeAllocationIndex < 0 || freeAllocationIndex >= freeAllocations.size() - 1) {
            return false;
        }
        var allocA = freeAllocations.get(freeAllocationIndex);
        var allocB = freeAllocations.get(freeAllocationIndex + 1);
        if (allocA.offset + allocA.size == allocB.offset) {
            // neighboring allocations, collapse them
            freeAllocations.remove(freeAllocationIndex + 1);
            freeAllocations.remove(freeAllocationIndex);
            freeAllocations.add(new Allocation(allocA.offset, allocA.size + allocB.size));
            return true;
        }
        return false;
    }
    
    private void collapseFreeAllocations() {
        for (int i = 0; i < freeAllocations.size(); i++) {
            if (collapseFreeAllocationWithNext(i)) {
                i--;
            }
        }
    }
    
}
