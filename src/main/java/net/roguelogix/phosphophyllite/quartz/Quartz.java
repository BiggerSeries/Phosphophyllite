package net.roguelogix.phosphophyllite.quartz;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.EventBus;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.roguelogix.phosphophyllite.quartz.internal.QuartzCore;
import net.roguelogix.phosphophyllite.repack.org.joml.Matrix4fc;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3fc;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3ic;
import net.roguelogix.phosphophyllite.util.MethodsReturnNonnullByDefault;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class Quartz {
    
    public static EventBus EVENT_BUS = new EventBus(new BusBuilder().setTrackPhases(false).startShutdown());
    
    public static void registerRenderType(RenderType type) {
        QuartzCore.instance().registerRenderType(type);
    }
    
    public static QuartzStaticMesh createStaticMesh(Consumer<QuartzStaticMesh.Builder> buildFunc) {
        return QuartzCore.instance().createStaticMeshInternal(buildFunc);
    }
    
    public static QuartzStaticMesh createStaticMesh(BlockState blockState) {
        return createStaticMesh(builder -> {
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(blockState, builder.matrixStack(), builder.bufferSource(), 0, 0x00000, net.minecraftforge.client.model.data.EmptyModelData.INSTANCE);
        });
    }
    
    public interface DynamicMatrixUpdateFunc {
        void accept(QuartzDynamicMatrix matrix, long nanoSinceLastFrame, float partialTicks, Vector3ic playerBlock, Vector3fc playerPartialBlock);
    }
    
    public static QuartzDynamicMatrix createDynamicMatrix(@Nullable QuartzDynamicMatrix parent, @Nullable DynamicMatrixUpdateFunc updateFunc) {
        return QuartzCore.instance().createDynamicMatrix(parent, updateFunc);
    }
    
    public static QuartzDynamicMatrix createDynamicMatrix(@Nullable DynamicMatrixUpdateFunc updateFunc) {
        return createDynamicMatrix(null, updateFunc);
    }
    
    public static QuartzDynamicMatrix createDynamicMatrix() {
        return createDynamicMatrix(null, null);
    }
    
    public interface DynamicLightUpdateFunc {
        void accept(QuartzDynamicLight light, BlockAndTintGetter blockAndTintGetter);
    }
    
    public static QuartzDynamicLight createDynamicLight(@Nullable DynamicLightUpdateFunc updateFunc) {
        return QuartzCore.instance().createDynamicLight(updateFunc);
    }
    
    public static int registerStaticMeshInstance(QuartzStaticMesh mesh, Vector3ic position, @Nullable QuartzDynamicMatrix dynamicMatrix, Matrix4fc staticTransform, QuartzDynamicLight light) {
        return QuartzCore.instance().registerStaticMeshInstance(mesh, position, dynamicMatrix, staticTransform, light);
    }
    
    public static void unregisterStaticMeshInstance(int handle) {
        QuartzCore.instance().unregisterStaticMeshInstance(handle);
    }
}
