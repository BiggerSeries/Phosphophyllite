package net.roguelogix.phosphophyllite.energy;

import net.minecraftforge.energy.IEnergyStorage;

@Deprecated(forRemoval = true)
public interface IPhosphophylliteEnergyStorage extends IEnergyStorage {
    long insertEnergy(long maxInsert, boolean simulate);
    
    long extractEnergy(long maxExtract, boolean simulate);
    
    long energyStored();
    
    long maxEnergyStored();
    
    boolean canInsert();
    
    @Override
    default int receiveEnergy(int maxReceive, boolean simulate) {
        return (int) insertEnergy(maxReceive, simulate);
    }
    
    @Override
    default int extractEnergy(int maxExtract, boolean simulate) {
        return (int) extractEnergy((long)maxExtract, simulate);
    }
    
    @Override
    default int getEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, energyStored());
    }
    
    @Override
    default int getMaxEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, maxEnergyStored());
    }
    
    @Override
    default boolean canReceive() {
        return canInsert();
    }
}
