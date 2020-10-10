package net.roguelogix.phosphophyllite.quartz.internal.mixin;


import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.util.math.BlockPos;
import net.roguelogix.phosphophyllite.quartz.internal.management.EventHandling;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChunkRenderDispatcher.ChunkRender.class)
public class RenderChunkMixin {
    
    @Inject(method = "clearNeedsUpdate", at = @At("RETURN"))
    private void clearNeedsUpdateMixin(CallbackInfo ci) {
        //noinspection ConstantConditions
        BlockPos pos = ((ChunkRenderDispatcher.ChunkRender) (Object) this).getPosition();
        EventHandling.onChunkRebuild(new Vector3i(pos.getX(), pos.getY(), pos.getZ()));
    }
}
