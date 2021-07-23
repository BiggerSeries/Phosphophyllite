package net.roguelogix.phosphophyllite.multiblock.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static net.roguelogix.phosphophyllite.multiblock.generic.ConnectedTextureStates.*;

public abstract class MultiblockBlock<ControllerType extends MultiblockController<ControllerType, TileType, BlockType>, TileType extends MultiblockTile<ControllerType, TileType, BlockType>, BlockType extends MultiblockBlock<ControllerType, TileType, BlockType>> extends Block implements EntityBlock {
    
    public static final BooleanProperty ASSEMBLED = BooleanProperty.create("assembled");
    
    public MultiblockBlock(Properties properties) {
        super(properties);
        BlockState defaultState = this.defaultBlockState();
        if(usesAssemblyState()) {
            defaultState = defaultState.setValue(ASSEMBLED, false);
        }
        if (connectedTexture()) {
            defaultState = defaultState.setValue(TOP_CONNECTED_PROPERTY, false);
            defaultState = defaultState.setValue(BOTTOM_CONNECTED_PROPERTY, false);
            defaultState = defaultState.setValue(NORTH_CONNECTED_PROPERTY, false);
            defaultState = defaultState.setValue(SOUTH_CONNECTED_PROPERTY, false);
            defaultState = defaultState.setValue(EAST_CONNECTED_PROPERTY, false);
            defaultState = defaultState.setValue(WEST_CONNECTED_PROPERTY, false);
        }
        this.registerDefaultState(defaultState);
    }
    
    public boolean connectedTexture() {
        return false;
    }
    
    public boolean usesAssemblyState(){
        return true;
    }
    
    @SuppressWarnings("deprecation")
    @Nonnull
    @Override
    public InteractionResult use(@Nonnull BlockState state, Level worldIn, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        BlockEntity te = worldIn.getBlockEntity(pos);
        if (te instanceof MultiblockTile) {
            InteractionResult tileResult = ((MultiblockTile) te).onBlockActivated(player, handIn);
            if (tileResult != InteractionResult.PASS) {
                return tileResult;
            }
        }
        return super.use(state, worldIn, pos, player, handIn, hit);
    }
    
    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        if(usesAssemblyState()) {
            builder.add(ASSEMBLED);
        }
        if (connectedTexture()) {
            builder.add(TOP_CONNECTED_PROPERTY);
            builder.add(BOTTOM_CONNECTED_PROPERTY);
            builder.add(NORTH_CONNECTED_PROPERTY);
            builder.add(SOUTH_CONNECTED_PROPERTY);
            builder.add(EAST_CONNECTED_PROPERTY);
            builder.add(WEST_CONNECTED_PROPERTY);
        }
    }
    
    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level worldIn, @Nonnull BlockPos pos, @Nonnull Block blockIn, @Nonnull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, worldIn, pos, blockIn, fromPos, isMoving);
        if (connectedTexture()) {
            updateConnectedTextureState(worldIn, pos, state);
        }
    }
    
    @Override
    public void setPlacedBy(@Nonnull Level worldIn, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nullable LivingEntity placer, @Nonnull ItemStack stack) {
        super.setPlacedBy(worldIn, pos, state, placer, stack);
        if (connectedTexture()) {
            updateConnectedTextureState(worldIn, pos, this.defaultBlockState());
        }
    }
    
    private void updateConnectedTextureState(@Nonnull Level worldIn, @Nonnull BlockPos pos, @Nonnull BlockState state) {
        state = state.setValue(TOP_CONNECTED_PROPERTY, connectToBlock(worldIn.getBlockState(pos.relative(Direction.UP)).getBlock()));
        state = state.setValue(BOTTOM_CONNECTED_PROPERTY, connectToBlock(worldIn.getBlockState(pos.relative(Direction.DOWN)).getBlock()));
        state = state.setValue(NORTH_CONNECTED_PROPERTY, connectToBlock(worldIn.getBlockState(pos.relative(Direction.NORTH)).getBlock()));
        state = state.setValue(SOUTH_CONNECTED_PROPERTY, connectToBlock(worldIn.getBlockState(pos.relative(Direction.SOUTH)).getBlock()));
        state = state.setValue(EAST_CONNECTED_PROPERTY, connectToBlock(worldIn.getBlockState(pos.relative(Direction.EAST)).getBlock()));
        state = state.setValue(WEST_CONNECTED_PROPERTY, connectToBlock(worldIn.getBlockState(pos.relative(Direction.WEST)).getBlock()));
        worldIn.setBlock(pos, state, 2);
    }
    
    protected boolean connectToBlock(Block block){
        return block == this;
    }
}
