package net.roguelogix.phosphophyllite.quartz.internal;

import net.minecraft.world.World;
import net.roguelogix.phosphophyllite.quartz.api.QuartzState;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;
import net.roguelogix.phosphophyllite.threading.WorkQueue;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class WorldManager {
    
    
    public static void init(World world) {
    }
    
    private static final WorkQueue sectionUpdateQueue = new WorkQueue().addProcessingThread();
    
    private static final HashMap<Vector3i, HashMap<Vector3i, QuartzState>> stateSections = new HashMap<>();
    private static final ConcurrentHashMap<Vector3i, AtomicBoolean> sectionsToUpdate = new ConcurrentHashMap<>();
    
    public static void setQuartzState(Vector3i position, @Nullable QuartzState state) {
        Vector3i sectionPos = new Vector3i(position.x & ~0xF, position.y & ~0xF, position.z & ~0xF);
        HashMap<Vector3i, QuartzState> section = stateSections.computeIfAbsent(sectionPos, k -> new HashMap<>());
        synchronized (section) {
            section.put(position, state == null ? null : state.copy());
        }
        markSectionForUpdate(sectionPos);
    }
    
    public static void markSectionForUpdate(Vector3i pos) {
        AtomicBoolean bool = sectionsToUpdate.get(pos);
        if (bool == null) {
            bool = sectionsToUpdate.putIfAbsent(pos, new AtomicBoolean());
            if (bool == null) {
                bool = sectionsToUpdate.get(pos);
            }
        }
        if (!bool.getAndSet(true)) {
            sectionUpdateQueue.enqueue(() -> updateSection(pos));
        }
    }
    
    private static void updateSection(Vector3i sectionPos) {
        if (sectionsToUpdate.get(sectionPos).getAndSet(false)) {
            HashMap<Vector3i, QuartzState> section = stateSections.computeIfAbsent(sectionPos, k -> new HashMap<>());
            synchronized (section) {
                section = (HashMap<Vector3i, QuartzState>) section.clone();
            }
            
            // todo: threadlocal?
            ArrayList<BlockRenderInfo> newRenderInfos = new ArrayList<>();
            ArrayList<Vector3i> toRemove = new ArrayList<>();
            
            for (Vector3i blockPos : section.keySet()) {
                BlockRenderInfo renderInfo = new BlockRenderInfo();
                newRenderInfos.add(renderInfo);
                
                renderInfo.x = blockPos.x;
                renderInfo.y = blockPos.y;
                renderInfo.z = blockPos.z;
                
                QuartzState state = section.get(blockPos);
                if (state == null) {
                    toRemove.add(blockPos);
                    break;
                }
                
                renderInfo.textureData(StateCache.infoForState(state));
                renderInfo.lightingData(LightingManager.getLightData(blockPos));
            }
            
            if (!toRemove.isEmpty()) {
                section = stateSections.get(sectionPos);
                synchronized (section) {
                    for (Vector3i vector3i : toRemove) {
                        section.remove(vector3i);
                    }
                }
            }
            
            if(newRenderInfos.isEmpty()){
                return;
            }
            
            Renderer.INSTANCE.setBlockRenderInfo(newRenderInfos);
        }
    }
}