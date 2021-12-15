package net.roguelogix.phosphophyllite.quartz.internal;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.roguelogix.phosphophyllite.quartz.DrawBatch;
import net.roguelogix.phosphophyllite.quartz.Quartz;
import net.roguelogix.phosphophyllite.quartz.QuartzConfig;
import net.roguelogix.phosphophyllite.quartz.QuartzEvent;
import net.roguelogix.phosphophyllite.quartz.internal.common.LightEngine;
import net.roguelogix.phosphophyllite.quartz.internal.common.Mesh;
import net.roguelogix.phosphophyllite.quartz.internal.gl.GLCore;
import net.roguelogix.phosphophyllite.quartz.internal.world.WorldEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.Cleaner;

public abstract class QuartzCore {
    
    public static final Logger LOGGER = LogManager.getLogger("Phosphophyllite/Quartz");
    
    @Nonnull
    public static final QuartzCore INSTANCE;
    public static final Cleaner CLEANER = Cleaner.create();
    
    static {
        if (!Thread.currentThread().getStackTrace()[2].getClassName().equals(EventListener.class.getName())) {
            throw new IllegalStateException("Attempt to init quartz before it is ready");
        }
        LOGGER.info("Quartz Init");
        var instance = createCore(QuartzConfig.INSTANCE.mode);
        if (instance == null) {
            throw new IllegalStateException();
        }
        INSTANCE = instance;
    }
    
    @Nullable
    private static QuartzCore createCore(QuartzConfig.Mode mode) {
        return switch (mode) {
            case Vulkan10 -> null; // VK is currently unsupported
            case OpenGL33 -> GLCore.INSTANCE;
            case Automatic -> {
                for (QuartzConfig.Mode value : QuartzConfig.Mode.values()) {
                    if (value == QuartzConfig.Mode.Automatic) {
                        yield null;
                    }
                    final var core = createCore(value);
                    if (core != null) {
                        yield core;
                    }
                }
                yield null;
            }
        };
    }
    
    static void init() {
    }
    
    private static boolean wasInit = false;
    
    static void startup() {
        INSTANCE.startupInternal();
        Quartz.EVENT_BUS.post(new QuartzEvent.Startup());
        wasInit = true;
    }
    
    protected abstract void startupInternal();
    
    public static void shutdown() {
        if(!wasInit){
            return;
        }
        Quartz.EVENT_BUS.post(new QuartzEvent.Shutdown());
        INSTANCE.shutdownInternal();
    }
    
    protected abstract void shutdownInternal();
    
    public static void resourcesReloaded() {
        INSTANCE.meshManager.buildAllMeshes();
        INSTANCE.resourcesReloadedInternal();
    }
    
    protected abstract void resourcesReloadedInternal();
    
    public final WorldEngine worldEngine = new WorldEngine();
    public final LightEngine lightEngine = new LightEngine();
    public final Mesh.Manager meshManager = new Mesh.Manager(allocBuffer());
    
    public WorldEngine getWorldEngine() {
        return worldEngine;
    }
    
    public abstract DrawBatch createDrawBatch();
    
    public abstract Buffer allocBuffer();
    
    public abstract void frameStart(PoseStack pMatrixStack, float pPartialTicks, long pFinishTimeNano, boolean pDrawBlockOutline, Camera pActiveRenderInfo, GameRenderer pGameRenderer, LightTexture pLightmap, Matrix4f pProjection);
    
    /**
     * This is abstract because VK can handle the writes being done on a separate thread
     */
    public abstract void lightUpdated();
    
    public abstract void preTerrainSetup();
    
    public abstract void preOpaque();
    
    public abstract void endOpaque();
    
    public abstract void endTranslucent();
}
