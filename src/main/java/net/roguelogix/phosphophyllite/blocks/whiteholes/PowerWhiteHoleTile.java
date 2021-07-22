package net.roguelogix.phosphophyllite.blocks.whiteholes;

import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.roguelogix.phosphophyllite.energy.EnergyStorageWrapper;
import net.roguelogix.phosphophyllite.energy.IPhosphophylliteEnergyStorage;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.registry.TileSupplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@RegisterTileEntity(name = "power_white_hole")
public class PowerWhiteHoleTile extends TileEntity implements IEnergyStorage, ITickableTileEntity {
    
    @RegisterTileEntity.Type
    public static TileEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final TileSupplier SUPPLIER = PowerWhiteHoleTile::new;
    
    public PowerWhiteHoleTile() {
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
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return 0;
    }
    
    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }
    
    @Override
    public int getEnergyStored() {
        return 0;
    }
    
    @Override
    public int getMaxEnergyStored() {
        return 0;
    }
    
    @Override
    public boolean canExtract() {
        return false;
    }
    
    @Override
    public boolean canReceive() {
        return false;
    }
    
    private final IEnergyStorage[] lastCapability = new IEnergyStorage[6];
    private final IPhosphophylliteEnergyStorage[] wrapped = new IPhosphophylliteEnergyStorage[6];
    
    @Override
    public void tick() {
        assert world != null;
        for (Direction direction : Direction.values()) {
            TileEntity te = world.getTileEntity(pos.offset(direction));
            if (te != null) {
                te.getCapability(CapabilityEnergy.ENERGY, direction.getOpposite()).ifPresent(c -> {
                    final int directionIndex = direction.getIndex();
                    if (c != lastCapability[directionIndex]) {
                        lastCapability[directionIndex] = c;
                        wrapped[directionIndex] = EnergyStorageWrapper.wrap(c);
                    }
                    wrapped[directionIndex].insertEnergy(Long.MAX_VALUE, false);
                });
            }
        }
    }
}
