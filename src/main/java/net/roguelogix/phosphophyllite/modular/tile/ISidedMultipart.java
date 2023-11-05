package net.roguelogix.phosphophyllite.modular.tile;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.capabilities.Capability;
import net.neoforged.neoforge.common.util.LazyOptional;
import net.roguelogix.phosphophyllite.debug.DebugInfo;
import net.roguelogix.phosphophyllite.modular.api.IModularTile;
import net.roguelogix.phosphophyllite.modular.api.TileModule;
import net.roguelogix.phosphophyllite.modular.api.ModuleRegistry;
import net.roguelogix.phosphophyllite.registry.OnModLoad;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static net.roguelogix.phosphophyllite.modular.tile.PhosphophylliteTile.MODULE_LOGGER;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface ISidedMultipart extends IModularTile {
    
    TileModule<?> initialCoreModule();
    
    class SidedMultipartModule extends TileModule<ISidedMultipart> {
        
        @OnModLoad
        private static void onModLoad() {
            ModuleRegistry.registerTileModule(ISidedMultipart.class, SidedMultipartModule::new);
        }
        
        @Nonnull
        TileModule<?> coreModule;
        
        final TileModule<?>[] sidedModules = new TileModule[6];
        
        private SidedMultipartModule(ISidedMultipart iface) {
            super(iface);
            coreModule = iface.initialCoreModule();
        }
        
        public TileModule<?> getSideModule(@Nullable Direction side) {
            if (side == null) {
                return coreModule;
            }
            var module = sidedModules[side.get3DDataValue()];
            if (module == null) {
                return coreModule;
            }
            return module;
        }
        
        public <T> T getSideModule(@Nullable Direction side, Class<T> moduleType) {
            //noinspection unchecked
            return (T) getSideModule(side);
        }
        
        public void setSideModule(@Nullable TileModule<?> module, @Nullable Direction side) {
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
            for (TileModule<?> sidedModule : sidedModules) {
                if (sidedModule != null) {
                    sidedModule.onAdded();
                }
            }
        }
        
        @Override
        public void onRemoved(boolean chunkUnload) {
            coreModule.onRemoved(chunkUnload);
            for (TileModule<?> sidedModule : sidedModules) {
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
                    TileModule<?> module = sidedModules[value.get3DDataValue()];
                    if (module == null) {
                        MODULE_LOGGER.warn("Attempting to read NBT to side module that doesnt exist! " + iface + " : " + iface.as(BlockEntity.class).getBlockState());
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
                TileModule<?> module = sidedModules[value.get3DDataValue()];
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
                    var module = sidedModules[value.get3DDataValue()];
                    if (module == null) {
                        MODULE_LOGGER.warn("Attempting to read NBT to side module that doesnt exist! " + iface + " : " + iface.as(BlockEntity.class).getBlockState());
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
                var module = sidedModules[value.get3DDataValue()];
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
                    var module = sidedModules[value.get3DDataValue()];
                    if (module == null) {
                        MODULE_LOGGER.warn("Attempting to read NBT to side module that doesnt exist! " + iface + " : " + iface.as(BlockEntity.class).getBlockState());
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
                var module = sidedModules[value.get3DDataValue()];
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
        
        @Nullable
        @Override
        public DebugInfo getDebugInfo() {
            return null;
//            var hasDebug = false;
//            StringBuilder builder = new StringBuilder();
//            var debugString = coreModule.getDebugInfo();
//            builder.append("SidedModules:\n");
//            if (debugString != null) {
//                hasDebug = true;
//                builder.append("    Core:\n");
//                var lines = debugString.split("\n");
//                for (String line : lines) {
//                    builder.append("    ");
//                    builder.append(line);
//                }
//            }
//            for (Direction value : Direction.values()) {
//                var module = sidedModules[value.get3DDataValue()];
//                if (module != null) {
//                    debugString = module.getDebugInfo();
//                    if (debugString == null) {
//                        continue;
//                    }
//                    hasDebug = true;
//                    builder.append("\n    ");
//                    builder.append(value);
//                    builder.append(":\n");
//                    var lines = debugString.split("\n");
//                    for (String line : lines) {
//                        builder.append("        ");
//                        builder.append(line);
//                    }
//                }
//            }
//            if (!hasDebug) {
//                return null;
//            }
//            builder.append("\n");
//            return builder.toString();
        }
        
        @Override
        public String saveKey() {
            return "PhosphophylliteMultipartTileModule";
        }
    }
}
