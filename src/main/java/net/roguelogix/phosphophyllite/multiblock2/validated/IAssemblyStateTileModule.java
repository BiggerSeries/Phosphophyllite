package net.roguelogix.phosphophyllite.multiblock2.validated;

import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.util.NonnullDefault;
import org.jetbrains.annotations.Contract;

@NonnullDefault
public interface IAssemblyStateTileModule {
    @Contract(pure = true)
    default BlockState assembledBlockState(BlockState state) {
        return state;
    }
    
    @Contract(pure = true)
    default BlockState disassembledBlockState(BlockState state) {
        return state;
    }
}
