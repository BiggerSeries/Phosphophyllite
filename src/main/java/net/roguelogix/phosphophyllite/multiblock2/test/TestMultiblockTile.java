package net.roguelogix.phosphophyllite.multiblock2.test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.modular.tile.PhosphophylliteTile;
import net.roguelogix.phosphophyllite.multiblock2.IMultiblockTile;
import net.roguelogix.phosphophyllite.registry.RegisterTile;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

@NonnullDefault
public class TestMultiblockTile extends PhosphophylliteTile implements IMultiblockTile<TestMultiblockTile, TestMultiblockController> {
    
    @RegisterTile("test_multiblock_tile")
    public static final BlockEntityType.BlockEntitySupplier<TestMultiblockTile> SUPPLIER = new RegisterTile.Producer<>(TestMultiblockTile::new);
    
    public TestMultiblockTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }
    
    @Override
    public TestMultiblockController createController() {
        return new TestMultiblockController(level);
    }
}
