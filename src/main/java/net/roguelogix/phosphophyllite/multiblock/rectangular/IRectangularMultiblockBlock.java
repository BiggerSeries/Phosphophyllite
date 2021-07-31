package net.roguelogix.phosphophyllite.multiblock.rectangular;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.roguelogix.phosphophyllite.multiblock.IMultiblockBlock;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface IRectangularMultiblockBlock extends IMultiblockBlock {
    boolean isGoodForInterior();
    
    boolean isGoodForExterior();
    
    boolean isGoodForFrame();
    
    default boolean isGoodForCorner() {
        return isGoodForFrame();
    }
}
