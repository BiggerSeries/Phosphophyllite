package net.roguelogix.phosphophyllite.multiblock2.rectangular;

import net.roguelogix.phosphophyllite.multiblock2.validated.IValidatedMultiblockBlock;
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
