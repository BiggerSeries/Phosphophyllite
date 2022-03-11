package net.roguelogix.phosphophyllite.blocks.blackholes;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.roguelogix.phosphophyllite.energy.IPhosphophylliteEnergyStorage;
import net.roguelogix.phosphophyllite.modular.tile.PhosphophylliteTile;
import net.roguelogix.phosphophyllite.registry.RegisterTile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PowerBlackHoleTile extends PhosphophylliteTile implements IPhosphophylliteEnergyStorage {
    
    @RegisterTile("power_black_hole")
    public static final BlockEntityType.BlockEntitySupplier<PowerBlackHoleTile> SUPPLIER = new RegisterTile.Producer<>(PowerBlackHoleTile::new);
    
    public PowerBlackHoleTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
    @Nonnull
    @Override
    public <T> LazyOptional<T> capability(@Nonnull Capability<T> cap, final @Nullable Direction side) {
        if (cap == CapabilityEnergy.ENERGY) {
            return LazyOptional.of(() -> this).cast();
        }
        return super.capability(cap, side);
    }
    
    @Override
    public long insertEnergy(long maxInsert, boolean simulate) {
        return maxInsert;
    }
    
    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        return 0;
    }
    
    @Override
    public long energyStored() {
        return 0;
    }
    
    @Override
    public long maxEnergyStored() {
        return Long.MAX_VALUE;
    }
    
    @Override
    public boolean canInsert() {
        return true;
    }
    
    @Override
    public boolean canExtract() {
        return false;
    }
}
