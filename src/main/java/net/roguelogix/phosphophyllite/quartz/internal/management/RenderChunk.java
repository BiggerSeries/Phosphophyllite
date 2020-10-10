package net.roguelogix.phosphophyllite.quartz.internal.management;

import net.roguelogix.phosphophyllite.quartz.internal.BlockRenderInfo;
import net.roguelogix.phosphophyllite.quartz.internal.rendering.Renderer;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3ic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class RenderChunk {
    private final Vector3ic basePosition;
    private Map<Vector3i, BlockRenderInfo> blockPositions = new HashMap<>();
    
    RenderChunk(Vector3ic basePosition) {
        this.basePosition = new Vector3i(basePosition);
        updateRange(basePosition, basePosition.add(16, 16, 16, new Vector3i()));
    }
    
    void updateRange(Vector3ic posA, Vector3ic posB) {
        StateChunk stateChunk = WorldManagement.stateChunks.get(basePosition);
        
        if (stateChunk == null) {
            removeAll();
            return;
        }
        
        ArrayList<BlockRenderInfo> toUpdate = new ArrayList<>();
        
        // first i need to sync from state chunk to here
        // updates texture data too
        Vector3i pos = new Vector3i();
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                for (int k = 0; k < 16; k++) {
                    pos.set(i, j, k).add(basePosition);
                    StateCache.TextureInfo textureInfo = stateChunk.getTextureInfo(pos);
                    BlockRenderInfo blockRenderInfo = blockPositions.get(pos);
                    if (textureInfo != null) {
                        if (blockRenderInfo == null) {
                            blockRenderInfo = new BlockRenderInfo();
                            blockRenderInfo.x = pos.x;
                            blockRenderInfo.y = pos.y;
                            blockRenderInfo.z = pos.z;
                            blockPositions.put(new Vector3i(pos), blockRenderInfo);
                        }
                        blockRenderInfo.textureData(textureInfo);
                        if (blockRenderInfo.textureIDWest == -1 &&
                                blockRenderInfo.textureIDEast == -1 &&
                                blockRenderInfo.textureIDBottom == -1 &&
                                blockRenderInfo.textureIDTop == -1 &&
                                blockRenderInfo.textureIDNorth == -1 &&
                                blockRenderInfo.textureIDSouth == -1) {
                            toUpdate.add(blockRenderInfo);
                            blockPositions.remove(pos);
                        }
                    } else if (blockRenderInfo != null) {
                        blockRenderInfo.textureIDWest = -1;
                        blockRenderInfo.textureIDEast = -1;
                        blockRenderInfo.textureIDBottom = -1;
                        blockRenderInfo.textureIDTop = -1;
                        blockRenderInfo.textureIDNorth = -1;
                        blockRenderInfo.textureIDSouth = -1;
                        toUpdate.add(blockRenderInfo);
                        blockPositions.remove(pos);
                    }
                }
            }
        }
        
        // aaaannd update lighting info
        LightingChunk lightingChunk = WorldManagement.lightingChunks.get(basePosition);
        blockPositions.forEach((blockPosition, renderInfo) -> {
            if (blockPosition.x >= posA.x() && blockPosition.x <= posB.x() &&
                    blockPosition.y >= posA.y() && blockPosition.y <= posB.y() &&
                    blockPosition.z >= posA.z() && blockPosition.z <= posB.z()) {
                if (lightingChunk != null) {
                    renderInfo.lightingData(lightingChunk.getBlockData(blockPosition));
                }
                toUpdate.add(renderInfo);
            }
        });
        
        
        Renderer.INSTANCE.setBlockRenderInfo(toUpdate);
    }
    
    void removeAll() {
        ArrayList<BlockRenderInfo> toRemove = new ArrayList<>();
        for (BlockRenderInfo renderInfo : blockPositions.values()) {
            renderInfo.textureIDWest = -1;
            renderInfo.textureIDEast = -1;
            renderInfo.textureIDBottom = -1;
            renderInfo.textureIDTop = -1;
            renderInfo.textureIDNorth = -1;
            renderInfo.textureIDSouth = -1;
            toRemove.add(renderInfo);
        }
        blockPositions.clear();
        Renderer.INSTANCE.setBlockRenderInfo(toRemove);
    }
    
    // just in case, im doing this in finalize too
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        removeAll();
    }
}
