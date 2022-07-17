package net.roguelogix.phosphophyllite.multiblock2.test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.roguelogix.phosphophyllite.modular.block.PhosphophylliteBlock;
import net.roguelogix.phosphophyllite.multiblock2.IMultiblockBlock;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

import javax.annotation.Nullable;

@NonnullDefault
public class TestMultiblockBlock extends PhosphophylliteBlock implements IMultiblockBlock {
    
    @RegisterBlock(name = "test_multiblock_block", tileEntityClass = TestMultiblockTile.class)
    public static final TestMultiblockBlock TEST_BLOCK = new TestMultiblockBlock(Properties.of(Material.METAL));
    
    public TestMultiblockBlock(Properties properties) {
        super(properties);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TestMultiblockTile.SUPPLIER.create(pos, state);
    }
}
