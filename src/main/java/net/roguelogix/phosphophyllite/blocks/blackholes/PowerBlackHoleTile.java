package net.roguelogix.phosphophyllite.blocks.blackholes;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.roguelogix.phosphophyllite.energy.IPhosphophylliteEnergyStorage;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.registry.TileSupplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@RegisterTileEntity(name = "power_black_hole")
public class PowerBlackHoleTile extends TileEntity implements IPhosphophylliteEnergyStorage {
    
    @RegisterTileEntity.Type
    public static TileEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final TileSupplier SUPPLIER = PowerBlackHoleTile::new;
    
    public PowerBlackHoleTile() {
        super(TYPE);
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
