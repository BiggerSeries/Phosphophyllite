package net.roguelogix.phosphophyllite.quartz.internal.world;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.SectionPos;
import net.roguelogix.phosphophyllite.quartz.DrawBatch;
import net.roguelogix.phosphophyllite.quartz.internal.QuartzCore;
import net.roguelogix.phosphophyllite.repack.org.joml.AABBi;

import java.lang.ref.WeakReference;

public class WorldEngine {
    
    private final Long2ObjectOpenHashMap<WeakReference<DrawBatch>> sectionDrawBatchers = new Long2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<AABBi, WeakReference<DrawBatch>> customDrawBatchers = new Object2ObjectOpenHashMap<>();
    
    public synchronized DrawBatch getBatcherForAABB(final AABBi aabb) {
        if ((aabb.minX >> 4) == (aabb.maxX >> 4) &&
                (aabb.minY >> 4) == (aabb.maxY >> 4) &&
                (aabb.minZ >> 4) == (aabb.maxZ >> 4)) {
            // AABB is conained entirely  in a single section, just return the section's batcher
            return getBatcherForSection(SectionPos.asLong(aabb.minX >> 4, aabb.minY >> 4, aabb.minZ >> 4));
        }
        final var weakRef = customDrawBatchers.get(aabb);
        DrawBatch drawBatch = null;
        if (weakRef != null) {
            drawBatch = weakRef.get();
        }
        if (drawBatch == null) {
            final var newDrawBatch = QuartzCore.INSTANCE.createDrawBatch();
            final var finalAABB = new AABBi(aabb);
            newDrawBatch.setCullAABB(finalAABB);
            QuartzCore.CLEANER.register(newDrawBatch, () -> {
                synchronized (this){
                    customDrawBatchers.remove(finalAABB);
                }
            });
            customDrawBatchers.put(finalAABB, new WeakReference<>(newDrawBatch));
            return newDrawBatch;
        }
        return drawBatch;
    }
    
    public synchronized DrawBatch getBatcherForSection(final long sectionPos) {
        final var weakRef = sectionDrawBatchers.get(sectionPos);
        DrawBatch drawBatch = null;
        if (weakRef != null) {
            drawBatch = weakRef.get();
        }
        if (drawBatch == null) {
            final var newDrawBatch = QuartzCore.INSTANCE.createDrawBatch();
            newDrawBatch.setCullAABB(new AABBi(0, 0, 0, 16, 16, 16).translate(SectionPos.sectionToBlockCoord(SectionPos.x(sectionPos)), SectionPos.sectionToBlockCoord(SectionPos.y(sectionPos)), SectionPos.sectionToBlockCoord(SectionPos.z(sectionPos))));
            QuartzCore.CLEANER.register(newDrawBatch, () -> {
                synchronized (this){
                    sectionDrawBatchers.remove(sectionPos);
                }
            });
            sectionDrawBatchers.put(sectionPos, new WeakReference<>(newDrawBatch));
            return newDrawBatch;
        }
        return drawBatch;
    }
}
