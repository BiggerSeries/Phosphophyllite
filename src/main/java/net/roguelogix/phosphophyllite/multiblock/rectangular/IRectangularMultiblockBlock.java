package net.roguelogix.phosphophyllite.multiblock.rectangular;

import net.roguelogix.phosphophyllite.multiblock.validated.IValidatedMultiblockBlock;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

@NonnullDefault
public interface IRectangularMultiblockBlock extends IValidatedMultiblockBlock {
    boolean isGoodForInterior();
    
    boolean isGoodForExterior();
    
    default boolean isGoodForFrame() {
        return isGoodForExterior();
    }
    
    default boolean isGoodForCorner() {
        return isGoodForFrame();
    }
}
