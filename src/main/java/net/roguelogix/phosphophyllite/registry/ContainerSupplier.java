package net.roguelogix.phosphophyllite.registry;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;

public interface ContainerSupplier {
    @Nonnull
    Container create(int windowId, BlockPos blockPos, PlayerEntity player);
}
