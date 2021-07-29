package net.roguelogix.phosphophyllite.multiblock.modular.rectangular;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.roguelogix.phosphophyllite.modular.api.IModularTile;
import net.roguelogix.phosphophyllite.multiblock.modular.IMultiblockTile;
import net.roguelogix.phosphophyllite.multiblock.modular.MultiblockTileModule;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface IRectangularMultiblockTile<
        TileType extends BlockEntity & IRectangularMultiblockTile<TileType, ControllerType>,
        ControllerType extends RectangularMultiblockController<TileType, ControllerType>
        > extends IMultiblockTile<TileType, ControllerType> {
    
    default RectangularMultiblockTileModule<TileType, ControllerType> rectangularMultiblockModule() {
        return (RectangularMultiblockTileModule<TileType, ControllerType>) multiblockModule();
    }
    
    @Override
    default MultiblockTileModule<TileType, ControllerType> createMultiblockModule() {
        return new RectangularMultiblockTileModule<>(as(IModularTile.class));
    }
}
