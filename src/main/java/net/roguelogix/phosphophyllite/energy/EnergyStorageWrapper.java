package net.roguelogix.phosphophyllite.energy;

import com.buuz135.industrial.block.misc.tile.InfinityChargerTile;
import com.buuz135.industrial.item.infinity.InfinityEnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

public class EnergyStorageWrapper implements IPhosphophylliteEnergyStorage {
    
    private static Class<?> infinityEnergyStorageClazz = null;
    
    static {
        try {
            infinityEnergyStorageClazz = InfinityEnergyStorage.class;
        } catch (NoClassDefFoundError ignored) {
            // expected when the mod isn't loaded
        }
    }
    
    public static IPhosphophylliteEnergyStorage wrap(IEnergyStorage storage) {
        if (storage instanceof IPhosphophylliteEnergyStorage) {
            return (IPhosphophylliteEnergyStorage) storage;
        }
        if (infinityEnergyStorageClazz != null) {
            // class is loaded
            if(storage instanceof InfinityEnergyStorage){
                return new InfinityEnergyStorageWrapper((InfinityEnergyStorage<?>) storage);
            }
        }
        return new EnergyStorageWrapper(storage);
    }
    
    
    final IEnergyStorage storage;
    
    private EnergyStorageWrapper(IEnergyStorage storage) {
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
