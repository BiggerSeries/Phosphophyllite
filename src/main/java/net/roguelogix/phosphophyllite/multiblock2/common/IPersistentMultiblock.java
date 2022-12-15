package net.roguelogix.phosphophyllite.multiblock2.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.roguelogix.phosphophyllite.multiblock2.MultiblockController;
import net.roguelogix.phosphophyllite.multiblock2.modular.MultiblockControllerModule;
import net.roguelogix.phosphophyllite.multiblock2.modular.MultiblockControllerModuleRegistry;
import net.roguelogix.phosphophyllite.multiblock2.rectangular.IRectangularMultiblockBlock;
import net.roguelogix.phosphophyllite.multiblock2.validated.IValidatedMultiblock;
import net.roguelogix.phosphophyllite.multiblock2.validated.IValidatedMultiblockControllerModule;
import net.roguelogix.phosphophyllite.registry.OnModLoad;
import net.roguelogix.phosphophyllite.util.NonnullDefault;
import net.roguelogix.phosphophyllite.util.Util;

import javax.annotation.Nullable;

/**
 * TODO: this is currently basically just mimicking the current functionality of the old system
 *       should there be better support for splitting up a multiblock piece by piece?
 */
@NonnullDefault
public interface IPersistentMultiblock<
        TileType extends BlockEntity & IPersistentMultiblockTile<TileType, BlockType, ControllerType>,
        BlockType extends Block & IRectangularMultiblockBlock,
        ControllerType extends MultiblockController<TileType, BlockType, ControllerType> & IPersistentMultiblock<TileType, BlockType, ControllerType>
        > extends IValidatedMultiblock<TileType, BlockType, ControllerType> {
    
    CompoundTag mergeNBTs(CompoundTag nbtA, CompoundTag nbtB);
    
    void read(CompoundTag nbt);
    
    @Nullable
    CompoundTag write();
    
    default Module<TileType, BlockType, ControllerType> persistentModule() {
        //noinspection unchecked,ConstantConditions
        return module(IPersistentMultiblock.class, IPersistentMultiblock.Module.class);
    }
    
    default void dirty() {
        final var module = module(IPersistentMultiblock.class, Module.class);
        assert module != null;
        module.dirty();
    }
    
    final class Module<
            TileType extends BlockEntity & IPersistentMultiblockTile<TileType, BlockType, ControllerType>,
            BlockType extends Block & IRectangularMultiblockBlock,
            ControllerType extends MultiblockController<TileType, BlockType, ControllerType> & IPersistentMultiblock<TileType, BlockType, ControllerType>
            > extends MultiblockControllerModule<TileType, BlockType, ControllerType> implements IValidatedMultiblockControllerModule {
        @Nullable
        private TileType saveDelegate;
        @Nullable
        private IPersistentMultiblockTile.Module<TileType, BlockType, ControllerType> saveDelegateModule;
        @Nullable
        private CompoundTag nbt;
        
        @OnModLoad
        public static void register() {
            MultiblockControllerModuleRegistry.registerModule(IPersistentMultiblock.class, Module::new);
        }
        
        public Module(IPersistentMultiblock<TileType, BlockType, ControllerType> controller) {
            super(controller);
        }
        
        @Override
        public void postModuleConstruction() {
            controller.transitionToState(AssemblyState.PAUSED);
        }
        
        private void pausedToDisassembled() {
            if (controller.assemblyState() == AssemblyState.PAUSED) {
                controller.transitionToState(AssemblyState.DISASSEMBLED);
            }
        }
        
        private void partAdded(TileType newPart) {
            final var persistentModule = newPart.module(IPersistentMultiblockTile.class, IPersistentMultiblockTile.Module.class);
            assert persistentModule != null;
            final var newNBT = persistentModule.nbt;
            persistentModule.nbt = null;
            if (newNBT == null) {
                return;
            }
            if (saveDelegate == null) {
                saveDelegate = newPart;
                //noinspection unchecked
                saveDelegateModule = persistentModule;
            }
            if (nbt != null) {
                if (nbt.equals(newNBT)) {
                    return;
                }
                nbt = controller.mergeNBTs(nbt, newNBT);
            } else {
                nbt = newNBT;
            }
        }
        
        private void partRemoved(TileType oldPart) {
            if (oldPart == saveDelegate) {
                saveDelegate = null;
                saveDelegateModule = null;
            }
        }
        
        public void onPartLoaded(TileType tile) {
            partAdded(tile);
        }
        
        public void onPartUnloaded(TileType tile) {
            partRemoved(tile);
            if (controller.assemblyState() != AssemblyState.PAUSED) {
                controller.transitionToState(AssemblyState.PAUSED);
            }
        }
        
        public void onPartAttached(TileType tile) {
            partAdded(tile);
        }
        
        public void onPartDetached(TileType tile) {
            partRemoved(tile);
        }
        
        public void onPartPlaced(TileType tile) {
            pausedToDisassembled();
            // cannot have nbt when first placed
        }
        
        public void onPartBroken(TileType tile) {
            pausedToDisassembled();
            partRemoved(tile);
        }
        
        @Override
        public void onStateTransition(AssemblyState oldAssemblyState, AssemblyState newAssemblyState) {
            if (newAssemblyState != AssemblyState.ASSEMBLED) {
                return;
            }
            if (saveDelegate == null) {
                saveDelegate = controller.randomTile();
                //noinspection unchecked
                saveDelegateModule = saveDelegate.module(IPersistentMultiblockTile.class, IPersistentMultiblockTile.Module.class);
                if (saveDelegateModule != null) {
                    saveDelegateModule.nbt = null;
                }
                return;
            }
            assert saveDelegateModule != null;
            if (oldAssemblyState != AssemblyState.ASSEMBLED && nbt != null) {
                controller.read(nbt);
            }
        }
        
        void dirty() {
            nbt = null;
            if (saveDelegateModule != null) {
                saveDelegateModule.nbt = null;
            }
            Util.markRangeDirty(controller.level, controller.min(), controller.max());
        }
        
        boolean isSaveDelegate(TileType tile) {
            return tile == saveDelegate;
        }
        
        @Nullable
        CompoundTag getNBT() {
            if (nbt == null) {
                nbt = controller.write();
            }
            return nbt;
        }
    }
}
