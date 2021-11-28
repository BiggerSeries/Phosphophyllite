package net.roguelogix.phosphophyllite.mixin;

import net.minecraftforge.fmllegacy.NoVizFallback;
import net.roguelogix.phosphophyllite.mixinhelpers.OpenGLSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

@Mixin(NoVizFallback.class)
public class NoVizFallbackMixin {
    @Inject(method = "fallback",at = @At("HEAD"), remap = false, cancellable = true)
    private static void fallback(IntSupplier width, IntSupplier height, Supplier<String> title, LongSupplier monitor, CallbackInfoReturnable<LongSupplier> cir) {
//        boolean doGlSearch = Boolean.parseBoolean(System.getProperty("phosphophyllite.glsearch", "true"));
//        if(!doGlSearch) {
            cir.setReturnValue(OpenGLSelector.fallback(width, height, title, monitor));
//        }
    }
}
