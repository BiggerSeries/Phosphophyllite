package net.roguelogix.phosphophyllite.mixin.helpers;

import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullConsumer;

public interface IPhosphophylliteLazyOptional<T> {
    void removeListener(NonNullConsumer<LazyOptional<T>> listener);
}
