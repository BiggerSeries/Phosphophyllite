package net.roguelogix.phosphophyllite.tile;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.roguelogix.phosphophyllite.registry.OnModLoad;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface ISidedMultipart {
    
    ITileModule initialCoreModule();
    
    class SidedMultipartModule implements ITileModule {
        
        @OnModLoad
        static void onModLoad() {
            ModuleRegistry.registerModule(ISidedMultipart.class, SidedMultipartModule::new);
        }
        
        @Nonnull
        ITileModule coreModule;
        final ITileModule[] sidedModules = new ITileModule[6];
        
        final TileEntity tile;
        final ISidedMultipart multipartTile;
        
        SidedMultipartModule(TileEntity tile) {
            assert tile instanceof ISidedMultipart;
            this.tile = tile;
            this.multipartTile = (ISidedMultipart) tile;
            coreModule = multipartTile.initialCoreModule();
        }
        
        @Override
        public TileEntity getTile() {
            return tile;
        }
        
        ITileModule getSideModule(@Nullable Direction side) {
            if (side == null) {
                return coreModule;
            }
            ITileModule module = sidedModules[side.getIndex()];
            if (module == null) {
                return coreModule;
            }
            return module;
        }
        
        void setSideModule(@Nullable ITileModule module, @Nullable Direction side) {
            if (side == null) {
                if (module != null) {
                    coreModule = module;
                }
                return;
            }
            sidedModules[side.getIndex()] = module;
        }
        
        @Override
        public <T> LazyOptional<T> capability(Capability<T> cap, @Nullable Direction side) {
            return getSideModule(side).capability(cap, side);
        }
        
        @Override
        public void onAdded() {
            coreModule.onAdded();
            for (ITileModule sidedModule : sidedModules) {
                if (sidedModule != null) {
                    sidedModule.onAdded();
                }
            }
        }
        
        @Override
        public void onRemoved(boolean chunkUnload) {
            coreModule.onRemoved(chunkUnload);
            for (ITileModule sidedModule : sidedModules) {
                if (sidedModule != null) {
                    sidedModule.onRemoved(chunkUnload);
                }
            }
        }
        
        @Override
        public void readNBT(CompoundNBT nbt) {
            if (nbt.contains("core")) {
                CompoundNBT subNBT = nbt.getCompound("core");
                coreModule.readNBT(subNBT);
            }
            for (Direction value : Direction.values()) {
                if (nbt.contains(value.toString())) {
                    CompoundNBT subNBT = nbt.getCompound(value.toString());
                    ITileModule module = sidedModules[value.getIndex()];
                    if (module == null) {
                        PhosphophylliteTile.LOGGER.warn("Attempting to read NBT to side module that doesnt exist! " + tile + " : " + tile.getPos());
                        continue;
                    }
                    module.readNBT(subNBT);
                }
            }
        }
        
        @Nullable
        @Override
        public CompoundNBT writeNBT() {
            CompoundNBT nbt = null;
            CompoundNBT subNBT = coreModule.writeNBT();
            if (subNBT != null) {
                nbt = new CompoundNBT();
                nbt.put("core", subNBT);
            }
            for (Direction value : Direction.values()) {
                ITileModule module = sidedModules[value.getIndex()];
                if (module != null) {
                    subNBT = module.writeNBT();
                    if (subNBT != null) {
                        if (nbt == null) {
                            nbt = new CompoundNBT();
                        }
                        nbt.put(value.toString(), subNBT);
                    }
                }
            }
            return nbt;
        }
        
        @Override
        public void handleDataNBT(CompoundNBT nbt) {
            if (nbt.contains("core")) {
                CompoundNBT subNBT = nbt.getCompound("core");
                coreModule.handleDataNBT(subNBT);
            }
            for (Direction value : Direction.values()) {
                if (nbt.contains(value.toString())) {
                    CompoundNBT subNBT = nbt.getCompound(value.toString());
                    ITileModule module = sidedModules[value.getIndex()];
                    if (module == null) {
                        PhosphophylliteTile.LOGGER.warn("Attempting to read NBT to side module that doesnt exist! " + tile + " : " + tile.getPos());
                        continue;
                    }
                    module.handleDataNBT(subNBT);
                }
            }
        }
        
        @Nullable
        @Override
        public CompoundNBT getDataNBT() {
            CompoundNBT nbt = null;
            CompoundNBT subNBT = coreModule.getDataNBT();
            if (subNBT != null) {
                nbt = new CompoundNBT();
                nbt.put("core", subNBT);
            }
            for (Direction value : Direction.values()) {
                ITileModule module = sidedModules[value.getIndex()];
                if (module != null) {
                    subNBT = module.getDataNBT();
                    if (subNBT != null) {
                        if (nbt == null) {
                            nbt = new CompoundNBT();
                        }
                        nbt.put(value.toString(), subNBT);
                    }
                }
            }
            return nbt;
        }
        
        @Override
        public void handleUpdateNBT(CompoundNBT nbt) {
            if (nbt.contains("core")) {
                CompoundNBT subNBT = nbt.getCompound("core");
                coreModule.handleUpdateNBT(subNBT);
            }
            for (Direction value : Direction.values()) {
                if (nbt.contains(value.toString())) {
                    CompoundNBT subNBT = nbt.getCompound(value.toString());
                    ITileModule module = sidedModules[value.getIndex()];
                    if (module == null) {
                        PhosphophylliteTile.LOGGER.warn("Attempting to read NBT to side module that doesnt exist! " + tile + " : " + tile.getPos());
                        continue;
                    }
                    module.handleUpdateNBT(subNBT);
                }
            }
        }
        
        @Nullable
        @Override
        public CompoundNBT getUpdateNBT() {
            CompoundNBT nbt = null;
            CompoundNBT subNBT = coreModule.getUpdateNBT();
            if (subNBT != null) {
                nbt = new CompoundNBT();
                nbt.put("core", subNBT);
            }
            for (Direction value : Direction.values()) {
                ITileModule module = sidedModules[value.getIndex()];
                if (module != null) {
                    subNBT = module.getUpdateNBT();
                    if (subNBT != null) {
                        if (nbt == null) {
                            nbt = new CompoundNBT();
                        }
                        nbt.put(value.toString(), subNBT);
                    }
                }
            }
            return nbt;
        }
        
        @Override
        public String getDebugInfo() {
            StringBuilder builder = new StringBuilder();
            builder.append("SidedModules:\n");
            builder.append("Core:\n");
            builder.append(coreModule.getDebugInfo());
            for (Direction value : Direction.values()) {
                ITileModule module = sidedModules[value.getIndex()];
                if(module != null){
                    builder.append("\n");
                    builder.append(value);
                    builder.append(":\n");
                    builder.append(module.getDebugInfo());
                }
            }
            builder.append("\n");
            return builder.toString();
        }
        
        @Override
        public String saveKey() {
            return "PhosphophylliteMultipartTileModule";
        }
    }
}
