package net.roguelogix.phosphophyllite.blocks.blackholes;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.roguelogix.phosphophyllite.energy.IPhosphophylliteEnergyStorage;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@RegisterTileEntity(name = "power_black_hole")
public class PowerBlackHoleTile extends BlockEntity implements IPhosphophylliteEnergyStorage {
    
    @RegisterTileEntity.Type
    public static BlockEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final BlockEntityType.BlockEntitySupplier<PowerBlackHoleTile> SUPPLIER = PowerBlackHoleTile::new;
    
    public PowerBlackHoleTile(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, final @Nullable Direction side) {
        if (cap == CapabilityEnergy.ENERGY) {
            return LazyOptional.of(() -> this).cast();
        }
        return super.getCapability(cap, side);
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
