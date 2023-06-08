package net.roguelogix.phosphophyllite.blocks;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.registry.CreativeTabBlock;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PhosphophylliteOre extends Block implements EntityBlock {
    
    @CreativeTabBlock
    @RegisterBlock(name = "phosphophyllite_ore",  tileEntityClass = PhosphophylliteOreTile.class)
    public static final PhosphophylliteOre INSTANCE = new PhosphophylliteOre();
    
    public PhosphophylliteOre() {
        super(Properties.of().noLootTable().destroyTime(3.0F).explosionResistance(3.0F));
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return PhosphophylliteOreTile.SUPPLIER.create(pPos, pState);
    }
}
