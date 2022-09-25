package net.roguelogix.phosphophyllite.multiblock2.validated;

import net.roguelogix.phosphophyllite.util.NonnullDefault;

@NonnullDefault
public interface IAssembledTickMultiblockModule {
    
    default void preTick() {
    }
    
    default void postTick() {
    }
    
    default void preDisassembledTick() {
    }
    
    default void postDisassembledTick() {
    }
}
