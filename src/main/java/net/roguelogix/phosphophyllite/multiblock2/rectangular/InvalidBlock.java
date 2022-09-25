package net.roguelogix.phosphophyllite.multiblock2.rectangular;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.roguelogix.phosphophyllite.multiblock2.ValidationException;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3ic;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

@NonnullDefault
@SuppressWarnings("unused")
public class InvalidBlock extends ValidationException {
    
    public InvalidBlock(String s) {
        super(s);
    }
    
    public InvalidBlock(Block block, Vector3ic worldPosition, String multiblockPosition) {
        super(Component.translatable(
                "multiblock.error.phosphophyllite.invalid_block." + multiblockPosition,
                Component.translatable(block.getDescriptionId()),
                "(x: " + worldPosition.x() + "; y: " + worldPosition.y() + "; z: " + worldPosition.z() + ")")
        );
    }
}
