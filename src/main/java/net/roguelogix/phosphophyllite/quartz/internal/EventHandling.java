package net.roguelogix.phosphophyllite.quartz.internal;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.lighting.LightEngine;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.roguelogix.phosphophyllite.registry.OnModLoad;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;
import net.roguelogix.phosphophyllite.threading.WorkQueue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static net.roguelogix.phosphophyllite.config.ConfigLoader.modifiersField;

public class EventHandling {
    @OnModLoad
    public static void onModLoad() {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }
        MinecraftForge.EVENT_BUS.addListener(EventHandling::onChunkLoad);
        MinecraftForge.EVENT_BUS.addListener(EventHandling::onWorldLoad);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(EventHandling::onClientSetup);
        MinecraftForge.EVENT_BUS.addListener(EventHandling::onRenderWorldLastEvent);
    }
    
    
    public static void onClientSetup(FMLClientSetupEvent e) {
        DeferredWorkQueue.runLater(() -> {
            Renderer.create();
            Renderer.INSTANCE.GLSetup();
        });
    }
    
    public static void onRenderWorldLastEvent(RenderWorldLastEvent e) {
        Minecraft.getInstance().worldRenderer.renderDispatcher.uploadTasks.add(Renderer.INSTANCE::draw);
    }
    
    public static WorkQueue queue = new WorkQueue().addProcessingThread();

//    public static class QuartzChunkSection extends ChunkSection {
//        public QuartzChunkSection(int yBaseIn) {
//            super(yBaseIn);
//        }
//
//        @Override
//        public BlockState setBlockState(int x, int y, int z, BlockState state, boolean useLocks) {
//            BlockState oldState = super.setBlockState(x, y, z, state, useLocks);
//            // if there was an event that triggered right *after* a block was placed, that would be great, but nooooo
//            if (oldState.getBlock() != state.getBlock() && state.getBlock() instanceof IQuartzBlock) {
//                queue.enqueue(() -> {
//                    WorldManager.setQuartzState(new Vector3i(x, y, z), ((IQuartzBlock) state.getBlock()).defaultQuartzState());
//                });
//            }
//            return oldState;
//        }
//
//        @Override
//        public void read(PacketBuffer packetBufferIn) {
//            super.read(packetBufferIn);
//
//        }
//    }
    
    public static class QuartzLongOpenHashSet extends LongOpenHashSet {
        @Override
        public LongIterator iterator() {
            super.iterator().forEachRemaining((long pos) -> {
                BlockPos blockPos = SectionPos.from(pos).asBlockPos();
                LightingManager.markSectionForRecompute(new Vector3i(blockPos.getX() & ~0xF, blockPos.getY()& ~0xF, blockPos.getZ()& ~0xF));
            });
            return super.iterator();
        }
    }
    
    static void onWorldLoad(WorldEvent.Load e) {
        World world = (World) e.getWorld();
        if (!world.isRemote()) {
            return;
        }
        for (Field declaredField : ((LightEngine<?, ?>) world.getLightManager().getLightEngine(LightType.BLOCK)).storage.getClass().getFields()) {
            try {
                modifiersField.set(declaredField, declaredField.getModifiers() & ~Modifier.FINAL);
                LongOpenHashSet newSet = new QuartzLongOpenHashSet();
                newSet.addAll(((LightEngine<?, ?>) world.getLightManager().getLightEngine(LightType.BLOCK)).storage.changedLightPositions);
                declaredField.set(((LightEngine<?, ?>) e.getWorld().getLightManager().getLightEngine(LightType.BLOCK)).storage, newSet);
            } catch (IllegalAccessException illegalAccessException) {
                illegalAccessException.printStackTrace();
            }
        }
        for (Field declaredField : ((LightEngine<?, ?>) world.getLightManager().getLightEngine(LightType.SKY)).storage.getClass().getFields()) {
            try {
                modifiersField.set(declaredField, declaredField.getModifiers() & ~Modifier.FINAL);
                LongOpenHashSet newSet = new QuartzLongOpenHashSet();
                newSet.addAll(((LightEngine<?, ?>) world.getLightManager().getLightEngine(LightType.SKY)).storage.changedLightPositions);
                declaredField.set(((LightEngine<?, ?>) e.getWorld().getLightManager().getLightEngine(LightType.SKY)).storage, newSet);
            } catch (IllegalAccessException illegalAccessException) {
                illegalAccessException.printStackTrace();
            }
        }
        WorldManager.init(world);
        LightingManager.init(world);
    }
    
    static void onChunkLoad(ChunkEvent.Load e) {
        if(!e.getWorld().isRemote()){
            return;
        }
        ChunkSection[] sections = e.getChunk().getSections();
        for (int i = 0; i < sections.length; i++) {
            // yes, i know this forces there to be all the chunk sections loaded, i may implement lazy loading later
//            sections[i] = new QuartzChunkSection(i * 16);
            
            ChunkPos pos = e.getChunk().getPos();
            LightingManager.loadSection(new Vector3i(pos.x << 4, i << 4, pos.z << 4));
        }
    }
}
