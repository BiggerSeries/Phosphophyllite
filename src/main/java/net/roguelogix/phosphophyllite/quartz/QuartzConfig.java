package net.roguelogix.phosphophyllite.quartz;

import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.config.ConfigType;
import net.roguelogix.phosphophyllite.config.ConfigValue;
import net.roguelogix.phosphophyllite.quartz.internal.gl.GLConfig;
import net.roguelogix.phosphophyllite.quartz.internal.vk.VKConfig;
import net.roguelogix.phosphophyllite.registry.RegisterConfig;

public class QuartzConfig {
    
    @RegisterConfig(folder = Phosphophyllite.modid, name = "quartz", type = ConfigType.CLIENT)
    public static final QuartzConfig INSTANCE = new QuartzConfig();
    
    @ConfigValue(hidden = true, enableAdvanced = true)
    public final boolean enableAdvanced;
    
    {
        enableAdvanced = false;
    }
    
    public enum Mode {
        Vulkan10,
        OpenGL33,
        Automatic,
        ;
    }
    
    @ConfigValue(comment = "Backend mode used by quartz\nAutomatic will try to use the best available, and fallback as necessary")
    public final Mode mode;
    
    {
        mode = Mode.Automatic;
    }
    
    @ConfigValue(advanced = true)
    public final GLConfig GL = GLConfig.INSTANCE;
    @ConfigValue(advanced = true)
    public final VKConfig VK = VKConfig.INSTANCE;
}
