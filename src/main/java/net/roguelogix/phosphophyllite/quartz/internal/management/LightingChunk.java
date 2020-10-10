package net.roguelogix.phosphophyllite.quartz.internal.management;

import net.minecraft.util.math.BlockPos;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3ic;

public class LightingChunk {
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
    
    private final Vector3ic basePosition;
    private final BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable();
    
    private final BlockFaceLightData[][][] blockLightData = new BlockFaceLightData[16][16][16];
    
    private final byte[][][] blockLightLevels = new byte[18][18][18];
    private final byte[][][] skyLightLevels = new byte[18][18][18];
    private final boolean[][][] opqaueBlocks = new boolean[18][18][18];
    private final boolean[][][] needsUpdate = new boolean[16][16][16];
    
    public LightingChunk(Vector3ic basePosition) {
        this.basePosition = new Vector3i(basePosition);
    }
    
    public void updateWorldInfo() {
        Vector3i currentPos = new Vector3i(basePosition.x(), basePosition.y(), basePosition.z());
        
        currentPos.sub(1, 1, 1);
        
        
        for (int i = 0; i < 18; i++) {
            for (int j = 0; j < 18; j++) {
                for (int k = 0; k < 18; k++) {
                    mutableBlockPos.setPos(currentPos.x, currentPos.y, currentPos.z);
                    
                    byte newBlockLightLevel = (byte) WorldManagement.blockLight.getLightFor(mutableBlockPos);
                    byte newSkyLightLevel = (byte) WorldManagement.skyLight.getLightFor(mutableBlockPos);
                    
//                    boolean isOpaque = WorldManagement.world.getBlockState(mutableBlockPos).hasOpaqueCollisionShape(WorldManagement.world, mutableBlockPos);
                    boolean isOpaque = WorldManagement.world.getBlockState(mutableBlockPos).isSolid();
                    
                    boolean changed = (opqaueBlocks[i][j][k] != isOpaque)
                            || (newBlockLightLevel != blockLightLevels[i][j][k])
                            || (newSkyLightLevel != skyLightLevels[i][j][k]);
                    
                    if (changed) {
                        for (int l = -1; l <= 1; l++) {
                            for (int m = -1; m <= 1; m++) {
                                for (int n = -1; n <= 1; n++) {
                                    int x = i + l - 1;
                                    int y = j + m - 1;
                                    int z = k + n - 1;
                                    if (x >= 0 && x <= 15 &&
                                            y >= 0 && y <= 15 &&
                                            z >= 0 && z <= 15) {
                                        needsUpdate[x][y][z] = true;
                                    }
                                }
                            }
                        }
                    }
                    
                    blockLightLevels[i][j][k] = newBlockLightLevel;
                    skyLightLevels[i][j][k] = newSkyLightLevel;
                    opqaueBlocks[i][j][k] = isOpaque;
                    
                    currentPos.add(0, 0, 1);
                }
                currentPos.add(0, 1, 0);
                currentPos.sub(0, 0, 18);
            }
            currentPos.add(1, 0, 0);
            currentPos.sub(0, 18, 0);
        }
        currentPos.sub(18, 0, 0);
    }
    
