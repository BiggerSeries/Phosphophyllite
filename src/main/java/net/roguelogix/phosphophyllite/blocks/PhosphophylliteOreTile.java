package net.roguelogix.phosphophyllite.blocks;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.modular.tile.PhosphophylliteTile;
import net.roguelogix.phosphophyllite.registry.RegisterTile;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PhosphophylliteOreTile extends PhosphophylliteTile {
    
    @RegisterTile("phosphophyllite_ore")
    public static final BlockEntityType.BlockEntitySupplier<PhosphophylliteOreTile> SUPPLIER = new RegisterTile.Producer<>(PhosphophylliteOreTile::new);
    
    public PhosphophylliteOreTile(BlockEntityType<?> TYPE, BlockPos pWorldPosition, BlockState pBlockState) {
        super(TYPE, pWorldPosition, pBlockState);
    }
}
