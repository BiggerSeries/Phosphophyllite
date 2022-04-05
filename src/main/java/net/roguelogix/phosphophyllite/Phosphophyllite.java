package net.roguelogix.phosphophyllite;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import net.roguelogix.phosphophyllite.event.ReloadDataEvent;
import net.roguelogix.phosphophyllite.multiblock.MultiblockController;
import net.roguelogix.phosphophyllite.registry.RegisterConfig;
import net.roguelogix.phosphophyllite.registry.Registry;
import net.roguelogix.phosphophyllite.threading.Queues;
import net.roguelogix.phosphophyllite.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@SuppressWarnings("unused")
@Mod(Phosphophyllite.modid)
public class Phosphophyllite {
    public static final String modid = "phosphophyllite";
    
    public static final Logger LOGGER = LogManager.getLogger("Phosphophyllite/Main");
    public static long lastTime = 0;
    // used to ensure i dont tick things twice
    private static long tick = 0;
    
    @RegisterConfig(folder = modid, name = "general")
    public static final PhosphophylliteConfig CONFIG = new PhosphophylliteConfig();
    
    public Phosphophyllite() {
        new Registry();
        MinecraftForge.EVENT_BUS.register(this);
        
        if (CONFIG.bypassPerformantCheck) {
            LOGGER.warn("Performant check bypassed");
            LOGGER.warn("Performant " + (FMLLoader.getLoadingModList().getModFileById("performant") != null ? "is" : "is not") + " present");
        } else {
            if (FMLLoader.getLoadingModList().getModFileById("performant") != null) {
                throw new IllegalStateException("""
                        Performant is incompatible with Phosphophyllite
                        This is a known issue with performant and it breaking other mods, the author does not care
                        GitHub issue on the matter: https://github.com/someaddons/performant_issues/issues/70
                        To bypass this check add "bypassPerformantCheck: true" to your phosphophyllite config
                        If you bypass this check I (RogueLogix) will not provide any support for any issues related to Phosphophyllite or the mods that use it
                        If you believe your issue is unrelated, disable performant and reproduce it
                        By choosing to bypass this check you understand that here there be dragons""");
            }
        }
    }
    
    private static MinecraftServer server;
    public static ResourceManager serverResourceManager;
    
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent serverStartedEvent) {
        server = serverStartedEvent.getServer();
        updateRegistries();
    }
    
    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent serverStoppedEvent) {
        serverResourceManager = null;
        server = null;
    }
    
    @SubscribeEvent()
    public void onTagsUpdated(TagsUpdatedEvent tagsUpdatedEvent) {
        updateRegistries();
    }
    
    void updateRegistries() {
        if (server == null) {
            return;
        }
        if (FMLLoader.getDist().isClient()) {
            // ignore client thread
            // prevents double reloads, and reaching across sides
            if(RenderSystem.isOnRenderThread()){
                return;
            }
        }
        serverResourceManager = server.getResourceManager();
        MinecraftForge.EVENT_BUS.post(new ReloadDataEvent());
        controllersToTick.forEach((serverLevel, multiblockControllers) -> multiblockControllers.forEach(MultiblockController::revalidate));
    }
    
    public static long tickNumber() {
        return tick;
    }
    
    private static final HashMap<ServerLevel, ArrayList<MultiblockController<?, ?>>> controllersToTick = new HashMap<>();
    private static final ArrayList<MultiblockController<?, ?>> newControllers = new ArrayList<>();
    private static final ArrayList<MultiblockController<?, ?>> oldControllers = new ArrayList<>();
    
    public static void addController(MultiblockController<?, ?> controller) {
        newControllers.add(controller);
    }
    
    public static void removeController(MultiblockController<?, ?> controller) {
        oldControllers.add(controller);
    }
    
    @SubscribeEvent
    void onWorldUnload(final WorldEvent.Unload worldUnloadEvent) {
        if (!worldUnloadEvent.getWorld().isClientSide()) {
            //noinspection SuspiciousMethodCalls
            ArrayList<MultiblockController<?, ?>> controllersToTick = Phosphophyllite.controllersToTick.remove(worldUnloadEvent.getWorld());
            if (controllersToTick != null) {
                for (MultiblockController<?, ?> multiblockController : controllersToTick) {
                    multiblockController.suicide();
                }
            }
            // apparently, stragglers can exist
            newControllers.removeIf(multiblockController -> multiblockController.getWorld() == worldUnloadEvent.getWorld());
            oldControllers.removeIf(multiblockController -> multiblockController.getWorld() == worldUnloadEvent.getWorld());
        }
    }
    
    @SubscribeEvent
    void onServerStop(final ServerStoppedEvent serverStoppedEvent) {
        controllersToTick.clear();
        newControllers.clear();
        oldControllers.clear();
    }
    
    @SubscribeEvent
    public void advanceTick(TickEvent.ServerTickEvent e) {
        if (!e.side.isServer()) {
            return;
        }
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        tick++;
        
        // prevents deadlock
        final boolean[] run = {true};
        Queues.serverThread.enqueue(() -> run[0] = false);
        while (run[0]) {
            Queues.serverThread.runOne();
        }
        
        for (MultiblockController<?, ?> newController : newControllers) {
            controllersToTick.computeIfAbsent((ServerLevel) newController.getWorld(), k -> new ArrayList<>()).add(newController);
        }
        newControllers.clear();
        for (MultiblockController<?, ?> oldController : oldControllers) {
            //noinspection SuspiciousMethodCalls
            ArrayList<MultiblockController<?, ?>> controllers = controllersToTick.get(oldController.getWorld());
            controllers.remove(oldController);
        }
        oldControllers.clear();
    }
    
    @SubscribeEvent
    public void advanceTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.START) {
            return;
        }
        
        // prevents deadlock
        final boolean[] run = {true};
        Queues.clientThread.enqueue(() -> run[0] = false);
        while (run[0]) {
            Queues.clientThread.runOne();
        }
    }
    
    @SubscribeEvent
    public void tickWorld(TickEvent.WorldTickEvent e) {
        if (!(e.world instanceof ServerLevel)) {
            return;
        }
        Util.updateBlockStates(e.world);
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        
        ArrayList<MultiblockController<?, ?>> controllersToTick = Phosphophyllite.controllersToTick.get(e.world);
        if (controllersToTick != null) {
            for (MultiblockController<?, ?> controller : controllersToTick) {
                if (controller != null) {
                    controller.update();
                }
            }
        }
        
        Util.worldTickEndEvent(e.world);
    }
}