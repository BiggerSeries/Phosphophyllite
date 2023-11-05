package net.roguelogix.phosphophyllite.mixin.helpers;

import net.neoforged.neoforge.common.util.LazyOptional;
import net.neoforged.neoforge.common.util.NonNullConsumer;

public interface IPhosphophylliteLazyOptional<T> {
    void removeListener(NonNullConsumer<LazyOptional<T>> listener);
}
