package net.roguelogix.phosphophyllite.config;

import net.minecraftforge.fml.loading.FMLEnvironment;

public enum ConfigType {
    CLIENT(FMLEnvironment.dist.isClient()),
    COMMON(true),
    SERVER(true);
    
    public final boolean appliesToPhysicalSide;
    
    ConfigType(boolean appliesToPhysicalSide) {
        this.appliesToPhysicalSide = appliesToPhysicalSide;
    }
}
