package net.roguelogix.phosphophyllite.quartz.internal.common;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.BlockAndTintGetter;
import net.roguelogix.phosphophyllite.quartz.Quartz;
import net.roguelogix.phosphophyllite.quartz.QuartzDynamicLight;
import net.roguelogix.phosphophyllite.quartz.internal.QuartzCore;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class DynamicLightManager implements GLDeletable {
    
    public class DynamicLight implements QuartzDynamicLight {
        
        final GLBuffer.Allocation allocation;
        ByteBuffer buffer;
        Consumer<BlockAndTintGetter> updateCall;
        
        private DynamicLight(GLBuffer.Allocation alloc) {
            this.allocation = alloc;
            alloc.addBufferSliceCallback(newAlloc -> this.buffer = newAlloc.buffer());
        }
        
        @Override
        public void write(int vertex, int vertexDirection, byte skyLight, byte blockLight, byte AO) {
            blockLight &= 63;
            skyLight &= 63;
            AO &= 3;
            blockLight |= AO << 6;
            vertex &= 7;
            vertexDirection += 2;
            vertexDirection &= 0xFF;
            vertexDirection -= 2;
            buffer.put(vertex * 12 + vertexDirection * 2, blockLight);
            buffer.put(vertex * 12 + vertexDirection * 2 + 1, skyLight);
        }
        
        @Override
        public void dispose() {
            allocation.delete();
            var removed = updateCalls.pop();
            if (removed != updateCall) {
                int index = updateCalls.indexOf(updateCall);
                if (index != -1) {
                    updateCalls.set(index, removed);
                } else {
                    updateCalls.add(removed);
                }
            }
        }
        
        public int id() {
            return allocation.offset() / MagicNumbers.DYNAMIC_LIGHT_BYTE_SIZE;
        }
    }
    
    private final GLBuffer glBuffer;
    private final ObjectArrayList<Consumer<BlockAndTintGetter>> updateCalls = new ObjectArrayList<>();
    
    public DynamicLightManager() {
        this.glBuffer = QuartzCore.instance().allocBuffer(true, MagicNumbers.DYNAMIC_LIGHT_BYTE_SIZE);
    }
    
    @Override
    public void delete() {
        glBuffer.delete();
    }
    
    public QuartzDynamicLight alloc(@Nullable Quartz.DynamicLightUpdateFunc updateFunc) {
        DynamicLight dynamicLight = new DynamicLight(glBuffer.alloc(MagicNumbers.DYNAMIC_LIGHT_BYTE_SIZE, MagicNumbers.DYNAMIC_LIGHT_BYTE_SIZE));
        if (updateFunc != null) {
            Consumer<BlockAndTintGetter> updateCall = level -> updateFunc.accept(dynamicLight, level);
            dynamicLight.updateCall = updateCall;
            updateCalls.add(updateCall);
        }
        return dynamicLight;
    }
    
    public void updateAll() {
        var level = Minecraft.getInstance().level;
        assert level != null;
        for (int i = 0; i < updateCalls.size(); i++) {
            updateCalls.get(i).accept(level);
        }
        glBuffer.flushAll();
    }
    
    public GLBuffer buffer() {
        return glBuffer;
    }
}
