package net.roguelogix.phosphophyllite.mixin.mixins;

import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullConsumer;
import net.roguelogix.phosphophyllite.mixin.helpers.IPhosphophylliteLazyOptional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(LazyOptional.class)
public class LazyOptionalMixin<T> implements IPhosphophylliteLazyOptional<T> {
    
    @Shadow
    private Set<NonNullConsumer<LazyOptional<T>>> listeners;
    
    @Override
    public void removeListener(NonNullConsumer<LazyOptional<T>> listener) {
        listeners.remove(listener);
    }
}
