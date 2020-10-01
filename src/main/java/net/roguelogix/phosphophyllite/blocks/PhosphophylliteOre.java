package net.roguelogix.phosphophyllite.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;
import net.roguelogix.phosphophyllite.registry.CreativeTabBlock;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;


// "Ore"
@CreativeTabBlock
@RegisterBlock(name = "phosphophyllite_ore", tileEntityClass = PhosphophylliteOreTile.class)
public class PhosphophylliteOre extends Block {
    
    @RegisterBlock.Instance
    public static PhosphophylliteOre INSTANCE;
    
    public PhosphophylliteOre() {
        super(Properties.create(Material.ROCK).noDrops().hardnessAndResistance(3.0F, 3.0F));
    }
    
    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
    }
    
    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }
    
    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new PhosphophylliteOreTile();
    }
}
