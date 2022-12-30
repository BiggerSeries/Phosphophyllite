package net.roguelogix.phosphophyllite.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.roguelogix.phosphophyllite.serialization.PhosphophylliteCompound;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Supplier;

@NonnullDefault
public class SimplePhosChannel {
    private final SimpleChannel simpleChannel;
    private final Consumer<PhosphophylliteCompound> callbackFunction;
    
    public SimplePhosChannel(ResourceLocation id, String version, Consumer<PhosphophylliteCompound> callbackFunction) {
        simpleChannel = NetworkRegistry.newSimpleChannel(id, () -> version, version::equals, version::equals);
        simpleChannel.registerMessage(1, PhosphophylliteCompound.class, SimplePhosChannel::encodePacket, SimplePhosChannel::decodePacket, this::handler);
        this.callbackFunction = callbackFunction;
    }
    
    private static void encodePacket(PhosphophylliteCompound packet, FriendlyByteBuf buf) {
        final var robn = packet.toROBN();
        buf.writeVarInt(robn.size());
        for (int i = 0; i < robn.size(); i++) {
            buf.writeByte(robn.get(i));
        }
    }
    
    private static PhosphophylliteCompound decodePacket(FriendlyByteBuf buf) {
        return new PhosphophylliteCompound(buf.readByteArray());
    }
    
    private void handler(PhosphophylliteCompound compound, @Nonnull Supplier<NetworkEvent.Context> ctxs) {
        final var ctx = ctxs.get();
        ctx.enqueueWork(() -> {
            callbackFunction.accept(compound);
        });
        ctx.setPacketHandled(true);
    }
    
    public void sendToServer(PhosphophylliteCompound compound) {
        simpleChannel.sendToServer(compound);
    }
    
    public void sendToPlayer(ServerPlayer serverPlayer, PhosphophylliteCompound compound) {
        simpleChannel.sendTo(compound, serverPlayer.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
}
