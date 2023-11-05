package net.roguelogix.phosphophyllite.energy;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.capabilities.Capability;
import net.neoforged.neoforge.common.util.LazyOptional;
import net.roguelogix.phosphophyllite.modular.api.IModularTile;
import net.roguelogix.phosphophyllite.modular.api.ModuleRegistry;
import net.roguelogix.phosphophyllite.modular.api.TileModule;
import net.roguelogix.phosphophyllite.registry.OnModLoad;
import net.roguelogix.phosphophyllite.util.NonnullDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@NonnullDefault
public interface IEnergyTile extends IModularTile {
    
    LazyOptional<IPhosphophylliteEnergyHandler> energyHandler();
    
    default LazyOptional<IPhosphophylliteEnergyHandler> findEnergyCapability(Direction direction) {
        return Objects.requireNonNull(module(IEnergyTile.class, Module.class)).findEnergyCapability(direction);
    }
    
    final class Module extends TileModule<IEnergyTile> {
        
        private LazyOptional<IPhosphophylliteEnergyHandler> baseHandler = LazyOptional.empty();
        
        private final Object2ObjectMap<Capability<?>, LazyOptional<?>> optionals = new Object2ObjectOpenHashMap<>();
        
        @OnModLoad
        private static void onModLoad() {
            ModuleRegistry.registerTileModule(IEnergyTile.class, Module::new);
        }
        
        public Module(IEnergyTile iface) {
            super(iface);
        }
        
        private void invalidate(LazyOptional<IPhosphophylliteEnergyHandler> ignored) {
            final var toInvalidate = new ObjectArrayList<>(optionals.values());
            optionals.clear();
            for (var lazyOptional : toInvalidate) {
                lazyOptional.invalidate();
            }
        }
        
        private LazyOptional<IPhosphophylliteEnergyHandler> handler() {
            if (!baseHandler.isPresent()) {
                baseHandler = iface.energyHandler();
                baseHandler.addListener(this::invalidate);
            }
            return baseHandler;
        }
        
        @Override
        public <T> LazyOptional<T> capability(Capability<T> cap, @Nullable Direction side) {
            var optional = optionals.get(cap);
            if (optional != null) {
                return optional.cast();
            }
            
            optional = EnergyHandlerWrappers.attemptWrap(cap, handler());
    
            if (optional.isPresent()) {
                optionals.put(cap, optional);
            }
            
            return optional.cast();
    
        }
        
        LazyOptional<IPhosphophylliteEnergyHandler> findEnergyCapability(Direction direction) {
            final var thisTile = iface.as(BlockEntity.class);
            //noinspection ConstantConditions
            final var tile = thisTile.getLevel().getBlockEntity(thisTile.getBlockPos().offset(direction.getNormal()));
            if (tile == null) {
                return LazyOptional.empty();
            }
            return EnergyHandlerWrappers.findCapability(tile, direction.getOpposite());
        }
    }
}
