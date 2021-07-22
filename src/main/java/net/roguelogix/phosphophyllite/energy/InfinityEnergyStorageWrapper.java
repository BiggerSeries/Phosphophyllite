package net.roguelogix.phosphophyllite.energy;

import com.buuz135.industrial.item.infinity.InfinityEnergyStorage;

public class InfinityEnergyStorageWrapper implements IPhosphophylliteEnergyStorage {
    final InfinityEnergyStorage<?> energyStorage;
    
    InfinityEnergyStorageWrapper(InfinityEnergyStorage<?> energyStorage) {
        this.energyStorage = energyStorage;
    }
    
    @Override
    public long insertEnergy(long maxInsert, boolean simulate) {
        if(!canInsert()){
            return 0;
        }
        final long stored = energyStorage.getLongEnergyStored();
        final long capacity = energyStorage.getLongCapacity();
        final long toInsert = Math.min(maxInsert, capacity - stored);
        if (!simulate) {
            energyStorage.setEnergyStored(stored + toInsert);
        }
        return toInsert;
    }
    
    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        if(!canExtract()){
            return 0;
        }
        final long stored = energyStorage.getLongEnergyStored();
        final long toExtract = Math.min(maxExtract, stored);
        if (!simulate) {
            energyStorage.setEnergyStored(stored - toExtract);
        }
        return toExtract;
    }
    
    @Override
    public long energyStored() {
        return energyStorage.getLongEnergyStored();
    }
    
    @Override
    public long maxEnergyStored() {
        return energyStorage.getLongCapacity();
    }
    
    @Override
    public boolean canInsert() {
        return energyStorage.canReceive();
    }
    
    @Override
    public boolean canExtract() {
        return energyStorage.canExtract();
    }
}
