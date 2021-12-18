package net.roguelogix.phosphophyllite.quartz.internal.gl;

import net.roguelogix.phosphophyllite.config.ConfigValue;

public class GLConfig {
    public static final GLConfig INSTANCE = new GLConfig();
    
    @ConfigValue
    public final boolean ALLOW_BASE_INSTANCE;
    @ConfigValue
    public final boolean ALLOW_ATTRIB_BINDING;
    
    {
        ALLOW_BASE_INSTANCE = true;
        ALLOW_ATTRIB_BINDING = true;
    }
}
