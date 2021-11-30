package net.roguelogix.phosphophyllite.quartz.internal;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.CrashReport;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.roguelogix.phosphophyllite.quartz.*;
import net.roguelogix.phosphophyllite.quartz.internal.common.*;
import net.roguelogix.phosphophyllite.quartz.internal.gl33.GL33Core;
import net.roguelogix.phosphophyllite.registry.ClientOnly;
import net.roguelogix.phosphophyllite.registry.OnModLoad;
import net.roguelogix.phosphophyllite.repack.org.joml.Matrix4f;
import net.roguelogix.phosphophyllite.repack.org.joml.Matrix4fc;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3ic;
import net.roguelogix.phosphophyllite.threading.WorkQueue;
import net.roguelogix.phosphophyllite.util.MethodsReturnNonnullByDefault;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

@ClientOnly
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class QuartzCore implements GLDeletable {
    public static final Logger LOGGER = LogManager.getLogger("Phosphophyllite/Quartz");
    
    private static QuartzCore instance = null;
    
    public static QuartzCore instance() {
        return instance;
    }
    
    private GLCapabilities capabilities;
    private GLMode glMode;
    
    public GLCapabilities capabilities() {
        return capabilities;
    }
    
    public static final WorkQueue nextFrameStart = new WorkQueue();
    
    private enum GLMode {
        //        GL46((capabilities) -> capabilities.OpenGL46, "OpenGL 4.6", GL46Core::new),
        GL33((capabilities) -> capabilities.OpenGL33 && capabilities.GL_ARB_separate_shader_objects, "OpenGL 3.3", GL33Core::new),
        ;
        
        private final Function<GLCapabilities, Boolean> compatibilitySupplier;
        private final String versionString;
        private final Runnable quartCoreConstructor;
        
        GLMode(Function<GLCapabilities, Boolean> supplier, String versionString, Runnable quartCoreConstructor) {
            compatibilitySupplier = supplier;
            this.versionString = versionString;
            this.quartCoreConstructor = quartCoreConstructor;
        }
        
        public boolean contains(GLMode mode) {
            return mode.ordinal() <= this.ordinal();
        }
        
        public boolean supported(GLCapabilities capabilities) {
            return compatibilitySupplier.apply(capabilities);
        }
        
        public String versionString() {
            return versionString;
        }
    }
    
    @OnModLoad
    private static void onModLoad() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(QuartzCore::clientSetup);
    }
    
    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                startup();
            } catch (Exception e) {
                Minecraft.crash(new CrashReport("Quartz failed to startup", e));
            }
        });
    }
    
    public static void startup() {
        if (instance != null) {
            return;
        }
        LOGGER.info("Quartz starting up");
        var capabilities = GL.getCapabilities();
        GLMode glMode;
        {
            GLMode lastTriedMode = GLMode.GL33;
            for (GLMode mode : GLMode.values()) {
                lastTriedMode = mode;
                if (mode.supported(capabilities)) {
                    break;
                }
            }
            if (!lastTriedMode.supported(capabilities)) {
                throw new IllegalStateException("Unable to initialize quartz render system, no compatible GL mode found, " + lastTriedMode.versionString() + " required");
            }
            glMode = lastTriedMode;
            LOGGER.info("GL mode " + glMode.versionString() + " selected");
        }
        glMode.quartCoreConstructor.run();
        instance.glMode = glMode;
        instance.capabilities = capabilities;
        Quartz.EVENT_BUS.start();
        Quartz.EVENT_BUS.post(new QuartzEvent.Startup());
        LOGGER.info("Quartz started up");
    }
    
    public static void shutdown() {
        if (instance == null) {
            return;
        }
        LOGGER.info("Quartz shutting down");
        Quartz.EVENT_BUS.post(new QuartzEvent.Shutdown());
        instance.delete();
        instance = null;
        LOGGER.info("Quartz shut down");
    }
    
    private static final DrawInfo drawInfo = new DrawInfo();
    
    protected final DynamicMatrixManager dynamicMatrixManager;
    protected final DynamicLightManager dynamicLightManager;
    protected final StaticMeshManager staticMeshManager;
    
    public final DynamicMatrixManager.DynamicMatrix DYNAMIC_MATRIX_0;
    
    protected QuartzCore() {
        instance = this;
        dynamicMatrixManager = new DynamicMatrixManager();
        dynamicLightManager = new DynamicLightManager();
        DYNAMIC_MATRIX_0 = (DynamicMatrixManager.DynamicMatrix) dynamicMatrixManager.alloc(null, null);
        DYNAMIC_MATRIX_0.write(new Matrix4f().identity());
        if (DYNAMIC_MATRIX_0.id() != 0) {
            throw new IllegalStateException("Failed to allocate root dynamic matrix");
        }
        staticMeshManager = new StaticMeshManager();
    }
    
    @Override
    public void delete() {
        staticMeshManager.delete();
        dynamicLightManager.delete();
        dynamicMatrixManager.delete();
        drawInfo.delete();
    }
    
    public final QuartzDynamicMatrix createDynamicMatrix(@Nullable QuartzDynamicMatrix parent, @Nullable Quartz.DynamicMatrixUpdateFunc updateFunc) {
        return dynamicMatrixManager.alloc(parent, updateFunc);
    }
    
    public final QuartzStaticMesh createStaticMeshInternal(Consumer<QuartzStaticMesh.Builder> buildFunc) {
        return staticMeshManager.createMesh(buildFunc);
    }
    
    public QuartzDynamicLight createDynamicLight(@Nullable Quartz.DynamicLightUpdateFunc updateFunc) {
        return dynamicLightManager.alloc(updateFunc);
    }
    
    private long lastTimeNano = 0;
    
    public final void drawFirst(PoseStack pMatrixStack, float pPartialTicks, long pFinishTimeNano, boolean pDrawBlockOutline, Camera pActiveRenderInfo, GameRenderer pGameRenderer, LightTexture pLightmap, com.mojang.math.Matrix4f pProjection) {
        
        final var instance = instance();
        
        // yes im keeping track of this myself, I don't care that Mojang does too
        long timeNanos = System.nanoTime();
        long deltaNano = lastTimeNano - timeNanos;
        lastTimeNano = timeNanos;
        if (lastTimeNano == 0) {
            deltaNano = 0;
        }
        
        nextFrameStart.runAll();
        
        var playerPosition = pActiveRenderInfo.getPosition();
        drawInfo.playerPosition.set((int) playerPosition.x, (int) playerPosition.y, (int) playerPosition.z);
        drawInfo.playerSubBlock.set(playerPosition.x - (int) playerPosition.x, playerPosition.y - (int) playerPosition.y, playerPosition.z - (int) playerPosition.z);
        pProjection.store(drawInfo.projectionMatrixFloatBuffer);
        drawInfo.projectionMatrix.set(drawInfo.projectionMatrixFloatBuffer);
        pMatrixStack.last().pose().store(drawInfo.projectionMatrixFloatBuffer);
        drawInfo.projectionMatrix.mul(new Matrix4f().set(drawInfo.projectionMatrixFloatBuffer));
        drawInfo.projectionMatrix.get(drawInfo.projectionMatrixFloatBuffer);
        drawInfo.fogStart = RenderSystem.getShaderFogStart();
        drawInfo.fogEnd = drawInfo.fogStart == Float.MAX_VALUE ? Float.MAX_VALUE : RenderSystem.getShaderFogEnd();
        drawInfo.fogColor.set(RenderSystem.getShaderFogColor());
        
        Quartz.EVENT_BUS.post(new QuartzEvent.FrameStart());
        
        instance.dynamicMatrixManager.updateAll(deltaNano, pPartialTicks, drawInfo.playerPosition, drawInfo.playerSubBlock);
        instance.dynamicLightManager.updateAll();
        instance.drawFirst(drawInfo);
    }
    
    static boolean resourcesLoaded = false;
    
    public static void resourcesReloaded() {
        if (resourcesLoaded) {
            LOGGER.info("Quartz resources reloading");
            Quartz.EVENT_BUS.post(new QuartzEvent.ResourcesReloaded());
            LOGGER.info("Quartz resources reloaded");
        } else {
            LOGGER.info("Quartz resources loading");
            Quartz.EVENT_BUS.post(new QuartzEvent.ResourcesLoaded());
            resourcesLoaded = true;
            LOGGER.info("Quartz resources loaded");
        }
    }
    
    public abstract void registerRenderType(RenderType type);
    
    public abstract int registerStaticMeshInstance(QuartzStaticMesh mesh, Vector3ic position, @Nullable QuartzDynamicMatrix dynamicMatrix, Matrix4fc staticTransform, QuartzDynamicLight light);
    
    public abstract void unregisterStaticMeshInstance(int handle);
    
    public abstract void drawFirst(DrawInfo drawInfo);
    
    public abstract Collection<RenderType> registeredRenderTypes();
    
    public abstract GLBuffer allocBuffer(boolean dynamic);
    
    public abstract GLBuffer allocBuffer(boolean dynamic, int size);
}
