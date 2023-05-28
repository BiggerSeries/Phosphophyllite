package net.roguelogix.phosphophyllite.multiblock2.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.roguelogix.phosphophyllite.debug.DebugInfo;
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
        private boolean hasSaveDelegate = false;
        @Nullable
        private IPersistentMultiblockTile.Module<TileType, BlockType, ControllerType> saveDelegateModule;
        @Nullable
        private CompoundTag nbt;
        private boolean shouldReadNBT = false;
        private int expectedBlocks = 0;
        @Nullable
        private IValidatedMultiblock.AssemblyState lastAssemblyState;
        
        @OnModLoad
        public static void register() {
            MultiblockControllerModuleRegistry.registerModule(IPersistentMultiblock.class, Module::new);
        }
        
        public Module(IPersistentMultiblock<TileType, BlockType, ControllerType> controller) {
            super(controller);
        }
        
        private void partAdded(TileType newPart) {
            final var persistentModule = newPart.module(IPersistentMultiblockTile.class, IPersistentMultiblockTile.Module.class);
            assert persistentModule != null;
            if (expectedBlocks == 0) {
                expectedBlocks = persistentModule.expectedBlocks;
            }
            if (persistentModule.expectedBlocks != 0 && persistentModule.expectedBlocks != expectedBlocks) {
                // merging controllers, reset to zero
                expectedBlocks = 0;
            }
            if (lastAssemblyState == null) {
                lastAssemblyState = persistentModule.lastAssemblyState;
            }
            
            final var newNBT = persistentModule.controllerNBT;
            persistentModule.controllerNBT = null;
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
            shouldReadNBT = true;
        }
        
        private void partRemoved(TileType oldPart) {
            if (oldPart == saveDelegate) {
                saveDelegate = null;
                saveDelegateModule = null;
            }
        }
        
        @Override
        public void merge(ControllerType other) {
            final var otherPersistentModule = other.module(IPersistentMultiblock.class, Module.class);
            assert otherPersistentModule != null;
            if (otherPersistentModule.nbt != null) {
                if (controller.assemblyState() != AssemblyState.DISASSEMBLED) {
                    // we are currently assembled, this will probably change, but this is on the implementation to handle
                    return;
                }
                if (nbt == null) {
                    nbt = otherPersistentModule.nbt;
                    shouldReadNBT = true;
                    return;
                }
                nbt = controller.mergeNBTs(nbt, otherPersistentModule.nbt);
                shouldReadNBT = true;
            }
        }
        
        public void onPartLoaded(TileType tile) {
            partAdded(tile);
        }
        
        public void onPartUnloaded(TileType tile) {
            partRemoved(tile);
        }
        
        public void onPartAttached(TileType tile) {
            partAdded(tile);
        }
        
        public void onPartDetached(TileType tile) {
            partRemoved(tile);
        }
        
        public void onPartPlaced(TileType tile) {
            // cannot have nbt when first placed
            expectedBlocks = 0;
        }
        
        public void onPartBroken(TileType tile) {
            expectedBlocks = 0;
            partRemoved(tile);
            if (tile == saveDelegate) {
                hasSaveDelegate = false;
            }
        }
        
        private void pickDelegate() {
            if (hasSaveDelegate) {
                return;
            }
            saveDelegate = controller.randomTile();
            //noinspection unchecked
            saveDelegateModule = saveDelegate.module(IPersistentMultiblockTile.class, IPersistentMultiblockTile.Module.class);
            if (saveDelegateModule != null) {
                saveDelegateModule.controllerNBT = null;
            }
            hasSaveDelegate = true;
        }
        
        @Override
        public boolean canValidate() {
            return expectedBlocks <= controller.blocks.size();
        }
    
        @Override
        public boolean canTick() {
            return canValidate();
        }
        
        @Override
        public void onStateTransition(AssemblyState oldAssemblyState, AssemblyState newAssemblyState) {
            if (shouldReadNBT) {
                shouldReadNBT = false;
                if (nbt != null) {
                    controller.read(nbt);
                }
            }
        }
        
        void dirty() {
            pickDelegate();
            nbt = null;
            if (saveDelegateModule != null) {
                saveDelegateModule.controllerNBT = null;
            }
            Util.markRangeDirty(controller.level, controller.min(), controller.max());
        }
        
        boolean isSaveDelegate(TileType tile) {
            pickDelegate();
            return tile == saveDelegate;
        }
        
        @Nullable
        CompoundTag getNBT() {
            if (nbt == null) {
                nbt = controller.write();
            }
            return nbt;
        }
    
        @Nullable
        @Override
        public DebugInfo getDebugInfo() {
            final var debugInfo = new DebugInfo("PersistentMultiblock");
            return null;
        }
    }
}
