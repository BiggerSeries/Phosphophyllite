package net.roguelogix.phosphophyllite.registry;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public interface TileSupplier<T extends BlockEntity> extends BlockEntityType.BlockEntitySupplier<T> {
}
