package net.roguelogix.phosphophyllite.quartz.api;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.extensions.IForgeTileEntity;

public interface IQuartzTile extends IForgeTileEntity {
    /**
     * Must always return the same QuartzState instance
     * this is cached inside the renderer so world queries aren't needed
     * the QuartzState itself can be modified as much as you like
     */
    QuartzState getState();
    
    default ResourceLocation getStateJSONLocation() {
        ResourceLocation registryName = this.getTileEntity().getType().getRegistryName();
        assert registryName != null;
        return new ResourceLocation(registryName.getNamespace(), "quartzstates/" + registryName.getPath());
    }
}
