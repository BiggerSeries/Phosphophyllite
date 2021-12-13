package net.roguelogix.phosphophyllite.quartz.internal.common;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.level.BlockAndTintGetter;
import net.roguelogix.phosphophyllite.quartz.DynamicLight;
import net.roguelogix.phosphophyllite.quartz.internal.Buffer;
import net.roguelogix.phosphophyllite.quartz.internal.MagicNumbers;
import net.roguelogix.phosphophyllite.quartz.internal.QuartzCore;
import net.roguelogix.phosphophyllite.util.MethodsReturnNonnullByDefault;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DynamicLightManager implements DynamicLight.Manager {
    public static class Light implements DynamicLight {
        final Buffer.Allocation allocation;
        ByteBuffer buffer;
        final DynamicLight.UpdateFunc updateCall;
        
        public Light(Buffer.Allocation allocation, final ObjectArrayList<WeakReference<Light>> lights, UpdateFunc updateCall) {
            this.allocation = allocation;
            this.updateCall = updateCall;
            final var ref = new WeakReference<>(this);
            synchronized (lights) {
                lights.add(ref);
            }
            allocation.addBufferSliceCallback(alloc -> {
                var light = ref.get();
                if (light != null) {
                    light.buffer = alloc.buffer();
                }
            });
            QuartzCore.CLEANER.register(this, () -> {
                synchronized (lights) {
                    int index = lights.indexOf(ref);
                    if (index != -1 && index != lights.size()) {
                        lights.set(index, lights.pop());
                    }
                }
            });
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
        public void update(BlockAndTintGetter blockAndTintGetter) {
            if (updateCall != null) {
                updateCall.accept(this, blockAndTintGetter);
                allocation.dirty();
            }
        }
        
        public int id() {
            return allocation.offset() / MagicNumbers.DYNAMIC_LIGHT_BYTE_SIZE;
        }
    }
    
    private final Buffer buffer;
    private final ObjectArrayList<WeakReference<Light>> lights = new ObjectArrayList<>();
    
    public DynamicLightManager(Buffer buffer) {
        this.buffer = buffer;
    }
    
    @Override
    public Light createLight(DynamicLight.UpdateFunc updateFunc) {
        return new Light(buffer.alloc(MagicNumbers.DYNAMIC_LIGHT_BYTE_SIZE, MagicNumbers.DYNAMIC_LIGHT_BYTE_SIZE), lights, updateFunc);
    }
    
    public Buffer buffer() {
        return buffer;
    }
    
    public void updateAll(BlockAndTintGetter blockAndTintGetter) {
        synchronized (lights) {
            for (int i = 0; i < lights.size(); i++) {
                var light = lights.get(i).get();
                if (light != null) {
                    light.update(blockAndTintGetter);
                }
            }
        }
    }
    
    public boolean owns(@Nullable DynamicLight dynamicLight) {
        if (dynamicLight instanceof Light light) {
            return light.allocation.allocator() == buffer;
        }
        return false;
    }
}
