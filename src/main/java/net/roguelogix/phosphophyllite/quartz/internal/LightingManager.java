package net.roguelogix.phosphophyllite.quartz.internal;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.lighting.IWorldLightListener;
import net.minecraft.world.lighting.WorldLightManager;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;
import net.roguelogix.phosphophyllite.threading.WorkQueue;

import java.util.concurrent.ConcurrentHashMap;

public class LightingManager {
    
    private static World world;
    
    private static IWorldLightListener blockLight;
    private static IWorldLightListener skyLight;
    
    public static void init(World world) {
        LightingManager.world = world;
        WorldLightManager lightManager = world.getLightManager();
        blockLight = lightManager.getLightEngine(LightType.BLOCK);
        skyLight = lightManager.getLightEngine(LightType.SKY);
        lightData.clear();
    }
    
    public static class BlockFaceLightData {
        public static final BlockFaceLightData defaultData = new BlockFaceLightData();
        
        public boolean hideWest = false;
        public boolean hideEast = false;
        public boolean hideBottom = false;
        public boolean hideTop = false;
        public boolean hideNorth = false;
        public boolean hideSouth = false;
        
        public byte lightmapBlocklightWestLYLZ = 0x3F;
        public byte lightmapSkylightWestLYLZ = 0x3F;
        public byte AOWestLYLZ = 0;
        public byte lightmapBlocklightWestHYLZ = 0x3F;
        public byte lightmapSkylightWestHYLZ = 0x3F;
        public byte AOWestHYLZ = 0;
        public byte lightmapBlocklightWestLYHZ = 0x3F;
        public byte lightmapSkylightWestLYHZ = 0x3F;
        public byte AOWestLYHZ = 0;
        public byte lightmapBlocklightWestHYHZ = 0x3F;
        public byte lightmapSkylightWestHYHZ = 0x3F;
        public byte AOWestHYHZ = 0;
        
        public byte lightmapBlocklightEastLYLZ = 0x3F;
        public byte lightmapSkylightEastLYLZ = 0x3F;
        public byte AOEastLYLZ = 0;
        public byte lightmapBlocklightEastHYLZ = 0x3F;
        public byte lightmapSkylightEastHYLZ = 0x3F;
        public byte AOEastHYLZ = 0;
        public byte lightmapBlocklightEastLYHZ = 0x3F;
        public byte lightmapSkylightEastLYHZ = 0x3F;
        public byte AOEastLYHZ = 0;
        public byte lightmapBlocklightEastHYHZ = 0x3F;
        public byte lightmapSkylightEastHYHZ = 0x3F;
        public byte AOEastHYHZ = 0;
        
        public byte lightmapBlocklightBottomLXLZ = 0x3F;
        public byte lightmapSkylightBottomLXLZ = 0x3F;
        public byte AOBottomLXLZ = 0;
        public byte lightmapBlocklightBottomHXLZ = 0x3F;
        public byte lightmapSkylightBottomHXLZ = 0x3F;
        public byte AOBottomHXLZ = 0;
        public byte lightmapBlocklightBottomLXHZ = 0x3F;
        public byte lightmapSkylightBottomLXHZ = 0x3F;
        public byte AOBottomLXHZ = 0;
        public byte lightmapBlocklightBottomHXHZ = 0x3F;
        public byte lightmapSkylightBottomHXHZ = 0x3F;
        public byte AOBottomHXHZ = 0;
        
        public byte lightmapBlocklightTopLXLZ = 0x3F;
        public byte lightmapSkylightTopLXLZ = 0x3F;
        public byte AOTopLXLZ = 0;
        public byte lightmapBlocklightTopHXLZ = 0x3F;
        public byte lightmapSkylightTopHXLZ = 0x3F;
        public byte AOTopHXLZ = 0;
        public byte lightmapBlocklightTopLXHZ = 0x3F;
        public byte lightmapSkylightTopLXHZ = 0x3F;
        public byte AOTopLXHZ = 0;
        public byte lightmapBlocklightTopHXHZ = 0x3F;
        public byte lightmapSkylightTopHXHZ = 0x3F;
        public byte AOTopHXHZ = 0;
        
