package net.roguelogix.phosphophyllite.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Material;
import net.roguelogix.phosphophyllite.registry.CreativeTabBlock;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;


// "Ore"
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

@CreativeTabBlock
public class PhosphophylliteOre extends Block {
    
    @RegisterBlock(name = "phosphophyllite_ore")
    public static final PhosphophylliteOre INSTANCE = new PhosphophylliteOre();
    
    public PhosphophylliteOre() {
        super(Properties.of(Material.STONE).noDrops().destroyTime(3.0F).explosionResistance(3.0F));
    }
}
