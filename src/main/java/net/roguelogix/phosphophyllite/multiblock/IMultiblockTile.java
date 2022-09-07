package net.roguelogix.phosphophyllite.multiblock;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.roguelogix.phosphophyllite.modular.api.IModularTile;
import net.roguelogix.phosphophyllite.modular.tile.IIsTickingTracker;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@Deprecated(forRemoval = true)
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface IMultiblockTile<
        TileType extends BlockEntity & IMultiblockTile<TileType, ControllerType>,
        ControllerType extends MultiblockController<TileType, ControllerType>
        > extends IModularTile, IIsTickingTracker.Tile {
    
    ControllerType createController();
    
    default ControllerType controller() {
        return multiblockModule().controller();
    }
    
    @Nullable
    default ControllerType nullableController() {
        return multiblockModule().controller();
    }
    
    default MultiblockTileModule<TileType, ControllerType> multiblockModule() {
        //noinspection unchecked
        return (MultiblockTileModule<TileType, ControllerType>) module(IMultiblockTile.class);
    }
    
    default MultiblockTileModule<TileType, ControllerType> createMultiblockModule() {
        return new MultiblockTileModule<>(as(IModularTile.class));
    }
}
