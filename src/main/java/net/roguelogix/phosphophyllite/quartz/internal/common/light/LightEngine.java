package net.roguelogix.phosphophyllite.quartz.internal.common.light;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.roguelogix.phosphophyllite.quartz.QuartzDynamicLight;
import net.roguelogix.phosphophyllite.quartz.internal.common.gl.GLDeletable;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3ic;
import net.roguelogix.phosphophyllite.util.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Keeps track of when mojang's light engine is updated, and runs light update callbacks as needed
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LightEngine implements GLDeletable {
    
    private static LightEngine ENGINE;
    
    private final Long2ObjectOpenHashMap<LightChunk> liveChunks = new Long2ObjectOpenHashMap<>();
    
    public LightEngine() {
        if (ENGINE != null) {
            throw new IllegalStateException("LightEngine is a singleton");
        }
        ENGINE = this;
    }
    
    @Override
    public void delete() {
        ENGINE = null;
    }
    
    private LightChunk getChunkFor(long longPos) {
        return liveChunks.computeIfAbsent(longPos, lonk -> new LightChunk(lonk, liveChunks::remove));
    }
    
    public void update(BlockAndTintGetter blockAndTintGetter) {
        liveChunks.forEach(((aLong, lightChunk) -> lightChunk.runUpdate(blockAndTintGetter)));
    }
    
    public static void sectionDirty(int x, int y, int z) {
        ENGINE.sectionDirtyInternal(x, y, z);
    }
    
    private void sectionDirtyInternal(int x, int y, int z) {
        long pos = SectionPos.asLong(x, y, z);
        var chunk = liveChunks.get(pos);
        if (chunk != null) {
            chunk.markDirty();
        }
    }
    
    public DynamicLightManager.DynamicLight createLightForPos(Vector3ic pos, DynamicLightManager lightManager, QuartzDynamicLight.Type lightType) {
        long longPos = BlockPos.asLong(pos.x(), pos.y(), pos.z());
        longPos = SectionPos.blockToSection(longPos);
        var chunk = getChunkFor(longPos);
        return chunk.createLightForPos(pos, lightManager, lightType);
    }
}
