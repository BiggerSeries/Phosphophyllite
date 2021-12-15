package net.roguelogix.phosphophyllite.quartz.internal.gl;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.roguelogix.phosphophyllite.quartz.internal.common.B3DStateHelper;
import net.roguelogix.phosphophyllite.quartz.internal.Buffer;
import net.roguelogix.phosphophyllite.quartz.internal.QuartzCore;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libc.LibCString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.system.MemoryUtil.memAddress;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GLBuffer implements Buffer {
    public class Allocation implements Buffer.Allocation {
        private record Info(int offset, int size) implements Comparable<Info> {
            @Override
            public int compareTo(@Nonnull Info info) {
                return Integer.compare(offset, info.offset);
            }
            
            private Pair<Info, Info> split(int size) {
                if (size > this.size) {
                    throw new IllegalArgumentException("Cannot split allocation to larger size");
                }
                if (size == this.size) {
                    return new Pair<>(new Info(this.offset, size), null);
                }
                return new Pair<>(new Info(this.offset, size), new Info(this.offset + size, this.size - size));
            }
        }
        
        private final Info info;
        private final ByteBuffer[] byteBuffer;
        private final ObjectArrayList<Consumer<Buffer.Allocation>> reallocCallbacks = new ObjectArrayList<>();
        private final ObjectArrayList<Consumer<Buffer.Allocation>> sliceCallbacks;
        private final Runnable slicer;
        
        protected Allocation(Info info) {
            this.info = info;
            var allocator = GLBuffer.this;
            final var byteBuffer = new ByteBuffer[1];
            final var sliceCallbacks = new ObjectArrayList<Consumer<Buffer.Allocation>>();
            final var weakRef = new WeakReference<>(this);
            Runnable slicer = () -> {
                byteBuffer[0] = allocator.byteBuffer[0].slice(info.offset, info.size);
                final var alloc = weakRef.get();
                if(alloc == null){
                    return;
                }
                for (int i = 0; i < sliceCallbacks.size(); i++) {
                    sliceCallbacks.get(i).accept(alloc);
                }
            };
            QuartzCore.CLEANER.register(this, () -> GLCore.deletionQueue.enqueue(() -> {
                allocator.free(info);
                allocator.slicers.remove(slicer);
            }));
            slicer.run();
            allocator.slicers.add(slicer);
            this.byteBuffer = byteBuffer;
            this.sliceCallbacks = sliceCallbacks;
            this.slicer = slicer;
        }
        
        public void delete() {
            free(info);
            slicers.remove(slicer);
        }
        
        public ByteBuffer buffer() {
            return byteBuffer[0].rewind();
        }
        
        public int offset() {
            return info.offset;
        }
        
        public int size() {
            return info.size;
        }
        
        public void dirty() {
            dirtyRange(0, info.size);
        }
        
        public void dirtyRange(int offset, int size) {
            GLBuffer.this.dirtyRange(info.offset + offset, info.offset + offset + size);
        }
        
        public GLBuffer allocator() {
            return GLBuffer.this;
        }
        
        public void copy(int srcOffset, int dstOffset, int size) {
            // is this safe? *no*
            // is this the fastest option? *no*
            // does LWJGL give me a better option, *no, not really*
            final long address = MemoryUtil.memAddress(byteBuffer[0]);
            final long srcAddress = address + srcOffset;
            final long dstAddress = address + dstOffset;
            LibCString.nmemmove(dstAddress, srcAddress, size);
            dirtyRange(dstOffset, size);
        }
        
        public void addReallocCallback(Consumer<Buffer.Allocation> consumer) {
            consumer.accept(this);
            reallocCallbacks.add(consumer);
        }
        
        public void addBufferSliceCallback(Consumer<Buffer.Allocation> consumer) {
            consumer.accept(this);
            sliceCallbacks.add(consumer);
        }
        
        @Override
        public void lock() {
        
        }
        
        @Override
        public void unlock() {
        
        }
        
        public int compareTo(Allocation other) {
            return Integer.compare(info.offset, other.info.offset);
        }
    }
    
    private final int buffer;
    private final int usage;
    private int size;
    private final ByteBuffer[] byteBuffer = new ByteBuffer[1];
    
    private final ObjectArrayList<Runnable> slicers = new ObjectArrayList<>();
    private final ObjectArrayList<Allocation.Info> liveAllocations = new ObjectArrayList<>();
    private final ObjectArrayList<Allocation.Info> freeAllocations = new ObjectArrayList<>() {
        @Override
        public boolean add(@Nullable Allocation.Info allocation) {
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
    
    private final ObjectArrayList<Consumer<Buffer>> reallocCallbacks = new ObjectArrayList<>();
    
    public GLBuffer(boolean dynamic, int initialSize) {
        if (initialSize <= 0) {
            throw new IllegalArgumentException("Initial buffer size must be greater than 0");
        }
        size = initialSize;
        usage = dynamic ? GL_DYNAMIC_DRAW : GL_STATIC_DRAW;
        buffer = glGenBuffers();
        B3DStateHelper.bindArrayBuffer(buffer);
        glBufferData(GL_ARRAY_BUFFER, initialSize, usage);
        byteBuffer[0] = MemoryUtil.memAlloc(initialSize);
        freeAllocations.add(new Allocation.Info(0, initialSize));
        var bufArray = byteBuffer;
        int buffer = this.buffer;
        QuartzCore.CLEANER.register(this, () -> {
            MemoryUtil.memFree(bufArray[0]);
            GLCore.deletionQueue.enqueue(() -> glDeleteBuffers(buffer));
        });
    }
    
    public GLBuffer(boolean dynamic) {
        this(dynamic, 1);
    }
    
    public int size() {
        return size;
    }
    
    public Allocation alloc(int size) {
        return alloc(size, 1);
    }
    
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
            return new Allocation(freeAlloc);
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
        return new Allocation(alloc);
    }
    
    public Allocation realloc(@Nullable Allocation allocation, int newSize) {
        return realloc(allocation, newSize, 1);
    }
    
    @Override
    public Buffer.Allocation realloc(@Nullable Buffer.Allocation bufAlloc, int newSize, int alignment) {
        if (!(bufAlloc instanceof Allocation alloc)) {
            throw new IllegalArgumentException("Cannot realloc allocation from another buffer");
        }
        return realloc(alloc, newSize, alignment);
    }
    
    public Allocation realloc(@Nullable Allocation allocation, int newSize, int alignment) {
        if (allocation == null) {
            return alloc(newSize, alignment);
        }
        if (allocation.allocator() != this) {
            // not an allocation from this buffer
            throw new IllegalArgumentException("Cannot realloc allocation from another buffer");
        }
        final int alignmentBitmask = alignment - 1;
        
        var liveIndex = liveAllocations.indexOf(allocation.info);
        if (liveIndex == -1) {
            throw new IllegalArgumentException("Cannot realloc non-live allocation");
        }
        if (newSize <= allocation.info.size && (allocation.info.offset & alignmentBitmask) == 0) {
            // this allocation already meets size and alignment requirements
            if (newSize == allocation.info.size) {
                return allocation;
            }
            var removed = liveAllocations.pop();
            if (liveIndex != liveAllocations.size()) {
                liveAllocations.set(liveIndex, removed);
            }
            var newAllocInfos = allocation.info.split(newSize);
            var newAllocInfo = newAllocInfos.getFirst();
            var freeAllocInfo = newAllocInfos.getSecond();
            var newAlloc = new Allocation(newAllocInfo);
            freeAllocations.add(freeAllocInfo);
            liveAllocations.add(newAllocInfo);
            long dstAddress = memAddress(newAlloc.byteBuffer[0]);
            long srcAddress = memAddress(allocation.byteBuffer[0]);
            int size = Math.min(newAlloc.byteBuffer[0].remaining(), allocation.byteBuffer[0].remaining());
            if (srcAddress != dstAddress) {
                LibCString.nmemmove(dstAddress, srcAddress, size);
                newAlloc.dirty();
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
        Allocation.Info precedingAlloc = null;
        Allocation.Info followingAlloc = null;
        for (int i = 0; i < freeAllocations.size(); i++) {
            var freeAllocation = freeAllocations.get(i);
            if (freeAllocation.offset + freeAllocation.size == allocation.info.offset) {
                precedingAlloc = freeAllocation;
                continue;
            }
            if (freeAllocation.offset == allocation.info.offset + allocation.info.size) {
                followingAlloc = freeAllocation;
                break;
            }
            if (freeAllocation.offset > allocation.info.offset) {
                break;
            }
        }
        int fullBlockOffset = precedingAlloc == null ? allocation.info.offset : precedingAlloc.offset;
        int alignmentWaste = fullBlockOffset & alignmentBitmask;
        int fullBlockSize = allocation.info.size;
        if (precedingAlloc != null) {
            fullBlockSize += precedingAlloc.size;
        }
        if (followingAlloc != null) {
            fullBlockSize += followingAlloc.size;
        } else if (allocation.info.offset + allocation.info.size == size) {
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
            var newAllocInfo = new Allocation.Info(fullBlockOffset, fullBlockSize);
            if (alignmentWaste > 0) {
                final var newAllocs = newAllocInfo.split(alignmentWaste);
                freeAllocations.add(newAllocs.getFirst());
                newAllocInfo = newAllocs.getSecond();
            }
            if (newAllocInfo.size > newSize) {
                final var newAllocs = newAllocInfo.split(newSize);
                newAllocInfo = newAllocs.getFirst();
                freeAllocations.add(newAllocs.getSecond());
            }
            var newAlloc = new Allocation(newAllocInfo);
            var removed = liveAllocations.pop();
            if (liveIndex != liveAllocations.size()) {
                liveAllocations.set(liveIndex, removed);
            }
            liveAllocations.add(newAllocInfo);
            long dstAddress = memAddress(newAlloc.byteBuffer[0]);
            long srcAddress = memAddress(allocation.byteBuffer[0]);
            int size = Math.min(newAlloc.byteBuffer[0].remaining(), allocation.byteBuffer[0].remaining());
            if (srcAddress != dstAddress) {
                LibCString.nmemmove(dstAddress, srcAddress, size);
                newAlloc.dirty();
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
        long dstAddress = memAddress(newAlloc.byteBuffer[0]);
        long srcAddress = memAddress(allocation.byteBuffer[0]);
        int size = Math.min(newAlloc.byteBuffer[0].remaining(), allocation.byteBuffer[0].remaining());
        LibCString.nmemcpy(dstAddress, srcAddress, size);
        newAlloc.dirty();
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
    public void addGPUReallocCallback(Consumer<Buffer> consumer) {
        // GL doesnt realloc on the GPU
    }
    
    @Override
    public void free(Buffer.Allocation allocation) {
        if (allocation instanceof Allocation alloc) {
            free(alloc);
        }
    }
    
    public void free(Allocation allocation) {
        free(allocation.info);
    }
    
    public void free(Allocation.Info allocation) {
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
    
    private int minDirty = Integer.MAX_VALUE;
    private int maxDirty = 0;
    
    @Override
    public void dirtyAll() {
        minDirty = 0;
        maxDirty = size;
    }
    
    public void dirtyRange(int min, int max) {
        minDirty = Math.min(min, minDirty);
        maxDirty = Math.max(max, maxDirty);
    }
    
    public void flush() {
        if (minDirty >= maxDirty) {
            return;
        }
        // TODO: maybe smaller blocks?
        B3DStateHelper.bindArrayBuffer(buffer);
        nglBufferSubData(GL_ARRAY_BUFFER, minDirty, maxDirty - minDirty, memAddress(byteBuffer[0]) + minDirty);
        minDirty = Integer.MAX_VALUE;
        maxDirty = 0;
    }
    
    public void addCPUReallocCallback(Consumer<Buffer> consumer) {
        reallocCallbacks.add(consumer);
    }
    
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
        
        byteBuffer[0] = MemoryUtil.memRealloc(byteBuffer[0], size);
        B3DStateHelper.bindArrayBuffer(buffer);
        glBufferData(GL_ARRAY_BUFFER, byteBuffer[0], usage);
        
        freeAllocations.add(new Allocation.Info(oldSize, size - oldSize));
        
        collapseFreeAllocationWithNext(freeAllocations.size() - 2);
        
        slicers.forEach(Runnable::run);
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
            freeAllocations.add(new Allocation.Info(allocA.offset, allocA.size + allocB.size));
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