        public byte lightmapBlocklightNorthLXLY = 0x3F;
        public byte lightmapSkylightNorthLXLY = 0x3F;
        public byte AONorthLXLY = 0;
        public byte lightmapBlocklightNorthHXLY = 0x3F;
        public byte lightmapSkylightNorthHXLY = 0x3F;
        public byte AONorthHXLY = 0;
        public byte lightmapBlocklightNorthLXHY = 0x3F;
        public byte lightmapSkylightNorthLXHY = 0x3F;
        public byte AONorthLXHY = 0;
        public byte lightmapBlocklightNorthHXHY = 0x3F;
        public byte lightmapSkylightNorthHXHY = 0x3F;
        public byte AONorthHXHY = 0;
        
        public byte lightmapBlocklightSouthLXLY = 0x3F;
        public byte lightmapSkylightSouthLXLY = 0x3F;
        public byte AOSouthLXLY = 0;
        public byte lightmapBlocklightSouthHXLY = 0x3F;
        public byte lightmapSkylightSouthHXLY = 0x3F;
        public byte AOSouthHXLY = 0;
        public byte lightmapBlocklightSouthLXHY = 0x3F;
        public byte lightmapSkylightSouthLXHY = 0x3F;
        public byte AOSouthLXHY = 0;
        public byte lightmapBlocklightSouthHXHY = 0x3F;
        public byte lightmapSkylightSouthHXHY = 0x3F;
        public byte AOSouthHXHY = 0;
    }
    
    private static ConcurrentHashMap<Vector3i, BlockFaceLightData[][][]> lightData = new ConcurrentHashMap<>();
    
    public static void loadSection(Vector3i pos) {
        lightData.putIfAbsent(new Vector3i(pos.x & ~0xF, pos.y & ~0xF, pos.z & ~0xF), new BlockFaceLightData[16][16][16]);
        markSectionForRecompute(pos);
    }
    
    public static void unloadSection(Vector3i pos) {
        lightData.remove(new Vector3i(pos.x & ~0xF, pos.y & ~0xF, pos.z & ~0xF));
    }
    
    public static BlockFaceLightData getLightData(Vector3i pos) {
        BlockFaceLightData[][][] data = lightData.get(new Vector3i(pos.x & ~0xF, pos.y & ~0xF, pos.z & ~0xF));
        if (data == null) {
            return BlockFaceLightData.defaultData;
        }
        return data[pos.x & 0xF][pos.y & 0xF][pos.z & 0xF];
    }
    
    private static final WorkQueue queue = new WorkQueue().addProcessingThreads(4);
    
    public static void markSectionForRecompute(Vector3i pos) {
        queue.enqueue(() -> {
            recomputeSection(pos);
            WorldManager.markSectionForUpdate(pos);
        });
    }
    
    private static byte AOMode(boolean sideA, boolean corner, boolean sideB) {
        if (sideA && sideB) {
            return 3;
        }
        if ((sideA || sideB) && corner) {
            return 2;
        }
        if (sideA || sideB || corner) {
            return 1;
        }
        return 0;
    }
    
    // well, "average", brings it into alignment with what the renderer is expecting anyway
    private static byte averageLightLevels(byte lightA, byte lightB, byte lightC, byte lightD) {
        if (lightB == 0) {
            lightB = lightA;
        }
        if (lightC == 0) {
            lightC = lightA;
        }
        if (lightD == 0) {
            lightD = lightA;
        }
        return (byte) (lightA + lightB + lightC + lightD);
    }
    
