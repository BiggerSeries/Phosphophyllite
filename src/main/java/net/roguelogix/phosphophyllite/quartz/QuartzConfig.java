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
    
    public enum OperationMode {
        ANY,
        GL21_JAVA,
        GL33_CPP,
        GL46_CPP,
    }
    
    @PhosphophylliteConfig.Value(comment = "" +
            "Limit operation modes available for automatic selection\n" +
            "Useful for testing to ensure compatibility with all three pipelines"
    )
    public static OperationMode MAX_OPERATION_MODE = OperationMode.ANY;
    
    @PhosphophylliteConfig.Value(comment = "" +
            "Request a specific operation mode to be used\n" +
            "Will crash if operation mode not compatible"
    )
    public static OperationMode OPERATION_MODE = OperationMode.ANY;
    
    
    /**
     * If you are looking here, you are probably a mod author
     * you can enable this for fully on the fly loading of your states
     * great for testing, DO NOT USE IN RUNTIME
     * texture loading is *not* fast, and its best to have it cached at load time
     */
    @PhosphophylliteConfig.Value(comment = "" +
            "leave it false unless you are writing a mod using quartz\n" +
            "if this was suggested as a fix for something, wait for the mod author to fix it"
    )
    public static boolean AllowOnTheFlyTextureLoading = false;
}

