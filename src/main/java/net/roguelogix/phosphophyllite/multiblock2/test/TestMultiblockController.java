package net.roguelogix.phosphophyllite.multiblock2.test;

import net.minecraft.world.level.Level;
import net.roguelogix.phosphophyllite.multiblock.Validator;
import net.roguelogix.phosphophyllite.multiblock2.IMultiblockBlock;
import net.roguelogix.phosphophyllite.multiblock2.IMultiblockTile;
import net.roguelogix.phosphophyllite.multiblock2.MultiblockController;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

@NonnullDefault
public class TestMultiblockController extends MultiblockController<TestMultiblockTile, TestMultiblockController> {
    public TestMultiblockController(Level level) {
        super(level, tile -> tile instanceof TestMultiblockTile, block -> block instanceof TestMultiblockBlock);
    }
}
