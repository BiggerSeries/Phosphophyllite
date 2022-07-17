package net.roguelogix.phosphophyllite.multiblock2.rectangular;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.roguelogix.phosphophyllite.multiblock2.IMultiblockBlock;

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
