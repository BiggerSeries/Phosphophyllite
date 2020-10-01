package net.roguelogix.phosphophyllite.quartz.api;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.roguelogix.phosphophyllite.quartz.internal.Renderer;
import net.roguelogix.phosphophyllite.quartz.internal.WorldManager;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;

public class Quartz {
    
    public static void setQuartzState(World world, Vector3i position, QuartzState state) {
        if (world.isRemote) {
            WorldManager.setQuartzState(position, state);
        }
    }
    
    public static void loadTexture(ResourceLocation textureLocation) {
        Renderer.INSTANCE.loadTexture(textureLocation.toString());
    }
    
    public static void loadTexture(String textureLocation) {
        Renderer.INSTANCE.loadTexture(textureLocation.toString());
    }
}
