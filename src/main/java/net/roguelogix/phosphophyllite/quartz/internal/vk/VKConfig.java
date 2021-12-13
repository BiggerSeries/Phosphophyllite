package net.roguelogix.phosphophyllite.quartz.internal.vk;

import net.roguelogix.phosphophyllite.config.ConfigValue;

public class VKConfig {
    public static final VKConfig INSTANCE = new VKConfig();
    
    @ConfigValue(comment = "Enables Vulkan validation layers\nDevelopment option")
    public final boolean enableValidationLayers;
    {
        enableValidationLayers = false;
    }
    
    @ConfigValue(comment = """
            Number of framebuffers to allocate
            Allows for the CPU/GPU to be working on multiple frames at the same time
            Increasing this number increases host and device memory requirements for values that are updated every frame
            """,
            range = "[1,9]"
    )
    public final int inFlightFrames;
    
    {
        inFlightFrames = 3;
    }
    
    @ConfigValue
    public final boolean useHostFrameCopy;
    
    {
        useHostFrameCopy = false;
    }
    
    @ConfigValue
    public final MemoryOptions memory = new MemoryOptions();
    
    public static class MemoryOptions {
        enum AllocationStyle {
            BUDDY,
            PACKED
        }
        
        @ConfigValue
        AllocationStyle deviceMemorySubAllocationStyle = AllocationStyle.BUDDY;
        @ConfigValue
        AllocationStyle bufferSubAllocationStyle = AllocationStyle.PACKED;
    }
}
