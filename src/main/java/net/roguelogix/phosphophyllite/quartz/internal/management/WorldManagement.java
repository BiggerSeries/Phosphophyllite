package net.roguelogix.phosphophyllite.quartz.internal.management;

import net.minecraft.client.Minecraft;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.lighting.IWorldLightListener;
import net.minecraft.world.lighting.WorldLightManager;
import net.roguelogix.phosphophyllite.quartz.api.QuartzState;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3ic;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WorldManagement {
    
    static World world;
    
    static IWorldLightListener blockLight;
    static IWorldLightListener skyLight;
    
    public static void init(World world) {
        
        renderChunks.forEach((k, chunk)-> chunk.removeAll());
        renderChunks.clear();
        lightingChunks.clear();
        stateChunks.clear();
        
        WorldManagement.world = world;
        WorldLightManager lightManager = world.getLightManager();
        blockLight = lightManager.getLightEngine(LightType.BLOCK);
        skyLight = lightManager.getLightEngine(LightType.SKY);
    }
    
    static final Map<Vector3ic, StateChunk> stateChunks = new ConcurrentHashMap<>();
    
    static final Map<Vector3ic, LightingChunk> lightingChunks = new ConcurrentHashMap<>();
    
    static final Map<Vector3ic, RenderChunk> renderChunks = new ConcurrentHashMap<>();
    
    
    public static void registerState(Vector3ic pos, QuartzState state) {
        Vector3i chunkPosition = new Vector3i(pos.x() & ~0xF, pos.y() & ~0xF, pos.z() & ~0xF);
        StateChunk stateChunk = stateChunks.get(chunkPosition);
        if (stateChunk == null) {
            if (state == null) {
                // removing where nothing exists, goodbye
                return;
            }
            // aight, lemme make that chunk for you
            stateChunk = new StateChunk(chunkPosition);
            stateChunks.put(new Vector3i(chunkPosition), stateChunk);
        }
        chunkPosition.set(pos);
        stateChunk.setState(chunkPosition, state);
        if (stateChunk.isEmpty()) {
            chunkPosition.set(pos.x() & ~0xF, pos.y() & ~0xF, pos.z() & ~0xF);
            stateChunks.remove(chunkPosition);
        }
    }
    
    public static void playerMovedChunk(int chunkX, int chunkY, int chunkZ) {
        Vector3i chunkPos = new Vector3i(chunkX, chunkY, chunkZ);
        int renderDistanceChunks = Minecraft.getInstance().gameSettings.renderDistanceChunks;
        int renderDistanceBlocks = renderDistanceChunks * 16;
        chunkPos.mul(16);
        renderChunks.keySet().stream().filter(v -> v.distance(chunkPos) > renderDistanceBlocks).forEach(renderchunkPos -> {
            lightingChunks.remove(renderchunkPos);
            renderChunks.remove(renderchunkPos).removeAll();
        });
        Vector3i renderChunkPos = new Vector3i();
        for (int i = chunkX - renderDistanceChunks - 1; i <= chunkX + renderDistanceChunks; i++) {
            for (int j = chunkY - renderDistanceChunks - 1; j <= chunkY + renderDistanceChunks; j++) {
                for (int k = chunkZ - renderDistanceChunks - 1; k <= chunkZ + renderDistanceChunks; k++) {
                    renderChunkPos.set(i * 16, j * 16, k * 16);
                    if(chunkPos.distance(renderChunkPos) <= renderDistanceBlocks){
                        if(!renderChunks.containsKey(renderChunkPos)){
                            Vector3i newRenderChunkPos = new Vector3i(renderChunkPos);
                            renderChunks.put(newRenderChunkPos, new RenderChunk(renderChunkPos));
                            lightingChunks.put(newRenderChunkPos, new LightingChunk(renderChunkPos));
                        }
                    }
                }
            }
        }
    }
    
    public static synchronized void updateRange(Vector3ic posA, Vector3ic posB) {
        assert posA.x() <= posB.x();
        assert posA.y() <= posB.y();
        assert posA.z() <= posB.z();
        Vector3i chunkPos = new Vector3i(posA);
        for (int i = posA.x() & ~0xF; i <= (posB.x()& ~0xF); i += 16) {
            for (int j = posA.y() & ~0xF; j <= (posB.y()& ~0xF); j += 16) {
                for (int k = posA.z() & ~0xF; k <= (posB.z()& ~0xF); k += 16) {
                    chunkPos.set(i, j, k);
                    
                    StateChunk stateChunk = stateChunks.get(chunkPos);
                    LightingChunk lightingChunk = lightingChunks.get(chunkPos);
                    RenderChunk renderChunk = renderChunks.get(chunkPos);
                    
                    if (stateChunk != null) {
                        stateChunk.updateStates();
                        if (stateChunk.isEmpty()) {
                            // its yeeting time!
                            stateChunks.remove(chunkPos);
                            lightingChunks.remove(chunkPos);
                            renderChunk.removeAll();
                        }
                    }
                    
                    if(lightingChunk != null){
                        lightingChunk.updateWorldInfo();
                    }
                    
                    if (renderChunk != null) {
                        renderChunk.updateRange(posA, posB);
                    }
                }
            }
        }
    }
}