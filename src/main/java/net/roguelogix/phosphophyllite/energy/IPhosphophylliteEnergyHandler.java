package net.roguelogix.phosphophyllite.energy;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.util.API;

@API
public interface IPhosphophylliteEnergyHandler {
    
    BlockCapability<IPhosphophylliteEnergyHandler, Direction> CAPABILITY = BlockCapability.createSided(new ResourceLocation(Phosphophyllite.modid, "energy"), IPhosphophylliteEnergyHandler.class);
    
    long insertEnergy(long maxInsert, boolean simulate);
    
    long extractEnergy(long maxExtract, boolean simulate);
    
    long energyStored();
    
    long maxEnergyStored();
}
