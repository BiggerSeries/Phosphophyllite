package net.roguelogix.phosphophyllite.multiblock2.common;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.roguelogix.phosphophyllite.multiblock2.IMultiblockBlock;
import net.roguelogix.phosphophyllite.multiblock2.IMultiblockTile;
import net.roguelogix.phosphophyllite.multiblock2.MultiblockController;
import net.roguelogix.phosphophyllite.multiblock2.modular.IModularMultiblockController;
import net.roguelogix.phosphophyllite.multiblock2.modular.MultiblockControllerModule;
import net.roguelogix.phosphophyllite.multiblock2.modular.MultiblockControllerModuleRegistry;
import net.roguelogix.phosphophyllite.registry.OnModLoad;
import net.roguelogix.phosphophyllite.util.FastArraySet;

import javax.annotation.Nonnull;

public interface ITickablePartsMultiblock<
        TileType extends BlockEntity & IMultiblockTile<TileType, BlockType, ControllerType>,
        BlockType extends Block & IMultiblockBlock,
        ControllerType extends MultiblockController<TileType, BlockType, ControllerType> & ITickablePartsMultiblock<TileType, BlockType, ControllerType>
        > extends IModularMultiblockController<TileType, BlockType, ControllerType> {
    
    
    /**
     * any Tile implementing this will be ticked
     */
    interface Tickable {
        void preTick();
        
        void postTick();
    }
    
    final class Module<
            TileType extends BlockEntity & IMultiblockTile<TileType, BlockType, ControllerType>,
            BlockType extends Block & IMultiblockBlock,
            ControllerType extends MultiblockController<TileType, BlockType, ControllerType> & ITickablePartsMultiblock<TileType, BlockType, ControllerType>
            > extends MultiblockControllerModule<TileType, BlockType, ControllerType> {
        
        private final FastArraySet<Tickable> tickables = new FastArraySet<>();
        
        @OnModLoad
        public static void register() {
            MultiblockControllerModuleRegistry.registerModule(ITickablePartsMultiblock.class, Module::new);
        }
        
        public Module(IModularMultiblockController<TileType, BlockType, ControllerType> controller) {
            super(controller);
        }
        
        @Override
        public void onPartAdded(@Nonnull TileType tile) {
            if (tile instanceof Tickable tickable) {
                tickables.add(tickable);
            }
        }
        
        @Override
        public void onPartRemoved(@Nonnull TileType tile) {
            if (tile instanceof Tickable tickable) {
                tickables.remove(tickable);
            }
        }
        
        @Override
        public void preTick() {
            tickables.elements().forEach(Tickable::preTick);
        }
        
        @Override
        public void postTick() {
            tickables.elements().forEach(Tickable::postTick);
        }
    }
}
