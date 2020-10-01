package net.roguelogix.phosphophyllite.quartz;

import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.config.ConfigType;
import net.roguelogix.phosphophyllite.config.PhosphophylliteConfig;
import net.roguelogix.phosphophyllite.registry.RegisterConfig;

@RegisterConfig
@PhosphophylliteConfig(
        folder = Phosphophyllite.modid,
        name = "quartz",
        type = ConfigType.CLIENT
)
public class QuartzConfig {
    
    public enum OperationMode{
        GL21_JAVA,
        GL46_CPP,
    }
    
    @PhosphophylliteConfig.Value
    public static OperationMode OPERATION_MODE = OperationMode.GL46_CPP;
    
    @PhosphophylliteConfig.Value
    public static boolean USE_SERVER_ASSIST = false;
}

