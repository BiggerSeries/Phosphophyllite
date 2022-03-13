package net.roguelogix.phosphophyllite.registry;

import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;

public interface IPhosphophylliteOre {
    /**
     * How many blocks are in each vein.
     */
    default int size() {
        return 8;
    }
    
    /**
     * How many times per chunk this ore attempts to spawn.
     */
    default int count() {
        return 20;
    }
    
    /**
     * The top spawn offset.
     */
    default int offset() {
        return 0;
    }
    
    /**
     * The lowest Y level to spawn at (the bottom spawn offset).
     */
    default int minLevel() {
        return Integer.MIN_VALUE;
    }
    
    /**
     * The highest Y level to spawn at.
     */
    default int maxLevel() {
        return 64;
    }
    
    /**
     * If true, this ore will spawn in netherrack, rather than stone.
     */
    default boolean isNetherOre() {
        return false;
    }
    
    @Nullable
    default Block stoneVariant(){
        if(this instanceof Block block){
            return block;
        }
        return null;
    }
    
    @Nullable
    default Block deepslateVariant(){
        if(this instanceof Block block){
            return block;
        }
        return null;
    }
    
    /**
     * What biomes this ore can spawn in, by key/name. Defaults to ALL biomes, if omitted.
     *
     * @see net.minecraft.world.biome.Biomes for a list.
     */
    default String[] spawnBiomes() {
        return new String[0];
    }
    
    /**
     * Should the ore spawn, use for config disables
     *
     * @return do spawn
     */
    default boolean doSpawn() {
        return true;
    }
}
