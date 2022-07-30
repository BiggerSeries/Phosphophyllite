package net.roguelogix.phosphophyllite.multiblock2.modular;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.modular.api.IModularTile;
import net.roguelogix.phosphophyllite.modular.api.TileModule;
import net.roguelogix.phosphophyllite.multiblock2.IMultiblockBlock;
import net.roguelogix.phosphophyllite.multiblock2.IMultiblockTile;
import net.roguelogix.phosphophyllite.multiblock2.MultiblockController;
import org.jetbrains.annotations.Contract;

public class ExtendedMultiblockTileModule<
        TileType extends BlockEntity & IMultiblockTile<TileType, BlockType, ControllerType>,
        BlockType extends Block & IMultiblockBlock,
        ControllerType extends MultiblockController<TileType, BlockType, ControllerType>
        > extends TileModule<TileType> {
    
    public ExtendedMultiblockTileModule(IModularTile iface) {
        super(iface);
    }
    
    @Contract(pure = true)
    public boolean shouldConnectTo(TileType tile, Direction direction) {
        return true;
    }
    
    public void aboutToAttemptAttach() {
    }
    
    @Contract(pure = true)
    public BlockState assembledBlockState(BlockState state) {
        return state;
    }
    
    @Contract(pure = true)
    public BlockState disassembledBlockState(BlockState state) {
        return state;
    }
    
    public void onControllerChange() {
    }
}
