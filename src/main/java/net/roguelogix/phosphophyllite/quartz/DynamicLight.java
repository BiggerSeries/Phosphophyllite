package net.roguelogix.phosphophyllite.quartz;

import net.minecraft.world.level.BlockAndTintGetter;

import javax.annotation.Nullable;

public interface DynamicLight {
    
    enum Type {
        SMOOTH,
        FLAT,
        INTERNAL,
//        AUTOMATIC, // TODO: query the MC settings
    }
    
    // [0, 64) range
    void write(int vertex, int vertexDirection, byte skyLight, byte blockLight, byte AO);
    
    default void write(byte skyLight, byte blockLight, byte AO) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 6; j++) {
                write(i, j, skyLight, blockLight, AO);
            }
        }
    }
    
    void update(BlockAndTintGetter blockAndTintGetter);
    
    interface UpdateFunc {
        void accept(DynamicLight light, BlockAndTintGetter blockAndTintGetter);
    }
    
    interface Manager {
        DynamicLight createLight(UpdateFunc updateFunc);
    
        boolean owns(@Nullable DynamicLight dynamicLight);
    }
}