    private static void recomputeSection(Vector3i pos) {
        BlockFaceLightData[][][] sectionLightData = lightData.get(new Vector3i(pos.x & ~0xF, pos.y & ~0xF, pos.z & ~0xF));
        
        if (sectionLightData == null) {
            return;
        }
        synchronized (sectionLightData) {
            
            byte[][][] blockLightLevels = new byte[18][18][18];
            byte[][][] skyLightLevels = new byte[18][18][18];
            boolean[][][] opqaueBlocks = new boolean[18][18][18];
            
            
            Vector3i currentPos = new Vector3i(pos.x, pos.y, pos.z);
            
            currentPos.sub(1, 1, 1);
            
            BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable();
            
            for (int i = 0; i < 18; i++) {
                for (int j = 0; j < 18; j++) {
                    for (int k = 0; k < 18; k++) {
                        mutableBlockPos.setPos(currentPos.x, currentPos.y, currentPos.z);
                        
                        blockLightLevels[i][j][k] = (byte) blockLight.getLightFor(mutableBlockPos);
                        skyLightLevels[i][j][k] = (byte) skyLight.getLightFor(mutableBlockPos);
                        
                        opqaueBlocks[i][j][k] = world.getBlockState(mutableBlockPos).hasOpaqueCollisionShape(world, mutableBlockPos);
                        
                        currentPos.add(0, 0, 1);
                    }
                    currentPos.add(0, 1, 0);
                    currentPos.sub(0, 0, 18);
                }
                currentPos.add(1, 0, 0);
                currentPos.sub(0, 18, 0);
            }
            currentPos.sub(18, 0, 0);
            
            currentPos.add(1, 1, 1);
            for (int i = 1; i < 17; i++) {
                for (int j = 1; j < 17; j++) {
                    for (int k = 1; k < 17; k++) {
                        
                        BlockFaceLightData lightData = sectionLightData[i - 1][j - 1][k - 1];
                        if (lightData == null) {
                            lightData = new BlockFaceLightData();
                            sectionLightData[i - 1][j - 1][k - 1] = lightData;
                        }
                        
                        if (!(lightData.hideWest = opqaueBlocks[i - 1][j][k])) {
                            lightData.lightmapBlocklightWestLYLZ = averageLightLevels(blockLightLevels[i - 1][j][k], blockLightLevels[i - 1][j - 1][k], blockLightLevels[i - 1][j - 1][k - 1], blockLightLevels[i - 1][j][k - 1]);
                            lightData.lightmapSkylightWestLYLZ = averageLightLevels(skyLightLevels[i - 1][j][k], skyLightLevels[i - 1][j - 1][k], skyLightLevels[i - 1][j - 1][k - 1], skyLightLevels[i - 1][j][k - 1]);
                            lightData.AOWestLYLZ = AOMode(opqaueBlocks[i - 1][j - 1][k], opqaueBlocks[i - 1][j - 1][k - 1], opqaueBlocks[i - 1][j][k - 1]);
                            lightData.lightmapBlocklightWestHYLZ = averageLightLevels(blockLightLevels[i - 1][j][k], blockLightLevels[i - 1][j + 1][k], blockLightLevels[i - 1][j + 1][k - 1], blockLightLevels[i - 1][j][k - 1]);
                            lightData.lightmapSkylightWestHYLZ = averageLightLevels(skyLightLevels[i - 1][j][k], skyLightLevels[i - 1][j + 1][k], skyLightLevels[i - 1][j + 1][k - 1], skyLightLevels[i - 1][j][k - 1]);
                            lightData.AOWestHYLZ = AOMode(opqaueBlocks[i - 1][j + 1][k], opqaueBlocks[i - 1][j + 1][k - 1], opqaueBlocks[i - 1][j][k - 1]);
                            lightData.lightmapBlocklightWestLYHZ = averageLightLevels(blockLightLevels[i - 1][j][k], blockLightLevels[i - 1][j - 1][k], blockLightLevels[i - 1][j - 1][k + 1], blockLightLevels[i - 1][j][k + 1]);
                            lightData.lightmapSkylightWestLYHZ = averageLightLevels(skyLightLevels[i - 1][j][k], skyLightLevels[i - 1][j - 1][k], skyLightLevels[i - 1][j - 1][k + 1], skyLightLevels[i - 1][j][k + 1]);
                            lightData.AOWestLYHZ = AOMode(opqaueBlocks[i - 1][j - 1][k], opqaueBlocks[i - 1][j - 1][k + 1], opqaueBlocks[i - 1][j][k + 1]);
                            lightData.lightmapBlocklightWestHYHZ = averageLightLevels(blockLightLevels[i - 1][j][k], blockLightLevels[i - 1][j + 1][k], blockLightLevels[i - 1][j + 1][k + 1], blockLightLevels[i - 1][j][k + 1]);
                            lightData.lightmapSkylightWestHYHZ = averageLightLevels(skyLightLevels[i - 1][j][k], skyLightLevels[i - 1][j + 1][k], skyLightLevels[i - 1][j + 1][k + 1], skyLightLevels[i - 1][j][k + 1]);
                            lightData.AOWestHYHZ = AOMode(opqaueBlocks[i - 1][j + 1][k], opqaueBlocks[i - 1][j + 1][k + 1], opqaueBlocks[i - 1][j][k + 1]);
                        }
                        
                        if(!(lightData.hideEast = opqaueBlocks[i + 1][j][k])) {
                            lightData.lightmapBlocklightEastLYLZ = averageLightLevels(blockLightLevels[i + 1][j][k], blockLightLevels[i + 1][j - 1][k], blockLightLevels[i + 1][j - 1][k - 1], blockLightLevels[i + 1][j][k - 1]);
                            lightData.lightmapSkylightEastLYLZ = averageLightLevels(skyLightLevels[i + 1][j][k], skyLightLevels[i + 1][j - 1][k], skyLightLevels[i + 1][j - 1][k - 1], skyLightLevels[i + 1][j][k - 1]);
                            lightData.AOEastLYLZ = AOMode(opqaueBlocks[i + 1][j - 1][k], opqaueBlocks[i + 1][j - 1][k - 1], opqaueBlocks[i + 1][j][k - 1]);
                            lightData.lightmapBlocklightEastHYLZ = averageLightLevels(blockLightLevels[i + 1][j][k], blockLightLevels[i + 1][j + 1][k], blockLightLevels[i + 1][j + 1][k - 1], blockLightLevels[i + 1][j][k - 1]);
                            lightData.lightmapSkylightEastHYLZ = averageLightLevels(skyLightLevels[i + 1][j][k], skyLightLevels[i + 1][j + 1][k], skyLightLevels[i + 1][j + 1][k - 1], skyLightLevels[i + 1][j][k - 1]);
                            lightData.AOEastHYLZ = AOMode(opqaueBlocks[i + 1][j + 1][k], opqaueBlocks[i + 1][j + 1][k - 1], opqaueBlocks[i + 1][j][k - 1]);
                            lightData.lightmapBlocklightEastLYHZ = averageLightLevels(blockLightLevels[i + 1][j][k], blockLightLevels[i + 1][j - 1][k], blockLightLevels[i + 1][j - 1][k + 1], blockLightLevels[i + 1][j][k + 1]);
                            lightData.lightmapSkylightEastLYHZ = averageLightLevels(skyLightLevels[i + 1][j][k], skyLightLevels[i + 1][j - 1][k], skyLightLevels[i + 1][j - 1][k + 1], skyLightLevels[i + 1][j][k + 1]);
                            lightData.AOEastLYHZ = AOMode(opqaueBlocks[i + 1][j - 1][k], opqaueBlocks[i + 1][j - 1][k + 1], opqaueBlocks[i + 1][j][k + 1]);
                            lightData.lightmapBlocklightEastHYHZ = averageLightLevels(blockLightLevels[i + 1][j][k], blockLightLevels[i + 1][j + 1][k], blockLightLevels[i + 1][j + 1][k + 1], blockLightLevels[i + 1][j][k + 1]);
                            lightData.lightmapSkylightEastHYHZ = averageLightLevels(skyLightLevels[i + 1][j][k], skyLightLevels[i + 1][j + 1][k], skyLightLevels[i + 1][j + 1][k + 1], skyLightLevels[i + 1][j][k + 1]);
                            lightData.AOEastHYHZ = AOMode(opqaueBlocks[i + 1][j + 1][k], opqaueBlocks[i + 1][j + 1][k + 1], opqaueBlocks[i + 1][j][k + 1]);
                        }
    
                        if(!(lightData.hideBottom = opqaueBlocks[i][j - 1][k])) {
                            lightData.lightmapBlocklightBottomLXLZ = averageLightLevels(blockLightLevels[i][j - 1][k], blockLightLevels[i - 1][j - 1][k], blockLightLevels[i - 1][j - 1][k - 1], blockLightLevels[i][j - 1][k - 1]);
                            lightData.lightmapSkylightBottomLXLZ = averageLightLevels(skyLightLevels[i][j - 1][k], skyLightLevels[i - 1][j - 1][k], skyLightLevels[i - 1][j - 1][k - 1], skyLightLevels[i][j - 1][k - 1]);
                            lightData.AOBottomLXLZ = AOMode(opqaueBlocks[i - 1][j - 1][k], opqaueBlocks[i - 1][j - 1][k - 1], opqaueBlocks[i][j - 1][k - 1]);
                            lightData.lightmapBlocklightBottomHXLZ = averageLightLevels(blockLightLevels[i][j - 1][k], blockLightLevels[i + 1][j - 1][k], blockLightLevels[i + 1][j - 1][k - 1], blockLightLevels[i][j - 1][k - 1]);
                            lightData.lightmapSkylightBottomHXLZ = averageLightLevels(skyLightLevels[i][j - 1][k], skyLightLevels[i + 1][j - 1][k], skyLightLevels[i + 1][j - 1][k - 1], skyLightLevels[i][j - 1][k - 1]);
                            lightData.AOBottomHXLZ = AOMode(opqaueBlocks[i + 1][j - 1][k], opqaueBlocks[i + 1][j - 1][k - 1], opqaueBlocks[i][j - 1][k - 1]);
                            lightData.lightmapBlocklightBottomLXHZ = averageLightLevels(blockLightLevels[i][j - 1][k], blockLightLevels[i - 1][j - 1][k], blockLightLevels[i - 1][j - 1][k + 1], blockLightLevels[i][j - 1][k + 1]);
                            lightData.lightmapSkylightBottomLXHZ = averageLightLevels(skyLightLevels[i][j - 1][k], skyLightLevels[i - 1][j - 1][k], skyLightLevels[i - 1][j - 1][k + 1], skyLightLevels[i][j - 1][k + 1]);
                            lightData.AOBottomLXHZ = AOMode(opqaueBlocks[i - 1][j - 1][k], opqaueBlocks[i - 1][j - 1][k + 1], opqaueBlocks[i][j - 1][k + 1]);
                            lightData.lightmapBlocklightBottomHXHZ = averageLightLevels(blockLightLevels[i][j - 1][k], blockLightLevels[i + 1][j - 1][k], blockLightLevels[i + 1][j - 1][k + 1], blockLightLevels[i][j - 1][k + 1]);
                            lightData.lightmapSkylightBottomHXHZ = averageLightLevels(skyLightLevels[i][j - 1][k], skyLightLevels[i + 1][j - 1][k], skyLightLevels[i + 1][j - 1][k + 1], skyLightLevels[i][j - 1][k + 1]);
                            lightData.AOBottomHXHZ = AOMode(opqaueBlocks[i + 1][j - 1][k], opqaueBlocks[i + 1][j - 1][k + 1], opqaueBlocks[i][j - 1][k + 1]);
                        }
    
                        if(!(lightData.hideTop = opqaueBlocks[i][j + 1][k])) {
                            lightData.lightmapBlocklightTopLXLZ = averageLightLevels(blockLightLevels[i][j + 1][k], blockLightLevels[i - 1][j + 1][k], blockLightLevels[i - 1][j + 1][k - 1], blockLightLevels[i][j + 1][k - 1]);
                            lightData.lightmapSkylightTopLXLZ = averageLightLevels(skyLightLevels[i][j + 1][k], skyLightLevels[i - 1][j + 1][k], skyLightLevels[i - 1][j + 1][k - 1], skyLightLevels[i][j + 1][k - 1]);
                            lightData.AOTopLXLZ = AOMode(opqaueBlocks[i - 1][j + 1][k], opqaueBlocks[i - 1][j + 1][k - 1], opqaueBlocks[i][j + 1][k - 1]);
                            lightData.lightmapBlocklightTopHXLZ = averageLightLevels(blockLightLevels[i][j + 1][k], blockLightLevels[i + 1][j + 1][k], blockLightLevels[i + 1][j + 1][k - 1], blockLightLevels[i][j + 1][k - 1]);
                            lightData.lightmapSkylightTopHXLZ = averageLightLevels(skyLightLevels[i][j + 1][k], skyLightLevels[i + 1][j + 1][k], skyLightLevels[i + 1][j + 1][k - 1], skyLightLevels[i][j + 1][k - 1]);
                            lightData.AOTopHXLZ = AOMode(opqaueBlocks[i + 1][j + 1][k], opqaueBlocks[i + 1][j + 1][k - 1], opqaueBlocks[i][j + 1][k - 1]);
                            lightData.lightmapBlocklightTopLXHZ = averageLightLevels(blockLightLevels[i][j + 1][k], blockLightLevels[i - 1][j + 1][k], blockLightLevels[i - 1][j + 1][k + 1], blockLightLevels[i][j + 1][k + 1]);
                            lightData.lightmapSkylightTopLXHZ = averageLightLevels(skyLightLevels[i][j + 1][k], skyLightLevels[i - 1][j + 1][k], skyLightLevels[i - 1][j + 1][k + 1], skyLightLevels[i][j + 1][k + 1]);
                            lightData.AOTopLXHZ = AOMode(opqaueBlocks[i - 1][j + 1][k], opqaueBlocks[i - 1][j + 1][k + 1], opqaueBlocks[i][j + 1][k + 1]);
                            lightData.lightmapBlocklightTopHXHZ = averageLightLevels(blockLightLevels[i][j + 1][k], blockLightLevels[i + 1][j + 1][k], blockLightLevels[i + 1][j + 1][k + 1], blockLightLevels[i][j + 1][k + 1]);
                            lightData.lightmapSkylightTopHXHZ = averageLightLevels(skyLightLevels[i][j + 1][k], skyLightLevels[i + 1][j + 1][k], skyLightLevels[i + 1][j + 1][k + 1], skyLightLevels[i][j + 1][k + 1]);
                            lightData.AOTopHXHZ = AOMode(opqaueBlocks[i + 1][j + 1][k], opqaueBlocks[i + 1][j + 1][k + 1], opqaueBlocks[i][j + 1][k + 1]);
                        }
    
                        if(!(lightData.hideNorth = opqaueBlocks[i][j][k - 1])) {
                            lightData.lightmapBlocklightNorthLXLY = averageLightLevels(blockLightLevels[i][j][k - 1], blockLightLevels[i - 1][j][k - 1], blockLightLevels[i - 1][j - 1][k - 1], blockLightLevels[i][j - 1][k - 1]);
                            lightData.lightmapSkylightNorthLXLY = averageLightLevels(skyLightLevels[i][j][k - 1], skyLightLevels[i - 1][j][k - 1], skyLightLevels[i - 1][j - 1][k - 1], skyLightLevels[i][j - 1][k - 1]);
                            lightData.AONorthLXLY = AOMode(opqaueBlocks[i - 1][j][k - 1], opqaueBlocks[i - 1][j - 1][k - 1], opqaueBlocks[i][j - 1][k - 1]);
                            lightData.lightmapBlocklightNorthHXLY = averageLightLevels(blockLightLevels[i][j][k - 1], blockLightLevels[i + 1][j][k - 1], blockLightLevels[i + 1][j - 1][k - 1], blockLightLevels[i][j - 1][k - 1]);
                            lightData.lightmapSkylightNorthHXLY = averageLightLevels(skyLightLevels[i][j][k - 1], skyLightLevels[i + 1][j][k - 1], skyLightLevels[i + 1][j - 1][k - 1], skyLightLevels[i][j - 1][k - 1]);
                            lightData.AONorthHXLY = AOMode(opqaueBlocks[i + 1][j][k - 1], opqaueBlocks[i + 1][j - 1][k - 1], opqaueBlocks[i][j - 1][k - 1]);
                            lightData.lightmapBlocklightNorthLXHY = averageLightLevels(blockLightLevels[i][j][k - 1], blockLightLevels[i - 1][j][k - 1], blockLightLevels[i - 1][j + 1][k - 1], blockLightLevels[i][j + 1][k - 1]);
                            lightData.lightmapSkylightNorthLXHY = averageLightLevels(skyLightLevels[i][j][k - 1], skyLightLevels[i - 1][j][k - 1], skyLightLevels[i - 1][j + 1][k - 1], skyLightLevels[i][j + 1][k - 1]);
                            lightData.AONorthLXHY = AOMode(opqaueBlocks[i - 1][j][k - 1], opqaueBlocks[i - 1][j + 1][k - 1], opqaueBlocks[i][j + 1][k - 1]);
                            lightData.lightmapBlocklightNorthHXHY = averageLightLevels(blockLightLevels[i][j][k - 1], blockLightLevels[i + 1][j][k - 1], blockLightLevels[i + 1][j + 1][k - 1], blockLightLevels[i][j + 1][k - 1]);
                            lightData.lightmapSkylightNorthHXHY = averageLightLevels(skyLightLevels[i][j][k - 1], skyLightLevels[i + 1][j][k - 1], skyLightLevels[i + 1][j + 1][k - 1], skyLightLevels[i][j + 1][k - 1]);
                            lightData.AONorthHXHY = AOMode(opqaueBlocks[i + 1][j][k - 1], opqaueBlocks[i + 1][j + 1][k - 1], opqaueBlocks[i][j + 1][k - 1]);
                        }
    
                        if(!(lightData.hideSouth = opqaueBlocks[i][j][k + 1])) {
                            lightData.lightmapBlocklightSouthLXLY = averageLightLevels(blockLightLevels[i][j][k + 1], blockLightLevels[i - 1][j][k + 1], blockLightLevels[i - 1][j - 1][k + 1], blockLightLevels[i][j - 1][k + 1]);
                            lightData.lightmapSkylightSouthLXLY = averageLightLevels(skyLightLevels[i][j][k + 1], skyLightLevels[i - 1][j][k + 1], skyLightLevels[i - 1][j - 1][k + 1], skyLightLevels[i][j - 1][k + 1]);
                            lightData.AOSouthLXLY = AOMode(opqaueBlocks[i - 1][j][k + 1], opqaueBlocks[i - 1][j - 1][k + 1], opqaueBlocks[i][j - 1][k + 1]);
                            lightData.lightmapBlocklightSouthHXLY = averageLightLevels(blockLightLevels[i][j][k + 1], blockLightLevels[i + 1][j][k + 1], blockLightLevels[i + 1][j - 1][k + 1], blockLightLevels[i][j - 1][k + 1]);
                            lightData.lightmapSkylightSouthHXLY = averageLightLevels(skyLightLevels[i][j][k + 1], skyLightLevels[i + 1][j][k + 1], skyLightLevels[i + 1][j - 1][k + 1], skyLightLevels[i][j - 1][k + 1]);
                            lightData.AOSouthHXLY = AOMode(opqaueBlocks[i + 1][j][k + 1], opqaueBlocks[i + 1][j - 1][k + 1], opqaueBlocks[i][j - 1][k + 1]);
                            lightData.lightmapBlocklightSouthLXHY = averageLightLevels(blockLightLevels[i][j][k + 1], blockLightLevels[i - 1][j][k + 1], blockLightLevels[i - 1][j + 1][k + 1], blockLightLevels[i][j + 1][k + 1]);
                            lightData.lightmapSkylightSouthLXHY = averageLightLevels(skyLightLevels[i][j][k + 1], skyLightLevels[i - 1][j][k + 1], skyLightLevels[i - 1][j + 1][k + 1], skyLightLevels[i][j + 1][k + 1]);
                            lightData.AOSouthLXHY = AOMode(opqaueBlocks[i - 1][j][k + 1], opqaueBlocks[i - 1][j + 1][k + 1], opqaueBlocks[i][j + 1][k + 1]);
                            lightData.lightmapBlocklightSouthHXHY = averageLightLevels(blockLightLevels[i][j][k + 1], blockLightLevels[i + 1][j][k + 1], blockLightLevels[i + 1][j + 1][k + 1], blockLightLevels[i][j + 1][k + 1]);
                            lightData.lightmapSkylightSouthHXHY = averageLightLevels(skyLightLevels[i][j][k + 1], skyLightLevels[i + 1][j][k + 1], skyLightLevels[i + 1][j + 1][k + 1], skyLightLevels[i][j + 1][k + 1]);
                            lightData.AOSouthHXHY = AOMode(opqaueBlocks[i + 1][j][k + 1], opqaueBlocks[i + 1][j + 1][k + 1], opqaueBlocks[i][j + 1][k + 1]);
                        }
                        
                        currentPos.add(0, 0, 1);
                    }
                    currentPos.add(0, 1, 0);
                    currentPos.sub(0, 0, 16);
                }
                currentPos.add(1, 0, 0);
                currentPos.sub(0, 16, 0);
            }
            currentPos.sub(16, 0, 0);
        }
    }
}
