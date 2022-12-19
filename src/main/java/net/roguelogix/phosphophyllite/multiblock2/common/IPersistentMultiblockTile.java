package net.roguelogix.phosphophyllite.multiblock2.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.roguelogix.phosphophyllite.modular.api.IModularTile;
import net.roguelogix.phosphophyllite.modular.api.ModuleRegistry;
import net.roguelogix.phosphophyllite.modular.api.TileModule;
import net.roguelogix.phosphophyllite.multiblock2.MultiblockController;
import net.roguelogix.phosphophyllite.multiblock2.MultiblockTileModule;
import net.roguelogix.phosphophyllite.multiblock2.modular.ICoreMultiblockTileModule;
import net.roguelogix.phosphophyllite.multiblock2.rectangular.IRectangularMultiblockBlock;
import net.roguelogix.phosphophyllite.multiblock2.validated.IValidatedMultiblock;
import net.roguelogix.phosphophyllite.multiblock2.validated.IValidatedMultiblockTile;
import net.roguelogix.phosphophyllite.registry.OnModLoad;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

import javax.annotation.Nullable;

@NonnullDefault
public interface IPersistentMultiblockTile<
        TileType extends BlockEntity & IPersistentMultiblockTile<TileType, BlockType, ControllerType>,
        BlockType extends Block & IRectangularMultiblockBlock,
        ControllerType extends MultiblockController<TileType, BlockType, ControllerType> & IPersistentMultiblock<TileType, BlockType, ControllerType>
        > extends IValidatedMultiblockTile<TileType, BlockType, ControllerType> {
    
    final class Module<
            TileType extends BlockEntity & IPersistentMultiblockTile<TileType, BlockType, ControllerType>,
            BlockType extends Block & IRectangularMultiblockBlock,
            ControllerType extends MultiblockController<TileType, BlockType, ControllerType> & IPersistentMultiblock<TileType, BlockType, ControllerType>
            > extends TileModule<TileType> implements ICoreMultiblockTileModule<TileType, BlockType, ControllerType> {
        
        @Nullable
        CompoundTag nbt;
        IValidatedMultiblock.AssemblyState lastAssemblyState = IValidatedMultiblock.AssemblyState.DISASSEMBLED;
        int expectedBlocks = 0;
        
        MultiblockTileModule<TileType, BlockType, ControllerType> multiblockModule;
        @Nullable
        IPersistentMultiblock.Module<TileType, BlockType, ControllerType> controllerPersistentModule;
        
        @OnModLoad
        public static void register() {
            ModuleRegistry.registerTileModule(IPersistentMultiblockTile.class, Module::new);
        }
        
        public Module(IModularTile iface) {
            super(iface);
            //noinspection ConstantConditions
            multiblockModule = null;
        }
        
        @Override
        public void postModuleConstruction() {
            multiblockModule = iface.multiblockModule();
        }
        
        @Override
        public String saveKey() {
            return "persistent_multiblock";
        }
        
        @Override
        public void readNBT(CompoundTag nbt) {
            // backwards compat with opening older worlds
            // TODO: require this
            if (nbt.contains("last_assembly_state")) {
                lastAssemblyState = IValidatedMultiblock.AssemblyState.valueOf(nbt.getString("last_assembly_state"));
            }
            if (nbt.contains("expected_blocks")) {
                expectedBlocks = nbt.getInt("expected_blocks");
            }
            if (nbt.contains("controller_data")) {
                this.nbt = nbt.getCompound("controller_data");
                return;
            }
            this.nbt = nbt;
        }
        
        @Override
        public CompoundTag writeNBT() {
            if (controllerPersistentModule != null && controllerPersistentModule.isSaveDelegate(iface) && nbt == null) {
                nbt = controllerPersistentModule.getNBT();
            }
            final var toReturn = new CompoundTag();
            toReturn.putString("last_assembly_state", lastAssemblyState.toString());
            if (nbt != null) {
                toReturn.put("controller_data", nbt);
            }
            return toReturn;
        }
        
        @Override
        public void aboutToUnloadDetach() {
            nbt = null;
            writeNBT();
        }
        
        @Override
        public void onControllerChange() {
            final var controller = multiblockModule.controller();
            if (controller == null) {
                controllerPersistentModule = null;
                return;
            }
            controllerPersistentModule = controller.persistentModule();
        }
    }
}
