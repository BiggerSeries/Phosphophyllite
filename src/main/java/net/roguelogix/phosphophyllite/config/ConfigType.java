package net.roguelogix.phosphophyllite.config;

import net.minecraftforge.fml.loading.FMLEnvironment;

public enum ConfigType {
    NULL(false),
    CLIENT(isFMLClient()),
    COMMON(true),
    SERVER(true);
    
    public final boolean appliesToPhysicalSide;
    
    ConfigType(boolean appliesToPhysicalSide) {
        this.appliesToPhysicalSide = appliesToPhysicalSide;
    }
    
    private static boolean isFMLClient() {
        try {
            return FMLEnvironment.dist == null || FMLEnvironment.dist.isClient();
            // in case its loaded without FML present, treat it as client
        } catch (NoClassDefFoundError e) {
            return true;
        }
    }
    
    public ConfigType from(ConfigType type) {
        return switch (this) {
            case NULL -> type;
            case CLIENT -> CLIENT;
            case COMMON -> COMMON;
            case SERVER -> SERVER;
        };
    }
}
