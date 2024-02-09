package net.roguelogix.phosphophyllite.energy;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.roguelogix.phosphophyllite.capability.CachedWrappedBlockCapability;
import net.roguelogix.phosphophyllite.modular.api.IModularTile;
import net.roguelogix.phosphophyllite.registry.CapabilityRegistration;
import net.roguelogix.phosphophyllite.registry.RegisterCapability;
import net.roguelogix.phosphophyllite.util.API;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

import javax.annotation.Nullable;

@API
@NonnullDefault
public interface IEnergyTile extends IModularTile {
    
    @RegisterCapability
    CapabilityRegistration ENERGY_HANDLER_CAP_REGISTRATION = CapabilityRegistration.lazyTileCaps(IEnergyTile.class, () -> EnergyHandlerWrappers.wrap(IEnergyTile::energyHandler));
    
    @API
    IPhosphophylliteEnergyHandler energyHandler(Direction direction);
    
    @API
    default CachedWrappedBlockCapability<IPhosphophylliteEnergyHandler, Direction> findEnergyCapability(Direction direction) {
        final var thisTile = this.as(BlockEntity.class);
        @Nullable
        final var level = thisTile.getLevel();
        assert level != null;
        return new CachedWrappedBlockCapability<>(EnergyHandlerWrappers::findCapability, (ServerLevel) level, thisTile.getBlockPos().offset(direction.getNormal()), direction.getOpposite(), () -> !thisTile.isRemoved(), () -> onNeighborEnergyCapabilityInvalidated(direction));
    }
    
    @API
    default void onNeighborEnergyCapabilityInvalidated(Direction direction) {
    }
}
