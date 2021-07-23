package net.roguelogix.phosphophyllite.util;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3ic;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TileMap<TileType extends BlockEntity> {
    private Vector3i scratchVector = new Vector3i();
    
    private final LinkedHashMap<Vector3ic, BlockEntity[][][]> internalMap = new LinkedHashMap<>();
    private int size = 0;
    
    public boolean addTile(TileType tile) {
        BlockPos tilePos = tile.getBlockPos();
        scratchVector.set(tilePos.getX() >> 4, tilePos.getY() >> 4, tilePos.getZ() >> 4);
        BlockEntity[][][] sectionArray = internalMap.computeIfAbsent(scratchVector, k -> {
            // the previous scratch vector is now the key, no longer allowed to edit it, so, new one plx
            scratchVector = new Vector3i(k);
            return new BlockEntity[16][][];
        });
        BlockEntity[][] XsubSection = sectionArray[tilePos.getX() & 15];
        if (XsubSection == null) {
            XsubSection = new BlockEntity[16][];
            sectionArray[tilePos.getX() & 15] = XsubSection;
        }
        BlockEntity[] XYsubSection = XsubSection[tilePos.getY() & 15];
        if (XYsubSection == null) {
            XYsubSection = new BlockEntity[16];
            XsubSection[tilePos.getY() & 15] = XYsubSection;
        }
        
        BlockEntity prevVal = XYsubSection[tilePos.getZ() & 15];
        XYsubSection[tilePos.getZ() & 15] = tile;
        if (prevVal == null) {
            size++;
            return true;
        }
        return false;
    }
    
    public void addAll(TileMap<TileType> otherMap) {
        otherMap.forEachTile(this::addTile);
    }
    
    public boolean removeTile(TileType tile) {
        BlockPos tilePos = tile.getBlockPos();
        scratchVector.set(tilePos.getX() >> 4, tilePos.getY() >> 4, tilePos.getZ() >> 4);
        BlockEntity[][][] sectionArray = internalMap.get(scratchVector);
        if (sectionArray == null) {
            return false;
        }
        BlockEntity[][] XsubSection = sectionArray[tilePos.getX() & 15];
        if (XsubSection == null) {
            return false;
        }
        BlockEntity[] XYsubSection = XsubSection[tilePos.getY() & 15];
        if (XYsubSection == null) {
            return false;
        }
        
        BlockEntity prevVal = XYsubSection[tilePos.getZ() & 15];
        XYsubSection[tilePos.getZ() & 15] = null;
        if (prevVal != null) {
            size--;
        }
        
        for (BlockEntity tileEntity : XYsubSection) {
            if (tileEntity != null) {
                return prevVal != null;
            }
        }
        XsubSection[tilePos.getY() & 15] = null;
        
        for (BlockEntity[] tileEntities : XsubSection) {
            if (tileEntities != null) {
                return prevVal != null;
            }
        }
        sectionArray[tilePos.getX() & 15] = null;
        
        for (BlockEntity[][] tileEntities : sectionArray) {
            if (tileEntities != null) {
                return prevVal != null;
            }
        }
        internalMap.remove(scratchVector);
        
        return prevVal != null;
    }
    
    public boolean containsTile(TileType tile) {
        return containsPos(tile.getBlockPos());
    }
    
    public boolean containsPos(BlockPos pos) {
        return getTile(pos) != null;
    }
    
    public boolean containsPos(Vector3i pos) {
        return getTile(pos) != null;
    }
    
    @Nullable
    public TileType getTile(Vector3ic pos) {
        int x = pos.x(), y = pos.y(), z = pos.z();
        scratchVector.set(pos.x() >> 4, pos.y() >> 4, pos.z() >> 4);
        BlockEntity[][][] sectionArray = internalMap.get(scratchVector);
        // getTile(BlockPos) passes the scratch vector in, so, i cant assume that pos isn't the scratchvector
        if (sectionArray == null) {
            return null;
        }
        BlockEntity[][] XsubSection = sectionArray[x & 15];
        if (XsubSection == null) {
            return null;
        }
        BlockEntity[] XYsubSection = XsubSection[y & 15];
        if (XYsubSection == null) {
            return null;
        }
        
        //noinspection unchecked
        return (TileType) XYsubSection[z & 15];
    }
    
    @Nullable
    public TileType getTile(BlockPos pos) {
        scratchVector.set(pos.getX(), pos.getY(), pos.getZ());
        return getTile(scratchVector);
    }
    
    public void forEach(BiConsumer<BlockPos, TileType> consumer) {
        forEachTile((t) -> consumer.accept(t.getBlockPos(), t));
    }
    
    public void forEachTile(Consumer<TileType> consumer) {
        internalMap.forEach((vec, sectionMap) -> {
            for (int i = 0, sectionMapLength = sectionMap.length; i < sectionMapLength; i++) {
                BlockEntity[][] tileEntities = sectionMap[i];
                if (tileEntities != null) {
                    for (int j = 0, tileEntitiesLength = tileEntities.length; j < tileEntitiesLength; j++) {
                        BlockEntity[] tileEntity = tileEntities[j];
                        if (tileEntity != null) {
                            for (int k = 0, tileEntityLength = tileEntity.length; k < tileEntityLength; k++) {
                                BlockEntity entity = tileEntity[k];
                                if (entity != null) {
                                    //noinspection unchecked
                                    consumer.accept((TileType) entity);
                                }
                            }
                        }
                    }
                }
            }
        });
    }
    
    public void forEachPos(Consumer<BlockPos> consumer) {
        forEachTile((t) -> consumer.accept(t.getBlockPos()));
    }
    
    public boolean isEmpty() {
        return size == 0;
    }
    
    public int size() {
        return size;
    }
    
    @Nullable
    public TileType getOne() {
        if (isEmpty()) {
            return null;
        }
        Iterator<BlockEntity[][][]> iter = internalMap.values().iterator();
        if (!iter.hasNext()) {
            return null;
        }
        BlockEntity[][][] tiles = iter.next();
        if (tiles == null) {
            return null;
        }
        for (BlockEntity[][] tile2d : tiles) {
            if (tile2d == null) {
                continue;
            }
            for (BlockEntity[] tile1d : tile2d) {
                if(tile1d == null){
                    continue;
                }
                for (BlockEntity tileEntity : tile1d) {
                    if(tileEntity != null){
                        //noinspection unchecked
                        return (TileType) tileEntity;
                    }
                }
            }
        }
        return null;
    }
}
