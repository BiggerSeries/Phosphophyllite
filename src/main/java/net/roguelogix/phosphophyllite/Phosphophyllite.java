package net.roguelogix.phosphophyllite;

import com.google.common.base.Stopwatch;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.ServerResources;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fmlserverevents.FMLServerStoppedEvent;
import net.roguelogix.phosphophyllite.multiblock.MultiblockController;
import net.roguelogix.phosphophyllite.multiblock.MultiblockTileModule;
import net.roguelogix.phosphophyllite.registry.Registry;
import net.roguelogix.phosphophyllite.threading.Queues;
import net.roguelogix.phosphophyllite.threading.WorkQueue;
import net.roguelogix.phosphophyllite.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

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
    
    public Phosphophyllite() {
        new Registry();
        MinecraftForge.EVENT_BUS.register(this);
        
        if (PhosphophylliteConfig.bypassPerformantCheck) {
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
    
    public static ServerResources dataPackRegistries;
    
    @SubscribeEvent
    public void onAddReloadListenerEvent(AddReloadListenerEvent reloadListenerEvent) {
        dataPackRegistries = reloadListenerEvent.getDataPackRegistries();
    }
    
    @SubscribeEvent
    public void onServerStopped(FMLServerStoppedEvent serverStoppedEvent) {
        dataPackRegistries = null;
    }
    
    public static long tickNumber() {
        return tick;
    }
    
    public static final WorkQueue serverQueue = Queues.serverThread;
    
    private static final HashMap<ServerLevel, ArrayList<MultiblockController<?, ?>>> controllersToTick = new HashMap<>();
    private static final HashMap<ServerLevel, ArrayList<MultiblockTileModule<?, ?>>> tilesToAttach = new HashMap<>();
    private static final ArrayList<MultiblockController<?, ?>> newControllers = new ArrayList<>();
    private static final ArrayList<MultiblockController<?, ?>> oldControllers = new ArrayList<>();
    private static final ArrayList<MultiblockTileModule<?, ?>> newTiles = new ArrayList<>();
    
    public static void addController(MultiblockController<?, ?> controller) {
        newControllers.add(controller);
    }
    
    public static void removeController(MultiblockController<?, ?> controller) {
        oldControllers.add(controller);
    }
    
    public static void attachTile(MultiblockTileModule<?, ?> tile) {
        newTiles.add(tile);
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
            //noinspection SuspiciousMethodCalls
            tilesToAttach.remove(worldUnloadEvent.getWorld());
            newControllers.removeIf(multiblockController -> multiblockController.getWorld() == worldUnloadEvent.getWorld());
            oldControllers.removeIf(multiblockController -> multiblockController.getWorld() == worldUnloadEvent.getWorld());
            newTiles.removeIf(multiblockTile -> multiblockTile.iface.getLevel() == worldUnloadEvent.getWorld());
        }
    }
    
    @SubscribeEvent
    void onServerStop(final FMLServerStoppedEvent serverStoppedEvent){
        controllersToTick.clear();
        tilesToAttach.clear();
        newControllers.clear();
        oldControllers.clear();
        newTiles.clear();
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
        
        Queues.serverThread.runAll();
        
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
        for (var newTile : newTiles) {
            tilesToAttach.computeIfAbsent((ServerLevel) newTile.iface.getLevel(), k -> new ArrayList<>()).add(newTile);
        }
        newTiles.clear();
    }
    
    @SubscribeEvent
    public void tickWorld(TickEvent.WorldTickEvent e) {
        if (!(e.world instanceof ServerLevel)) {
            return;
        }
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
        
        ArrayList<MultiblockTileModule<?, ?>> tilesToAttach = Phosphophyllite.tilesToAttach.get(e.world);
        if (tilesToAttach != null) {
            tilesToAttach.sort(Comparator.comparing(module -> module.iface.getBlockPos()));
            for (var toAttach : tilesToAttach) {
                if (toAttach != null) {
                    toAttach.attachToNeighbors();
                }
            }
            tilesToAttach.clear();
        }
        
        Util.worldTickEndEvent(e.world);
    }
}