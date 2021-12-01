//package net.roguelogix.phosphophyllite.mixin;
//
//import net.minecraft.client.ResourceLoadStateTracker;
//import net.roguelogix.phosphophyllite.quartz.internal.QuartzCore;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//@Mixin(ResourceLoadStateTracker.class)
//public class ResourceReloadListener {
//    @Inject(method = "finishReload", at = @At("HEAD"))
//    public void finishReload(CallbackInfo ci) {
//        QuartzCore.resourcesReloaded();
//    }
//}
