package net.roguelogix.phosphophyllite.multiblock2.common;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.roguelogix.phosphophyllite.multiblock2.IMultiblockBlock;
import net.roguelogix.phosphophyllite.multiblock2.IMultiblockTile;
import net.roguelogix.phosphophyllite.multiblock2.MultiblockController;
import net.roguelogix.phosphophyllite.multiblock2.modular.IModularMultiblockController;
import net.roguelogix.phosphophyllite.multiblock2.modular.MultiblockControllerModule;
import net.roguelogix.phosphophyllite.multiblock2.modular.MultiblockControllerModuleRegistry;
import net.roguelogix.phosphophyllite.multiblock2.validated.IValidatedMultiblock;
import net.roguelogix.phosphophyllite.multiblock2.validated.IValidatedMultiblockControllerModule;
import net.roguelogix.phosphophyllite.registry.OnModLoad;
import net.roguelogix.phosphophyllite.util.FastArraySet;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

import javax.annotation.Nonnull;

@NonnullDefault
public interface IEventMultiblock<
        TileType extends BlockEntity & IMultiblockTile<TileType, BlockType, ControllerType>,
        BlockType extends Block & IMultiblockBlock,
        ControllerType extends MultiblockController<TileType, BlockType, ControllerType> & IEventMultiblock<TileType, BlockType, ControllerType>
        > extends IModularMultiblockController<TileType, BlockType, ControllerType> {
    
    // TODO: more multiblock events, there are more of them
    // TODO: find a way to make this generic, or integrated into the modules better
    interface AssemblyStateTransition {
        void onAssemblyStateTransition(IValidatedMultiblock.AssemblyState oldState, IValidatedMultiblock.AssemblyState newState);
        
        interface OnAssembly extends AssemblyStateTransition {
            @Override
            default void onAssemblyStateTransition(IValidatedMultiblock.AssemblyState oldState, IValidatedMultiblock.AssemblyState newState) {
                if (newState == IValidatedMultiblock.AssemblyState.ASSEMBLED) {
                    onAssembly();
                }
            }
            
            void onAssembly();
        }
        
        interface OnDisassembly extends AssemblyStateTransition {
            @Override
            default void onAssemblyStateTransition(IValidatedMultiblock.AssemblyState oldState, IValidatedMultiblock.AssemblyState newState) {
                if (newState == IValidatedMultiblock.AssemblyState.DISASSEMBLED) {
                    onDisassembly();
                }
            }
            
            void onDisassembly();
        }
    }
    
    final class Module<
            TileType extends BlockEntity & IMultiblockTile<TileType, BlockType, ControllerType>,
            BlockType extends Block & IMultiblockBlock,
            ControllerType extends MultiblockController<TileType, BlockType, ControllerType> & IEventMultiblock<TileType, BlockType, ControllerType>
            > extends MultiblockControllerModule<TileType, BlockType, ControllerType> implements IValidatedMultiblockControllerModule {
        
        FastArraySet<AssemblyStateTransition> assemblyStateTransitionTiles = new FastArraySet<>();
        
        @OnModLoad
        public static void register() {
            MultiblockControllerModuleRegistry.registerModule(IEventMultiblock.class, Module::new);
        }
        
        public Module(IModularMultiblockController<TileType, BlockType, ControllerType> controller) {
            super(controller);
        }
        
        @Override
        public void onPartAdded(@Nonnull TileType tile) {
            if (tile instanceof AssemblyStateTransition iface) {
                assemblyStateTransitionTiles.add(iface);
            }
        }
        
        @Override
        public void onPartRemoved(@Nonnull TileType tile) {
            if (tile instanceof AssemblyStateTransition iface) {
                assemblyStateTransitionTiles.remove(iface);
            }
        }
    
        @Override
        public void onStateTransition(IValidatedMultiblock.AssemblyState oldAssemblyState, IValidatedMultiblock.AssemblyState newAssemblyState) {
            for (final var element : assemblyStateTransitionTiles.elements()) {
                element.onAssemblyStateTransition(oldAssemblyState, newAssemblyState);
            }
        }
    }
}
