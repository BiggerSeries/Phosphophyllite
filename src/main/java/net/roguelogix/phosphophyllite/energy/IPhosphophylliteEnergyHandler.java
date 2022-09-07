package net.roguelogix.phosphophyllite.energy;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.roguelogix.phosphophyllite.registry.RegisterCapability;

public interface IPhosphophylliteEnergyHandler {
    
    @RegisterCapability
    Capability<IPhosphophylliteEnergyHandler> CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});
    
    long insertEnergy(long maxInsert, boolean simulate);
    
    long extractEnergy(long maxExtract, boolean simulate);
    
    long energyStored();
    
    long maxEnergyStored();
}
