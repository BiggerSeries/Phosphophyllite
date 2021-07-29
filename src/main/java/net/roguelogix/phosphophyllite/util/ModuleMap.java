package net.roguelogix.phosphophyllite.util;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.roguelogix.phosphophyllite.modular.api.IModularTile;
import net.roguelogix.phosphophyllite.modular.api.TileModule;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3ic;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ModuleMap<ModuleType extends TileModule<TileType>, TileType extends BlockEntity & IModularTile> {
    private Vector3i scratchVector = new Vector3i();
    
    private final LinkedHashMap<Vector3ic, TileModule<?>[][][]> internalMap = new LinkedHashMap<>();
    private int size = 0;
    
    public boolean addModule(ModuleType module) {
        BlockPos tilePos = module.iface.getBlockPos();
        scratchVector.set(tilePos.getX() >> 4, tilePos.getY() >> 4, tilePos.getZ() >> 4);
        var sectionArray = internalMap.computeIfAbsent(scratchVector, k -> {
            // the previous scratch vector is now the key, no longer allowed to edit it, so, new one plx
            scratchVector = new Vector3i(k);
            return new TileModule<?>[16][][];
        });
        var XsubSection = sectionArray[tilePos.getX() & 15];
        if (XsubSection == null) {
            XsubSection = new TileModule<?>[16][];
            sectionArray[tilePos.getX() & 15] = XsubSection;
        }
        var XYsubSection = XsubSection[tilePos.getY() & 15];
        if (XYsubSection == null) {
            XYsubSection = new TileModule<?>[16];
            XsubSection[tilePos.getY() & 15] = XYsubSection;
        }
        
        var prevVal = XYsubSection[tilePos.getZ() & 15];
        XYsubSection[tilePos.getZ() & 15] = module;
        if (prevVal == null) {
            size++;
            return true;
        }
        return false;
    }
    
    public void addAll(ModuleMap<ModuleType, TileType> otherMap) {
        otherMap.forEachModule(this::addModule);
    }
    
    public boolean removeModule(ModuleType module) {
        BlockPos tilePos = module.iface.getBlockPos();
        scratchVector.set(tilePos.getX() >> 4, tilePos.getY() >> 4, tilePos.getZ() >> 4);
        var sectionArray = internalMap.get(scratchVector);
        if (sectionArray == null) {
            return false;
        }
        var XsubSection = sectionArray[tilePos.getX() & 15];
        if (XsubSection == null) {
            return false;
        }
        var XYsubSection = XsubSection[tilePos.getY() & 15];
        if (XYsubSection == null) {
            return false;
        }
        
        var prevVal = XYsubSection[tilePos.getZ() & 15];
        if (prevVal != module) {
            return false;
        }
        XYsubSection[tilePos.getZ() & 15] = null;
        size--;
        
        for (var tileEntity : XYsubSection) {
            if (tileEntity != null) {
                return true;
            }
        }
        XsubSection[tilePos.getY() & 15] = null;
        
        for (var tileEntities : XsubSection) {
            if (tileEntities != null) {
                return true;
            }
        }
        sectionArray[tilePos.getX() & 15] = null;
        
        for (var tileEntities : sectionArray) {
            if (tileEntities != null) {
                return true;
            }
        }
        internalMap.remove(scratchVector);
        
        return true;
    }
    
    
    public boolean containsTile(TileType tile) {
        return containsPos(tile.getBlockPos());
    }
    
    public boolean containsModule(ModuleType module) {
        return containsPos(module.iface.getBlockPos());
    }
    
    public boolean containsPos(BlockPos pos) {
        return getModule(pos) != null;
    }
    
    public boolean containsPos(Vector3i pos) {
        return getModule(pos) != null;
    }
    
    @Nullable
    public ModuleType getModule(Vector3ic pos) {
        int x = pos.x(), y = pos.y(), z = pos.z();
        scratchVector.set(pos.x() >> 4, pos.y() >> 4, pos.z() >> 4);
        TileModule<?>[][][] sectionArray = internalMap.get(scratchVector);
        // getModule(BlockPos) passes the scratch vector in, so, i cant assume that pos isn't the scratchvector
        if (sectionArray == null) {
            return null;
        }
        TileModule<?>[][] XsubSection = sectionArray[x & 15];
        if (XsubSection == null) {
            return null;
        }
        TileModule<?>[] XYsubSection = XsubSection[y & 15];
        if (XYsubSection == null) {
            return null;
        }
        
        //noinspection unchecked
        return (ModuleType) XYsubSection[z & 15];
    }
    
    @Nullable
    public ModuleType getModule(BlockPos pos) {
        scratchVector.set(pos.getX(), pos.getY(), pos.getZ());
        return getModule(scratchVector);
    }
    
    @Nullable
    public TileType getTile(Vector3ic pos){
        var module = getModule(pos);
        if(module == null) {
            return null;
        }
        return module.iface;
    }
    
    @Nullable
    public TileType getTile(BlockPos pos) {
        scratchVector.set(pos.getX(), pos.getY(), pos.getZ());
        return getTile(scratchVector);
    }
    
    public void forEachPosAndModule(BiConsumer<BlockPos, ModuleType> consumer) {
        forEachModule((module) -> consumer.accept(module.iface.getBlockPos(), module));
    }
    
    public void forEachPosAndTile(BiConsumer<BlockPos, TileType> consumer) {
        forEachTile((tile) -> consumer.accept(tile.getBlockPos(), tile));
    }
    
    public void forEachTile(Consumer<TileType> consumer) {
        internalMap.forEach((vec, sectionMap) -> {
            for (int i = 0, sectionMapLength = sectionMap.length; i < sectionMapLength; i++) {
                var tileEntities = sectionMap[i];
                if (tileEntities != null) {
                    for (int j = 0, tileEntitiesLength = tileEntities.length; j < tileEntitiesLength; j++) {
                        var tileEntity = tileEntities[j];
                        if (tileEntity != null) {
                            for (int k = 0, tileEntityLength = tileEntity.length; k < tileEntityLength; k++) {
                                var entity = tileEntity[k];
                                if (entity != null) {
                                    consumer.accept((TileType) entity.iface);
                                }
                            }
                        }
                    }
                }
            }
        });
    }
    
    public void forEachModule(Consumer<ModuleType> consumer) {
        internalMap.forEach((vec, sectionMap) -> {
            for (int i = 0, sectionMapLength = sectionMap.length; i < sectionMapLength; i++) {
                var tileEntities = sectionMap[i];
                if (tileEntities != null) {
                    for (int j = 0, tileEntitiesLength = tileEntities.length; j < tileEntitiesLength; j++) {
                        var tileEntity = tileEntities[j];
                        if (tileEntity != null) {
                            for (int k = 0, tileEntityLength = tileEntity.length; k < tileEntityLength; k++) {
                                var entity = tileEntity[k];
                                if (entity != null) {
                                    //noinspection unchecked
                                    consumer.accept((ModuleType) entity);
                                }
                            }
                        }
                    }
                }
            }
        });
    }
    
    public void forEachPos(Consumer<BlockPos> consumer) {
        forEachModule((module) -> consumer.accept(module.iface.getBlockPos()));
    }
    
    public boolean isEmpty() {
        return size == 0;
    }
    
    public int size() {
        return size;
    }
    
    @Nullable
    public ModuleType getOne() {
        if (isEmpty()) {
            return null;
        }
        var iter = internalMap.values().iterator();
        if (!iter.hasNext()) {
            return null;
        }
        var tiles = iter.next();
        if (tiles == null) {
            return null;
        }
        for (var tile2d : tiles) {
            if (tile2d == null) {
                continue;
            }
            for (var tile1d : tile2d) {
                if (tile1d == null) {
                    continue;
                }
                for (var tileEntity : tile1d) {
                    if (tileEntity != null) {
                        //noinspection unchecked
                        return (ModuleType) tileEntity;
                    }
                }
            }
        }
        return null;
    }
}
