package net.roguelogix.phosphophyllite.tile;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
        
        final BlockEntity tile;
        final ISidedMultipart multipartTile;
        
        SidedMultipartModule(BlockEntity tile) {
            assert tile instanceof ISidedMultipart;
            this.tile = tile;
            this.multipartTile = (ISidedMultipart) tile;
            coreModule = multipartTile.initialCoreModule();
        }
        
        @Override
        public BlockEntity getTile() {
            return tile;
        }
        
        public ITileModule getSideModule(@Nullable Direction side) {
            if (side == null) {
                return coreModule;
            }
            ITileModule module = sidedModules[side.get3DDataValue()];
            if (module == null) {
                return coreModule;
            }
            return module;
        }
        
        public <T> T getSideModule(@Nullable Direction side, Class<T> moduleType) {
            //noinspection unchecked
            return (T) getSideModule(side);
        }
        
        public void setSideModule(@Nullable ITileModule module, @Nullable Direction side) {
            if (side == null) {
                if (module != null) {
                    coreModule = module;
                }
                return;
            }
            sidedModules[side.get3DDataValue()] = module;
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
        public void readNBT(CompoundTag nbt) {
            if (nbt.contains("core")) {
                CompoundTag subNBT = nbt.getCompound("core");
                coreModule.readNBT(subNBT);
            }
            for (Direction value : Direction.values()) {
                if (nbt.contains(value.toString())) {
                    CompoundTag subNBT = nbt.getCompound(value.toString());
                    ITileModule module = sidedModules[value.get3DDataValue()];
                    if (module == null) {
                        PhosphophylliteTile.LOGGER.warn("Attempting to read NBT to side module that doesnt exist! " + tile + " : " + tile.getBlockState());
                        continue;
                    }
                    module.readNBT(subNBT);
                }
            }
        }
        
        @Nullable
        @Override
        public CompoundTag writeNBT() {
            CompoundTag nbt = null;
            CompoundTag subNBT = coreModule.writeNBT();
            if (subNBT != null) {
                nbt = new CompoundTag();
                nbt.put("core", subNBT);
            }
            for (Direction value : Direction.values()) {
                ITileModule module = sidedModules[value.get3DDataValue()];
                if (module != null) {
                    subNBT = module.writeNBT();
                    if (subNBT != null) {
                        if (nbt == null) {
                            nbt = new CompoundTag();
                        }
                        nbt.put(value.toString(), subNBT);
                    }
                }
            }
            return nbt;
        }
        
        @Override
        public void handleDataNBT(CompoundTag nbt) {
            if (nbt.contains("core")) {
                CompoundTag subNBT = nbt.getCompound("core");
                coreModule.handleDataNBT(subNBT);
            }
            for (Direction value : Direction.values()) {
                if (nbt.contains(value.toString())) {
                    CompoundTag subNBT = nbt.getCompound(value.toString());
                    ITileModule module = sidedModules[value.get3DDataValue()];
                    if (module == null) {
                        PhosphophylliteTile.LOGGER.warn("Attempting to read NBT to side module that doesnt exist! " + tile + " : " + tile.getBlockState());
                        continue;
                    }
                    module.handleDataNBT(subNBT);
                }
            }
        }
        
        @Nullable
        @Override
        public CompoundTag getDataNBT() {
            CompoundTag nbt = null;
            CompoundTag subNBT = coreModule.getDataNBT();
            if (subNBT != null) {
                nbt = new CompoundTag();
                nbt.put("core", subNBT);
            }
            for (Direction value : Direction.values()) {
                ITileModule module = sidedModules[value.get3DDataValue()];
                if (module != null) {
                    subNBT = module.getDataNBT();
                    if (subNBT != null) {
                        if (nbt == null) {
                            nbt = new CompoundTag();
                        }
                        nbt.put(value.toString(), subNBT);
                    }
                }
            }
            return nbt;
        }
        
        @Override
        public void handleUpdateNBT(CompoundTag nbt) {
            if (nbt.contains("core")) {
                CompoundTag subNBT = nbt.getCompound("core");
                coreModule.handleUpdateNBT(subNBT);
            }
            for (Direction value : Direction.values()) {
                if (nbt.contains(value.toString())) {
                    CompoundTag subNBT = nbt.getCompound(value.toString());
                    ITileModule module = sidedModules[value.get3DDataValue()];
                    if (module == null) {
                        PhosphophylliteTile.LOGGER.warn("Attempting to read NBT to side module that doesnt exist! " + tile + " : " + tile.getBlockState());
                        continue;
                    }
                    module.handleUpdateNBT(subNBT);
                }
            }
        }
        
        @Nullable
        @Override
        public CompoundTag getUpdateNBT() {
            CompoundTag nbt = null;
            CompoundTag subNBT = coreModule.getUpdateNBT();
            if (subNBT != null) {
                nbt = new CompoundTag();
                nbt.put("core", subNBT);
            }
            for (Direction value : Direction.values()) {
                ITileModule module = sidedModules[value.get3DDataValue()];
                if (module != null) {
                    subNBT = module.getUpdateNBT();
                    if (subNBT != null) {
                        if (nbt == null) {
                            nbt = new CompoundTag();
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
                ITileModule module = sidedModules[value.get3DDataValue()];
                if (module != null) {
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
        
        @Override
        public void onBlockUpdate(BlockState neighborBlockState, BlockPos neighborPos) {
            coreModule.onBlockUpdate(neighborBlockState, neighborPos);
            for (ITileModule sidedModule : sidedModules) {
                if (sidedModule != null) {
                    sidedModule.onBlockUpdate(neighborBlockState, neighborPos);
                }
            }
        }
    }
}
