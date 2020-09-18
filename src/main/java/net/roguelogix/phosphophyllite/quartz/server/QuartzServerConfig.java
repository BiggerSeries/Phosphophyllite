package net.roguelogix.phosphophyllite.quartz.server;

import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.config.ConfigType;
import net.roguelogix.phosphophyllite.config.PhosphophylliteConfig;

@PhosphophylliteConfig(
        folder = Phosphophyllite.modid,
        name = "quartz",
        type = ConfigType.SERVER
)
public class QuartzServerConfig {
    @PhosphophylliteConfig.Value
    public boolean ENABLE_SERVER_ASSIST = true;
}
