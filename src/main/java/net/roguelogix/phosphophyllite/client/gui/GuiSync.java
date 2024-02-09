package net.roguelogix.phosphophyllite.client.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.networking.SimplePhosChannel;
import net.roguelogix.phosphophyllite.registry.OnModLoad;
import net.roguelogix.phosphophyllite.serialization.PhosphophylliteCompound;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import static net.roguelogix.phosphophyllite.Phosphophyllite.modid;


@Deprecated
public class GuiSync {
    
    public interface IGUIPacketProvider {
        
        @Nullable
        IGUIPacket getGuiPacket();
        
        /**
         * DO NOT OVERRIDE THIS METHOD!
         *
         * @param requestName The request to make.
         * @param requestData The payload to send.
         */
        default void runRequest(@Nonnull String requestName, @Nullable Object requestData) {
            var compound = new PhosphophylliteCompound();
            
            compound.put("request", requestName);
            if (requestData != null) {
                compound.put("data", requestData);
            }
            
            INSTANCE.sendToServer(compound);
        }
        
        default void executeRequest(String requestName, Object requestData) {
        }
    }
    
    public interface IGUIPacket {
        void read(@Nonnull Map<?, ?> data);
        
        @Nullable
        Map<?, ?> write();
    }
    private static final HashMap<Player, IGUIPacketProvider> playerGUIs = new HashMap<>();
    
    public static synchronized void onContainerOpen(@Nonnull PlayerContainerEvent.Open e) {
        AbstractContainerMenu container = e.getContainer();
        if (container instanceof IGUIPacketProvider) {
            playerGUIs.put(e.getEntity(), (IGUIPacketProvider) container);
        }
    }
    
    public static synchronized void onContainerClose(@Nonnull PlayerContainerEvent.Close e) {
        playerGUIs.remove(e.getEntity());
    }
    
    private static IGUIPacketProvider currentGUI;
    
    @OnlyIn(Dist.CLIENT)
    public static synchronized void GuiOpenEvent(@Nonnull ScreenEvent.Opening e) {
        
        Screen gui = e.getScreen();
        if (gui instanceof AbstractContainerScreen) {
            AbstractContainerMenu container = ((AbstractContainerScreen<?>) gui).getMenu();
            if (container instanceof IGUIPacketProvider) {
                currentGUI = (IGUIPacketProvider) container;
            }
        } else {
            currentGUI = null;
        }
    }
    
    public static final SimplePhosChannel INSTANCE = new SimplePhosChannel(
            new ResourceLocation(modid, "multiblock/guisync"),
            GuiSync::clientHandler, GuiSync::serverHandler
    );
    
    @OnModLoad
    public static void onModLoad() {
        NeoForge.EVENT_BUS.addListener(GuiSync::onContainerClose);
        NeoForge.EVENT_BUS.addListener(GuiSync::onContainerOpen);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.addListener(GuiSync::GuiOpenEvent);
        }
        Thread updateThread = new Thread(() -> {
            while (true) {
                synchronized (GuiSync.class) {
                    playerGUIs.forEach((player, gui) -> {
                        try {
                            assert player instanceof ServerPlayer;
                            IGUIPacket packet = gui.getGuiPacket();
                            if (packet == null) {
                                return;
                            }
                            Map<?, ?> packetMap = packet.write();
                            if (packetMap == null) {
                                return;
                            }
                            
                            var compound = new PhosphophylliteCompound((Map<String, Object>) packetMap);
                            
                            INSTANCE.sendToPlayer(((ServerPlayer) player), compound);
                            
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
                try {
                    //noinspection BusyWait
                    Thread.sleep(Phosphophyllite.CONFIG.gui.UpdateIntervalMS);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        updateThread.setName("Phosphophyllite-GuiSync");
        updateThread.setDaemon(true);
        updateThread.start();
    }
    
    private static void clientHandler(PhosphophylliteCompound compound, IPayloadContext ctx) {
        if (currentGUI != null) {
            IGUIPacket guiPacket = currentGUI.getGuiPacket();
            if (guiPacket != null) {
                guiPacket.read(compound.toROBNMap());
            }
        }
    }
    
    private static void serverHandler(PhosphophylliteCompound compound, IPayloadContext ctx) {
        currentGUI = playerGUIs.get(ctx.player().orElse(null));
        if (currentGUI != null) {
            currentGUI.executeRequest((String) compound.get("request"), compound.get("data"));
        }
    }
}
