package net.roguelogix.phosphophyllite.quartz.internal.management;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.roguelogix.phosphophyllite.quartz.internal.rendering.Renderer;
import net.roguelogix.phosphophyllite.registry.OnModLoad;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EventHandling {
    
    public static final Logger LOGGER = LogManager.getLogger("Phosphophyllite-Quartz-Management");
    
    @OnModLoad
    static void onModLoad(){
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }
        FMLJavaModLoadingContext.get().getModEventBus().addListener(EventHandling::FMLClientSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(EventHandling::FMLLoadComplete);
        MinecraftForge.EVENT_BUS.addListener(EventHandling::onEnteringChunk);
        MinecraftForge.EVENT_BUS.addListener(EventHandling::onWorldLoad);
        MinecraftForge.EVENT_BUS.addListener(EventHandling::onRenderWorldLastEvent);
    }
    
    static void FMLClientSetup(FMLClientSetupEvent clientSetupEvent){
        clientSetupEvent.enqueueWork(()->{
            // sadly yes, this has to be done here
            
            // ok, so, first up, need to make sure ive got a front end to work with
            // this also does the GL setup the front end needs to do
            Renderer.create();
            
            // must be done before *any* textures have the chance to load, so it gets ID 0, the fallback ID
            TextureManagement.loadFallbackTexture();
            
            // i can now have it load textures and other stuff
            // which, surprise surprise, *needs to be done on this thread*
            // thx OpenGL, *fucking ass*
            TextureManagement.loadTexturesForTiles();
            
            // side effect of that, all the state jsons are now cached in ram
            // unless cleared, those wont need to be pulled from disk, exact states are not computed until needed
            // init time loading should be done now
        });
    }
    
    static void FMLLoadComplete(FMLLoadCompleteEvent loadCompleteEvent){
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }
        if(Renderer.INSTANCE == null){
            throw new IllegalStateException("Quartz failed to initialize, check log for details");
        }
    }
    
    // "Event" this is from a mixin
    public static void onChunkRebuild(Vector3i posA) {
        Vector3i posB = new Vector3i(posA).add(16, 16, 16);
        WorldManagement.updateRange(posA, posB);
    }
    
    static void onEnteringChunk(EntityEvent.EnteringChunk enteringChunk){
        Entity entity = enteringChunk.getEntity();
        if(entity instanceof ClientPlayerEntity){
            // local player moved to a new chunk
            WorldManagement.playerMovedChunk(entity.chunkCoordX, entity.chunkCoordY, entity.chunkCoordZ);
        }
    }
    
    public static void onRenderWorldLastEvent(RenderWorldLastEvent e) {
        Minecraft.getInstance().worldRenderer.renderDispatcher.uploadTasks.add(Renderer.INSTANCE::draw);
    }
    
    static void onWorldLoad(WorldEvent.Load e) {
        World world = (World) e.getWorld();
        if (!world.isRemote()) {
            return;
        }
        WorldManagement.init(world);
    }
}
