package net.roguelogix.phosphophyllite;

import net.roguelogix.phosphophyllite.config.ConfigValue;
import net.roguelogix.phosphophyllite.registry.RegisterConfig;

public class PhosphophylliteConfig {
    
    @ConfigValue(comment = "Recommended value: false\nNo really, it should be false, dont use performant, it breaks shit", hidden = ConfigValue.BoolOption.True)
    public final boolean bypassPerformantCheck;
    
    {
        bypassPerformantCheck = false;
    }
    
    @ConfigValue(comment = "Enabled debug mode, creates additional warnings for testing")
    public final boolean debugMode;
    
    {
        debugMode = false;
    }
    
    public static class GUI {
        @ConfigValue(range = "[50,)")
        public final long UpdateIntervalMS;
        
        {
            UpdateIntervalMS = 200;
        }
    }
    
    @ConfigValue
    public final GUI gui = new GUI();
    
    
    @RegisterConfig.Registration
    public static void registration() {
        Phosphophyllite.LOGGER.debug("Config registration callback");
    }
    @RegisterConfig.PreLoad
    public static void preLoad() {
        Phosphophyllite.LOGGER.debug("Config pre load callback");
    }
    @RegisterConfig.PostLoad
    public static void postLoad() {
        Phosphophyllite.LOGGER.debug("Config post load callback");
    }
}
