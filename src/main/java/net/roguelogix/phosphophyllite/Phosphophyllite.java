package net.roguelogix.phosphophyllite;

import net.minecraft.resources.DataPackRegistries;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.loading.FMLLoader;
import net.roguelogix.phosphophyllite.multiblock.generic.MultiblockController;
import net.roguelogix.phosphophyllite.multiblock.generic.MultiblockTile;
import net.roguelogix.phosphophyllite.registry.Registry;
import net.roguelogix.phosphophyllite.threading.WorkQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

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
                throw new IllegalStateException("" +
                        "Performant is incompatible with Phosphophyllite\n" +
                        "This is a known issue with performant and it breaking other mods, the author does not care\n" +
                        "GitHub issue on the matter: https://github.com/someaddons/performant_issues/issues/70\n" +
                        "To bypass this check add \"bypassPerformantCheck: true\" to your phosphophyllite config\n" +
                        "If you bypass this check I (RogueLogix) will not provide any support for any issues related to Phosphophyllite or the mods that use it\n" +
                        "If you believe your issue is unrelated, disable performant and reproduce it\n" +
                        "By choosing to bypass this check you understand that here there be dragons");
            }
        }
    }
    
    
    public static DataPackRegistries dataPackRegistries;
    
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

    public static final WorkQueue serverQueue = new WorkQueue();
    
    private static final HashMap<ServerWorld, ArrayList<MultiblockController<?, ?, ?>>> controllersToTick = new HashMap<>();
    private static final HashMap<ServerWorld, ArrayList<MultiblockTile<?, ?, ?>>> tilesToAttach = new HashMap<>();
    private static final ArrayList<MultiblockController<?, ?, ?>> newControllers = new ArrayList<>();
    private static final ArrayList<MultiblockController<?, ?, ?>> oldControllers = new ArrayList<>();
    private static final ArrayList<MultiblockTile<?, ?, ?>> newTiles = new ArrayList<>();
    
    public static void addController(MultiblockController<?, ?, ?> controller) {
        newControllers.add(controller);
    }
    
    public static void removeController(MultiblockController<?, ?, ?> controller) {
        oldControllers.add(controller);
    }
    
    public static void attachTile(MultiblockTile<?, ?, ?> tile) {
        newTiles.add(tile);
    }
    
    @SubscribeEvent
    void onWorldUnload(final WorldEvent.Unload worldUnloadEvent) {
        if (!worldUnloadEvent.getWorld().isRemote()) {
            //noinspection SuspiciousMethodCalls
            ArrayList<MultiblockController<?, ?, ?>> controllersToTick = Phosphophyllite.controllersToTick.remove(worldUnloadEvent.getWorld());
            if (controllersToTick != null) {
                for (MultiblockController<?, ?, ?> multiblockController : controllersToTick) {
                    multiblockController.suicide();
                }
            }
            // apparently, stragglers can exist
            //noinspection SuspiciousMethodCalls
            tilesToAttach.remove(worldUnloadEvent.getWorld());
            newControllers.removeIf(multiblockController -> multiblockController.getWorld() == worldUnloadEvent.getWorld());
            oldControllers.removeIf(multiblockController -> multiblockController.getWorld() == worldUnloadEvent.getWorld());
            newTiles.removeIf(multiblockTile -> multiblockTile.getWorld() == worldUnloadEvent.getWorld());
        }
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

        serverQueue.runAll();

        for (MultiblockController<?, ?, ?> newController : newControllers) {
            controllersToTick.computeIfAbsent((ServerWorld) newController.getWorld(), k -> new ArrayList<>()).add(newController);
        }
        newControllers.clear();
        for (MultiblockController<?, ?, ?> oldController : oldControllers) {
            //noinspection SuspiciousMethodCalls
            ArrayList<MultiblockController<?, ?, ?>> controllers = controllersToTick.get(oldController.getWorld());
            controllers.remove(oldController);
        }
        oldControllers.clear();
        for (MultiblockTile<?, ?, ?> newTile : newTiles) {
            tilesToAttach.computeIfAbsent((ServerWorld) newTile.getWorld(), k -> new ArrayList<>()).add(newTile);
        }
        newTiles.clear();
    }
    
    @SubscribeEvent
    public void tickWorld(TickEvent.WorldTickEvent e) {
        if (!(e.world instanceof ServerWorld)) {
            return;
        }
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        
        ArrayList<MultiblockController<?, ?, ?>> controllersToTick = Phosphophyllite.controllersToTick.get(e.world);
        if (controllersToTick != null) {
            for (MultiblockController<?, ?, ?> controller : controllersToTick) {
                if (controller != null) {
                    controller.update();
                }
            }
        }
        
        ArrayList<MultiblockTile<?, ?, ?>> tilesToAttach = Phosphophyllite.tilesToAttach.get(e.world);
        if (tilesToAttach != null) {
            tilesToAttach.sort(Comparator.comparing(TileEntity::getPos));
            for (MultiblockTile<?, ?, ?> toAttach : tilesToAttach) {
                if (toAttach != null) {
                    toAttach.attachToNeighbors();
                }
            }
            tilesToAttach.clear();
        }
    }
}