    public BlockFaceLightData getBlockData(Vector3i position) {
        position.sub(basePosition);
        BlockFaceLightData data = blockLightData[position.x][position.y][position.z];
        if (data == null) {
            data = new BlockFaceLightData();
            blockLightData[position.x][position.y][position.z] = data;
            needsUpdate[position.x][position.y][position.z] = true;
        }
        
        if (needsUpdate[position.x][position.y][position.z]) {
            
            needsUpdate[position.x][position.y][position.z] = false;
            
            int i = position.x + 1;
            int j = position.y + 1;
            int k = position.z + 1;
            
            if (!(data.hideWest = opqaueBlocks[i - 1][j][k])) {
                data.lightmapBlocklightWestLYLZ = averageLightLevels(blockLightLevels[i - 1][j][k], blockLightLevels[i - 1][j - 1][k], blockLightLevels[i - 1][j - 1][k - 1], blockLightLevels[i - 1][j][k - 1]);
                data.lightmapSkylightWestLYLZ = averageLightLevels(skyLightLevels[i - 1][j][k], skyLightLevels[i - 1][j - 1][k], skyLightLevels[i - 1][j - 1][k - 1], skyLightLevels[i - 1][j][k - 1]);
                data.AOWestLYLZ = AOMode(opqaueBlocks[i - 1][j - 1][k], opqaueBlocks[i - 1][j - 1][k - 1], opqaueBlocks[i - 1][j][k - 1]);
                data.lightmapBlocklightWestHYLZ = averageLightLevels(blockLightLevels[i - 1][j][k], blockLightLevels[i - 1][j + 1][k], blockLightLevels[i - 1][j + 1][k - 1], blockLightLevels[i - 1][j][k - 1]);
                data.lightmapSkylightWestHYLZ = averageLightLevels(skyLightLevels[i - 1][j][k], skyLightLevels[i - 1][j + 1][k], skyLightLevels[i - 1][j + 1][k - 1], skyLightLevels[i - 1][j][k - 1]);
                data.AOWestHYLZ = AOMode(opqaueBlocks[i - 1][j + 1][k], opqaueBlocks[i - 1][j + 1][k - 1], opqaueBlocks[i - 1][j][k - 1]);
                data.lightmapBlocklightWestLYHZ = averageLightLevels(blockLightLevels[i - 1][j][k], blockLightLevels[i - 1][j - 1][k], blockLightLevels[i - 1][j - 1][k + 1], blockLightLevels[i - 1][j][k + 1]);
                data.lightmapSkylightWestLYHZ = averageLightLevels(skyLightLevels[i - 1][j][k], skyLightLevels[i - 1][j - 1][k], skyLightLevels[i - 1][j - 1][k + 1], skyLightLevels[i - 1][j][k + 1]);
                data.AOWestLYHZ = AOMode(opqaueBlocks[i - 1][j - 1][k], opqaueBlocks[i - 1][j - 1][k + 1], opqaueBlocks[i - 1][j][k + 1]);
                data.lightmapBlocklightWestHYHZ = averageLightLevels(blockLightLevels[i - 1][j][k], blockLightLevels[i - 1][j + 1][k], blockLightLevels[i - 1][j + 1][k + 1], blockLightLevels[i - 1][j][k + 1]);
                data.lightmapSkylightWestHYHZ = averageLightLevels(skyLightLevels[i - 1][j][k], skyLightLevels[i - 1][j + 1][k], skyLightLevels[i - 1][j + 1][k + 1], skyLightLevels[i - 1][j][k + 1]);
                data.AOWestHYHZ = AOMode(opqaueBlocks[i - 1][j + 1][k], opqaueBlocks[i - 1][j + 1][k + 1], opqaueBlocks[i - 1][j][k + 1]);
            }
            
            if (!(data.hideEast = opqaueBlocks[i + 1][j][k])) {
                data.lightmapBlocklightEastLYLZ = averageLightLevels(blockLightLevels[i + 1][j][k], blockLightLevels[i + 1][j - 1][k], blockLightLevels[i + 1][j - 1][k - 1], blockLightLevels[i + 1][j][k - 1]);
                data.lightmapSkylightEastLYLZ = averageLightLevels(skyLightLevels[i + 1][j][k], skyLightLevels[i + 1][j - 1][k], skyLightLevels[i + 1][j - 1][k - 1], skyLightLevels[i + 1][j][k - 1]);
                data.AOEastLYLZ = AOMode(opqaueBlocks[i + 1][j - 1][k], opqaueBlocks[i + 1][j - 1][k - 1], opqaueBlocks[i + 1][j][k - 1]);
                data.lightmapBlocklightEastHYLZ = averageLightLevels(blockLightLevels[i + 1][j][k], blockLightLevels[i + 1][j + 1][k], blockLightLevels[i + 1][j + 1][k - 1], blockLightLevels[i + 1][j][k - 1]);
                data.lightmapSkylightEastHYLZ = averageLightLevels(skyLightLevels[i + 1][j][k], skyLightLevels[i + 1][j + 1][k], skyLightLevels[i + 1][j + 1][k - 1], skyLightLevels[i + 1][j][k - 1]);
                data.AOEastHYLZ = AOMode(opqaueBlocks[i + 1][j + 1][k], opqaueBlocks[i + 1][j + 1][k - 1], opqaueBlocks[i + 1][j][k - 1]);
                data.lightmapBlocklightEastLYHZ = averageLightLevels(blockLightLevels[i + 1][j][k], blockLightLevels[i + 1][j - 1][k], blockLightLevels[i + 1][j - 1][k + 1], blockLightLevels[i + 1][j][k + 1]);
                data.lightmapSkylightEastLYHZ = averageLightLevels(skyLightLevels[i + 1][j][k], skyLightLevels[i + 1][j - 1][k], skyLightLevels[i + 1][j - 1][k + 1], skyLightLevels[i + 1][j][k + 1]);
                data.AOEastLYHZ = AOMode(opqaueBlocks[i + 1][j - 1][k], opqaueBlocks[i + 1][j - 1][k + 1], opqaueBlocks[i + 1][j][k + 1]);
                data.lightmapBlocklightEastHYHZ = averageLightLevels(blockLightLevels[i + 1][j][k], blockLightLevels[i + 1][j + 1][k], blockLightLevels[i + 1][j + 1][k + 1], blockLightLevels[i + 1][j][k + 1]);
                data.lightmapSkylightEastHYHZ = averageLightLevels(skyLightLevels[i + 1][j][k], skyLightLevels[i + 1][j + 1][k], skyLightLevels[i + 1][j + 1][k + 1], skyLightLevels[i + 1][j][k + 1]);
                data.AOEastHYHZ = AOMode(opqaueBlocks[i + 1][j + 1][k], opqaueBlocks[i + 1][j + 1][k + 1], opqaueBlocks[i + 1][j][k + 1]);
            }
            
            if (!(data.hideBottom = opqaueBlocks[i][j - 1][k])) {
                data.lightmapBlocklightBottomLXLZ = averageLightLevels(blockLightLevels[i][j - 1][k], blockLightLevels[i - 1][j - 1][k], blockLightLevels[i - 1][j - 1][k - 1], blockLightLevels[i][j - 1][k - 1]);
                data.lightmapSkylightBottomLXLZ = averageLightLevels(skyLightLevels[i][j - 1][k], skyLightLevels[i - 1][j - 1][k], skyLightLevels[i - 1][j - 1][k - 1], skyLightLevels[i][j - 1][k - 1]);
                data.AOBottomLXLZ = AOMode(opqaueBlocks[i - 1][j - 1][k], opqaueBlocks[i - 1][j - 1][k - 1], opqaueBlocks[i][j - 1][k - 1]);
                data.lightmapBlocklightBottomHXLZ = averageLightLevels(blockLightLevels[i][j - 1][k], blockLightLevels[i + 1][j - 1][k], blockLightLevels[i + 1][j - 1][k - 1], blockLightLevels[i][j - 1][k - 1]);
                data.lightmapSkylightBottomHXLZ = averageLightLevels(skyLightLevels[i][j - 1][k], skyLightLevels[i + 1][j - 1][k], skyLightLevels[i + 1][j - 1][k - 1], skyLightLevels[i][j - 1][k - 1]);
                data.AOBottomHXLZ = AOMode(opqaueBlocks[i + 1][j - 1][k], opqaueBlocks[i + 1][j - 1][k - 1], opqaueBlocks[i][j - 1][k - 1]);
                data.lightmapBlocklightBottomLXHZ = averageLightLevels(blockLightLevels[i][j - 1][k], blockLightLevels[i - 1][j - 1][k], blockLightLevels[i - 1][j - 1][k + 1], blockLightLevels[i][j - 1][k + 1]);
                data.lightmapSkylightBottomLXHZ = averageLightLevels(skyLightLevels[i][j - 1][k], skyLightLevels[i - 1][j - 1][k], skyLightLevels[i - 1][j - 1][k + 1], skyLightLevels[i][j - 1][k + 1]);
                data.AOBottomLXHZ = AOMode(opqaueBlocks[i - 1][j - 1][k], opqaueBlocks[i - 1][j - 1][k + 1], opqaueBlocks[i][j - 1][k + 1]);
                data.lightmapBlocklightBottomHXHZ = averageLightLevels(blockLightLevels[i][j - 1][k], blockLightLevels[i + 1][j - 1][k], blockLightLevels[i + 1][j - 1][k + 1], blockLightLevels[i][j - 1][k + 1]);
                data.lightmapSkylightBottomHXHZ = averageLightLevels(skyLightLevels[i][j - 1][k], skyLightLevels[i + 1][j - 1][k], skyLightLevels[i + 1][j - 1][k + 1], skyLightLevels[i][j - 1][k + 1]);
                data.AOBottomHXHZ = AOMode(opqaueBlocks[i + 1][j - 1][k], opqaueBlocks[i + 1][j - 1][k + 1], opqaueBlocks[i][j - 1][k + 1]);
            }
            
            if (!(data.hideTop = opqaueBlocks[i][j + 1][k])) {
                data.lightmapBlocklightTopLXLZ = averageLightLevels(blockLightLevels[i][j + 1][k], blockLightLevels[i - 1][j + 1][k], blockLightLevels[i - 1][j + 1][k - 1], blockLightLevels[i][j + 1][k - 1]);
                data.lightmapSkylightTopLXLZ = averageLightLevels(skyLightLevels[i][j + 1][k], skyLightLevels[i - 1][j + 1][k], skyLightLevels[i - 1][j + 1][k - 1], skyLightLevels[i][j + 1][k - 1]);
                data.AOTopLXLZ = AOMode(opqaueBlocks[i - 1][j + 1][k], opqaueBlocks[i - 1][j + 1][k - 1], opqaueBlocks[i][j + 1][k - 1]);
                data.lightmapBlocklightTopHXLZ = averageLightLevels(blockLightLevels[i][j + 1][k], blockLightLevels[i + 1][j + 1][k], blockLightLevels[i + 1][j + 1][k - 1], blockLightLevels[i][j + 1][k - 1]);
                data.lightmapSkylightTopHXLZ = averageLightLevels(skyLightLevels[i][j + 1][k], skyLightLevels[i + 1][j + 1][k], skyLightLevels[i + 1][j + 1][k - 1], skyLightLevels[i][j + 1][k - 1]);
                data.AOTopHXLZ = AOMode(opqaueBlocks[i + 1][j + 1][k], opqaueBlocks[i + 1][j + 1][k - 1], opqaueBlocks[i][j + 1][k - 1]);
                data.lightmapBlocklightTopLXHZ = averageLightLevels(blockLightLevels[i][j + 1][k], blockLightLevels[i - 1][j + 1][k], blockLightLevels[i - 1][j + 1][k + 1], blockLightLevels[i][j + 1][k + 1]);
                data.lightmapSkylightTopLXHZ = averageLightLevels(skyLightLevels[i][j + 1][k], skyLightLevels[i - 1][j + 1][k], skyLightLevels[i - 1][j + 1][k + 1], skyLightLevels[i][j + 1][k + 1]);
                data.AOTopLXHZ = AOMode(opqaueBlocks[i - 1][j + 1][k], opqaueBlocks[i - 1][j + 1][k + 1], opqaueBlocks[i][j + 1][k + 1]);
                data.lightmapBlocklightTopHXHZ = averageLightLevels(blockLightLevels[i][j + 1][k], blockLightLevels[i + 1][j + 1][k], blockLightLevels[i + 1][j + 1][k + 1], blockLightLevels[i][j + 1][k + 1]);
                data.lightmapSkylightTopHXHZ = averageLightLevels(skyLightLevels[i][j + 1][k], skyLightLevels[i + 1][j + 1][k], skyLightLevels[i + 1][j + 1][k + 1], skyLightLevels[i][j + 1][k + 1]);
                data.AOTopHXHZ = AOMode(opqaueBlocks[i + 1][j + 1][k], opqaueBlocks[i + 1][j + 1][k + 1], opqaueBlocks[i][j + 1][k + 1]);
            }
            
            if (!(data.hideNorth = opqaueBlocks[i][j][k - 1])) {
                data.lightmapBlocklightNorthLXLY = averageLightLevels(blockLightLevels[i][j][k - 1], blockLightLevels[i - 1][j][k - 1], blockLightLevels[i - 1][j - 1][k - 1], blockLightLevels[i][j - 1][k - 1]);
                data.lightmapSkylightNorthLXLY = averageLightLevels(skyLightLevels[i][j][k - 1], skyLightLevels[i - 1][j][k - 1], skyLightLevels[i - 1][j - 1][k - 1], skyLightLevels[i][j - 1][k - 1]);
                data.AONorthLXLY = AOMode(opqaueBlocks[i - 1][j][k - 1], opqaueBlocks[i - 1][j - 1][k - 1], opqaueBlocks[i][j - 1][k - 1]);
                data.lightmapBlocklightNorthHXLY = averageLightLevels(blockLightLevels[i][j][k - 1], blockLightLevels[i + 1][j][k - 1], blockLightLevels[i + 1][j - 1][k - 1], blockLightLevels[i][j - 1][k - 1]);
                data.lightmapSkylightNorthHXLY = averageLightLevels(skyLightLevels[i][j][k - 1], skyLightLevels[i + 1][j][k - 1], skyLightLevels[i + 1][j - 1][k - 1], skyLightLevels[i][j - 1][k - 1]);
                data.AONorthHXLY = AOMode(opqaueBlocks[i + 1][j][k - 1], opqaueBlocks[i + 1][j - 1][k - 1], opqaueBlocks[i][j - 1][k - 1]);
                data.lightmapBlocklightNorthLXHY = averageLightLevels(blockLightLevels[i][j][k - 1], blockLightLevels[i - 1][j][k - 1], blockLightLevels[i - 1][j + 1][k - 1], blockLightLevels[i][j + 1][k - 1]);
                data.lightmapSkylightNorthLXHY = averageLightLevels(skyLightLevels[i][j][k - 1], skyLightLevels[i - 1][j][k - 1], skyLightLevels[i - 1][j + 1][k - 1], skyLightLevels[i][j + 1][k - 1]);
                data.AONorthLXHY = AOMode(opqaueBlocks[i - 1][j][k - 1], opqaueBlocks[i - 1][j + 1][k - 1], opqaueBlocks[i][j + 1][k - 1]);
                data.lightmapBlocklightNorthHXHY = averageLightLevels(blockLightLevels[i][j][k - 1], blockLightLevels[i + 1][j][k - 1], blockLightLevels[i + 1][j + 1][k - 1], blockLightLevels[i][j + 1][k - 1]);
                data.lightmapSkylightNorthHXHY = averageLightLevels(skyLightLevels[i][j][k - 1], skyLightLevels[i + 1][j][k - 1], skyLightLevels[i + 1][j + 1][k - 1], skyLightLevels[i][j + 1][k - 1]);
                data.AONorthHXHY = AOMode(opqaueBlocks[i + 1][j][k - 1], opqaueBlocks[i + 1][j + 1][k - 1], opqaueBlocks[i][j + 1][k - 1]);
            }
            
            if (!(data.hideSouth = opqaueBlocks[i][j][k + 1])) {
                data.lightmapBlocklightSouthLXLY = averageLightLevels(blockLightLevels[i][j][k + 1], blockLightLevels[i - 1][j][k + 1], blockLightLevels[i - 1][j - 1][k + 1], blockLightLevels[i][j - 1][k + 1]);
                data.lightmapSkylightSouthLXLY = averageLightLevels(skyLightLevels[i][j][k + 1], skyLightLevels[i - 1][j][k + 1], skyLightLevels[i - 1][j - 1][k + 1], skyLightLevels[i][j - 1][k + 1]);
                data.AOSouthLXLY = AOMode(opqaueBlocks[i - 1][j][k + 1], opqaueBlocks[i - 1][j - 1][k + 1], opqaueBlocks[i][j - 1][k + 1]);
                data.lightmapBlocklightSouthHXLY = averageLightLevels(blockLightLevels[i][j][k + 1], blockLightLevels[i + 1][j][k + 1], blockLightLevels[i + 1][j - 1][k + 1], blockLightLevels[i][j - 1][k + 1]);
                data.lightmapSkylightSouthHXLY = averageLightLevels(skyLightLevels[i][j][k + 1], skyLightLevels[i + 1][j][k + 1], skyLightLevels[i + 1][j - 1][k + 1], skyLightLevels[i][j - 1][k + 1]);
                data.AOSouthHXLY = AOMode(opqaueBlocks[i + 1][j][k + 1], opqaueBlocks[i + 1][j - 1][k + 1], opqaueBlocks[i][j - 1][k + 1]);
                data.lightmapBlocklightSouthLXHY = averageLightLevels(blockLightLevels[i][j][k + 1], blockLightLevels[i - 1][j][k + 1], blockLightLevels[i - 1][j + 1][k + 1], blockLightLevels[i][j + 1][k + 1]);
                data.lightmapSkylightSouthLXHY = averageLightLevels(skyLightLevels[i][j][k + 1], skyLightLevels[i - 1][j][k + 1], skyLightLevels[i - 1][j + 1][k + 1], skyLightLevels[i][j + 1][k + 1]);
                data.AOSouthLXHY = AOMode(opqaueBlocks[i - 1][j][k + 1], opqaueBlocks[i - 1][j + 1][k + 1], opqaueBlocks[i][j + 1][k + 1]);
                data.lightmapBlocklightSouthHXHY = averageLightLevels(blockLightLevels[i][j][k + 1], blockLightLevels[i + 1][j][k + 1], blockLightLevels[i + 1][j + 1][k + 1], blockLightLevels[i][j + 1][k + 1]);
                data.lightmapSkylightSouthHXHY = averageLightLevels(skyLightLevels[i][j][k + 1], skyLightLevels[i + 1][j][k + 1], skyLightLevels[i + 1][j + 1][k + 1], skyLightLevels[i][j + 1][k + 1]);
                data.AOSouthHXHY = AOMode(opqaueBlocks[i + 1][j][k + 1], opqaueBlocks[i + 1][j + 1][k + 1], opqaueBlocks[i][j + 1][k + 1]);
            }
        }
        position.add(basePosition);
        return data;
    }
}
