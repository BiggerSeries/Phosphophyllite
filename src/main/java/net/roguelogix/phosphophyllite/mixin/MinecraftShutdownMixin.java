package net.roguelogix.phosphophyllite.mixin;

import net.minecraft.client.Minecraft;
import net.roguelogix.phosphophyllite.quartz.internal.QuartzCore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftShutdownMixin {
    @Inject(method = "destroy", at = @At("HEAD"))
    public void destroy(CallbackInfo ci) {
        QuartzCore.shutdown();
    }
    
    @Inject(method = "stop", at = @At("HEAD"))
    public void stop(CallbackInfo ci) {
        QuartzCore.shutdown();
    }
}
