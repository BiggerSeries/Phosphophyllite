package net.roguelogix.phosphophyllite.energy;

import net.neoforged.neoforge.common.capabilities.Capability;
import net.neoforged.neoforge.common.capabilities.CapabilityManager;
import net.neoforged.neoforge.common.capabilities.CapabilityToken;
import net.roguelogix.phosphophyllite.registry.RegisterCapability;

public interface IPhosphophylliteEnergyHandler {
    
    @RegisterCapability
    Capability<IPhosphophylliteEnergyHandler> CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});
    
    long insertEnergy(long maxInsert, boolean simulate);
    
    long extractEnergy(long maxExtract, boolean simulate);
    
    long energyStored();
    
    long maxEnergyStored();
}
