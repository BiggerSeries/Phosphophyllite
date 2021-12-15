package net.roguelogix.phosphophyllite.mixin;

import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.ScreenManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import net.roguelogix.phosphophyllite.quartz.internal.EventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(Window.class)
public class QuartzStartupMixin {
    @Inject(method = "<init>", at=@At(value = "TAIL"))
    public void startQuartz(WindowEventHandler p_85372_, ScreenManager p_85373_, DisplayData p_85374_, @Nullable String p_85375_, String p_85376_, CallbackInfo ci){
        EventListener.initQuartz();
    }
}
