package net.roguelogix.phosphophyllite.networking;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.robn.ROBN;
import net.roguelogix.phosphophyllite.serialization.PhosphophylliteCompound;
import net.roguelogix.phosphophyllite.util.NonnullDefault;
import net.roguelogix.phosphophyllite.util.Pair;

import java.util.List;
import java.util.function.BiConsumer;

@NonnullDefault
public class SimplePhosChannel {
    
    private static final List<Pair<ResourceLocation, IPayloadHandler<CompoundPacket>>> channelsToRegister = new ObjectArrayList<>();
    public static void register(final RegisterPayloadHandlerEvent event) {
        for (var pair : channelsToRegister) {
            FriendlyByteBuf.Reader<CompoundPacket> reader = (buf) -> new CompoundPacket((PhosphophylliteCompound) ROBN.fromROBN(ByteArrayList.wrap(buf.readByteArray())), pair.first());
            event.registrar(Phosphophyllite.modid).common(pair.first(), reader, pair.second());
        }
    }

    private final ResourceLocation id;
    private final BiConsumer<PhosphophylliteCompound, IPayloadContext> clientCallback;
    private final BiConsumer<PhosphophylliteCompound, IPayloadContext> serverCallback;
    
    public SimplePhosChannel(ResourceLocation id, BiConsumer<PhosphophylliteCompound, IPayloadContext> clientCallback, BiConsumer<PhosphophylliteCompound, IPayloadContext> serverCallback) {
        channelsToRegister.add(new Pair<>(id, this::handler));
        this.id = id;
        this.clientCallback = clientCallback;
        this.serverCallback = serverCallback;
    }
    
    private void handler(CompoundPacket compoundPacket, IPayloadContext ctx) {
        ctx.workHandler().execute(() -> {
            var callbackToUse = ctx.flow().isClientbound() ? clientCallback : serverCallback;
            callbackToUse.accept(compoundPacket.compound, ctx);
        });
    }
    
    public void sendToServer(PhosphophylliteCompound compound) {
        PacketDistributor.SERVER.noArg().send(new CompoundPacket(compound, id));
    }
    
    public void sendToPlayer(ServerPlayer serverPlayer, PhosphophylliteCompound compound) {
        PacketDistributor.PLAYER.with(serverPlayer).send(new CompoundPacket(compound, id));
    }
    
    public void forceClassLoad() {
    }
    
    private static class CompoundPacket implements CustomPacketPayload {
        
        public final PhosphophylliteCompound compound;
        public final ResourceLocation id;
        
        private CompoundPacket(PhosphophylliteCompound compound, ResourceLocation id) {
            this.compound = compound;
            this.id = id;
        }
        
        @Override
        public void write(FriendlyByteBuf friendlyByteBuf) {
        
        }
        
        @Override
        public ResourceLocation id() {
            return id;
        }
    }
}
