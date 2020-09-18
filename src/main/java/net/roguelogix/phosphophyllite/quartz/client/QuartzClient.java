package net.roguelogix.phosphophyllite.quartz.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.roguelogix.phosphophyllite.quartz.client.renderer.QuartzRenderer;
import net.roguelogix.phosphophyllite.registry.OnModLoad;

public class QuartzClient {
    
    @OnModLoad
    public static void onModLoad() {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }
        
        FMLJavaModLoadingContext.get().getModEventBus().addListener(QuartzClient::onClientSetup);
        MinecraftForge.EVENT_BUS.addListener(QuartzClient::onRenderWorldLastEvent);
    }
    
    public static void onClientSetup(FMLClientSetupEvent e) {
        DeferredWorkQueue.runLater(() -> {
            QuartzRenderer.create();
            QuartzRenderer.INSTANCE.GLSetup();
        });
    }
    
    public static void onRenderWorldLastEvent(RenderWorldLastEvent e) {
        Minecraft.getInstance().worldRenderer.renderDispatcher.uploadTasks.add(QuartzRenderer.INSTANCE::draw);
    }
}
