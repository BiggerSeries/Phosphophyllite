package net.roguelogix.phosphophyllite.mixin;

import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ChunkMap.class)
public class ChunkMapMixin {
    @Inject(
            method = "processUnloads",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ChunkMap;saveChunkIfNeeded(Lnet/minecraft/server/level/ChunkHolder;)Z",
                    ordinal = 0
            ),
            cancellable = true
    )
    private void processUnloads(BooleanSupplier p_140354_, CallbackInfo ci) {
        if (ci.isCancellable()) {
            ci.cancel();
        }
    }
}
