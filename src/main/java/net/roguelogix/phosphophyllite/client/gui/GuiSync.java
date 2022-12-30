package net.roguelogix.phosphophyllite.client.gui;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.registry.OnModLoad;
import net.roguelogix.phosphophyllite.robn.ROBN;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static net.roguelogix.phosphophyllite.Phosphophyllite.modid;

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
            HashMap<String, Object> map = new HashMap<>();
            
            map.put("request", requestName);
            if (requestData != null) {
                map.put("data", requestData);
            }
            
            final var buf = ROBN.toROBN(map);
            
            GUIPacketMessage message = new GUIPacketMessage();
            message.bytes = new byte[buf.size()];
            for (int i = 0; i < buf.size(); i++) {
                message.bytes[i] = buf.get(i);
            }
            
            INSTANCE.sendToServer(message);
        }
        
        default void executeRequest(String requestName, Object requestData) {
        }
    }
    
    public interface IGUIPacket {
        void read(@Nonnull Map<?, ?> data);
        
        @Nullable
        Map<?, ?> write();
    }
    
    private static class GUIPacketMessage {
        public byte[] bytes;
        
        public GUIPacketMessage() {
        
        }
        
        public GUIPacketMessage(@Nonnull byte[] readByteArray) {
            bytes = readByteArray;
        }
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
    
    private static final String PROTOCOL_VERSION = "0";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(modid, "multiblock/guisync"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    
    @OnModLoad
    public static void onModLoad() {
        INSTANCE.registerMessage(1, GUIPacketMessage.class, GuiSync::encodePacket, GuiSync::decodePacket, GuiSync::handler);
        MinecraftForge.EVENT_BUS.addListener(GuiSync::onContainerClose);
        MinecraftForge.EVENT_BUS.addListener(GuiSync::onContainerOpen);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            MinecraftForge.EVENT_BUS.addListener(GuiSync::GuiOpenEvent);
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
                            ByteArrayList buf;
                            try {
                                buf = ROBN.toROBN(packetMap);
                            } catch (IllegalStateException e) {
                                e.printStackTrace();
                                return;
                            }
                            GUIPacketMessage message = new GUIPacketMessage();
                            message.bytes = new byte[buf.size()];
                            for (int i = 0; i < buf.size(); i++) {
                                message.bytes[i] = buf.get(i);
                            }
                            INSTANCE.sendTo(message, ((ServerPlayer) player).connection.connection, NetworkDirection.PLAY_TO_CLIENT);
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
    
    private static void encodePacket(@Nonnull GUIPacketMessage packet, @Nonnull FriendlyByteBuf buf) {
        buf.writeBytes(packet.bytes);
    }
    
    private static GUIPacketMessage decodePacket(@Nonnull FriendlyByteBuf buf) {
        byte[] byteBuf = new byte[buf.readableBytes()];
        buf.readBytes(byteBuf);
        return new GUIPacketMessage(byteBuf);
    }
    
    private static void handler(@Nonnull GUIPacketMessage packet, @Nonnull Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            NetworkDirection direction = ctx.get().getDirection();
            IGUIPacketProvider currentGUI;
            ArrayList<Byte> buf = new ArrayList<>();
            for (byte aByte : packet.bytes) {
                buf.add(aByte);
            }
            Map<?, ?> map = (Map<?, ?>) ROBN.fromROBN(buf);
            
            if (direction == NetworkDirection.PLAY_TO_CLIENT) {
                currentGUI = GuiSync.currentGUI;
                if (currentGUI != null) {
                    IGUIPacket guiPacket = currentGUI.getGuiPacket();
                    if (guiPacket != null) {
                        guiPacket.read(map);
                    }
                }
            } else {
                currentGUI = playerGUIs.get(ctx.get().getSender());
                if (currentGUI != null) {
                    currentGUI.executeRequest((String) map.get("request"), map.get("data"));
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
