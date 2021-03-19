package net.roguelogix.phosphophyllite.energy;

import net.minecraftforge.energy.IEnergyStorage;

public class EnergyStorageWrapper implements IPhosphophylliteEnergyStorage{
    
    public static IPhosphophylliteEnergyStorage wrap(IEnergyStorage storage){
        if(storage instanceof IPhosphophylliteEnergyStorage){
            return (IPhosphophylliteEnergyStorage) storage;
        }
        return new EnergyStorageWrapper(storage);
    }
    
    
    final IEnergyStorage storage;
    
    private EnergyStorageWrapper(IEnergyStorage storage){
        this.storage = storage;
    }
    
    @Override
    public long insertEnergy(long maxInsert, boolean simulate) {
        return storage.receiveEnergy((int) Math.min(Integer.MAX_VALUE, maxInsert), simulate);
    }
    
    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        return storage.extractEnergy((int) Math.min(Integer.MAX_VALUE, maxExtract), simulate);
    }
    
    @Override
    public long energyStored() {
        return storage.getEnergyStored();
    }
    
    @Override
    public long maxEnergyStored() {
        return storage.getMaxEnergyStored();
    }
    
    @Override
    public boolean canInsert() {
        return storage.canReceive();
    }
    
    @Override
    public boolean canExtract() {
        return storage.canExtract();
    }
}
