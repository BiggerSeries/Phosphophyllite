package net.roguelogix.phosphophyllite.quartz;

import net.minecraftforge.eventbus.api.Event;

/**
 * Fired on MinecraftForge.EVENT_BUS
 */
public abstract class QuartzEvent extends Event {
    public QuartzEvent() {
    }
    
    public static class Startup extends QuartzEvent {
    }
    
    public static class Shutdown extends QuartzEvent {
    }
    
    public static class ResourcesLoaded extends QuartzEvent {
    }
    
    public static class ResourcesReloaded extends ResourcesLoaded {
    }
    
    public static class FrameStart extends QuartzEvent {
        public FrameStart() {
        }
    }
}
