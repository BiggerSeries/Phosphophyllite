package net.roguelogix.phosphophyllite.multiblock.generic;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.items.DebugTool;

import javax.annotation.Nonnull;

import static net.roguelogix.phosphophyllite.multiblock.generic.MultiblockBlock.ASSEMBLED;

public abstract class MultiblockTile<ControllerType extends MultiblockController<ControllerType, TileType, BlockType>, TileType extends MultiblockTile<ControllerType, TileType, BlockType>, BlockType extends MultiblockBlock<ControllerType, TileType, BlockType>> extends TileEntity {
    protected ControllerType controller;
    
    public TileType self() {
        //noinspection unchecked
        return (TileType) this;
    }
    
    long lastSavedTick = 0;
    
    public void attemptAttach() {
        controller = null;
        attemptAttach = true;
        assert world != null;
        if (!world.isRemote) {
            Phosphophyllite.attachTile(this);
        }
    }
    
    private boolean attemptAttach = true;
    private boolean allowAttach = true;
    boolean isSaveDelegate = false;
    
    public MultiblockTile(@Nonnull TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }
    
    public void attachToNeighbors() {
        assert world != null;
        if (allowAttach && attemptAttach && !world.isRemote) {
            attemptAttach = false;
            Block thisBlock = this.getBlockState().getBlock();
            if (!(thisBlock instanceof MultiblockBlock)) {
                // can happen if a block is broken in the same tick it is placed
                return;
            }
            if (((MultiblockBlock<?, ?, ?>) thisBlock).usesAssemblyState()) {
                world.setBlockState(this.pos, this.getBlockState().with(ASSEMBLED, false));
            }
            if (controller != null) {
                controller.detach(self());
                controller = null;
            }
            // at this point, i need to get or create a controller
            BlockPos.Mutable possibleTilePos = new BlockPos.Mutable();
            for (Direction value : Direction.values()) {
                possibleTilePos.setPos(pos);
                possibleTilePos.move(value);
                IChunk chunk = world.getChunk(possibleTilePos.getX() >> 4, possibleTilePos.getZ() >> 4, ChunkStatus.FULL, false);
                if (chunk != null) {
                    TileEntity possibleTile = chunk.getTileEntity(possibleTilePos);
                    if (possibleTile instanceof MultiblockTile) {
                        if (((MultiblockTile<?, ?, ?>) possibleTile).controller != null) {
                            ((MultiblockTile<?, ?, ?>) possibleTile).controller.attemptAttach(this);
                        } else {
                            ((MultiblockTile<?, ?, ?>) possibleTile).attemptAttach = true;
                        }
                    }
                }
            }
            if (controller == null) {
                createController().attemptAttach(self());
            }
        }
    }
    
    @Override
    public void validate() {
        super.validate();
        attemptAttach();
        if (world.isRemote) {
            controllerData = null;
        }
    }
    
    @Override
    public void onLoad() {
        attemptAttach();
    }
    
    @Override
    public final void remove() {
        if (controller != null) {
            controller.detach(self());
        }
        allowAttach = false;
        onRemoved(false);
        super.remove();
    }
    
    @Override
    public final void onChunkUnloaded() {
        if (controller != null) {
            controller.detach(self(), true);
        }
        allowAttach = false;
        onRemoved(true);
        super.onChunkUnloaded();
    }
    
    public void onRemoved(boolean chunkUnload){
    }
    
    @Nonnull
    public abstract ControllerType createController();
    
    protected void readNBT(@Nonnull CompoundNBT compound) {
    }
    
    @Nonnull
    protected CompoundNBT writeNBT() {
        return new CompoundNBT();
    }
    
    boolean preExistingBlock = false;
    CompoundNBT controllerData = null;
    
    @Override
    public final void read(@Nonnull BlockState state, @Nonnull CompoundNBT compound) {
        super.read(state, compound);
        if (compound.contains("controllerData")) {
            controllerData = compound.getCompound("controllerData");
        }
        if (compound.contains("userdata")) {
            readNBT(compound.getCompound("userdata"));
        }
        isSaveDelegate = compound.getBoolean("isSaveDelegate");
        preExistingBlock = true;
    }
    
    
    @Override
    @Nonnull
    public final CompoundNBT write(@Nonnull CompoundNBT compound) {
        super.write(compound);
        if (isSaveDelegate && controller != null && controller.blocks.containsTile(self())) {
            compound.put("controllerData", controller.getNBT());
        }
        compound.put("userdata", writeNBT());
        compound.putBoolean("isSaveDelegate", isSaveDelegate);
        return compound;
    }
    
    protected String getDebugInfo() {
        return controller.getDebugInfo();
    }
    
    @Nonnull
    public ActionResultType onBlockActivated(@Nonnull PlayerEntity player, @Nonnull Hand handIn) {
        if (handIn == Hand.MAIN_HAND) {
            if (player.getHeldItemMainhand() == ItemStack.EMPTY && (!((MultiblockBlock) getBlockState().getBlock()).usesAssemblyState() || !getBlockState().get(ASSEMBLED))) {
                if (controller != null && controller.assemblyState() != MultiblockController.AssemblyState.ASSEMBLED) {
                    if (controller.lastValidationError != null) {
                        player.sendMessage(controller.lastValidationError.getTextComponent(), Util.DUMMY_UUID);
                    } else {
                        player.sendMessage(new TranslationTextComponent("multiblock.error.phosphophyllite.unknown"), Util.DUMMY_UUID);
                    }
                    
                }
                return ActionResultType.SUCCESS;
                
            } else if (player.getHeldItemMainhand().getItem() == DebugTool.INSTANCE) {
                // no its not getting translated, its debug info, *english*
                if (controller != null) {
                    player.sendMessage(new StringTextComponent(getDebugInfo()), Util.DUMMY_UUID);
                } else if (!world.isRemote) {
                    player.sendMessage(new StringTextComponent("null controller on server"), Util.DUMMY_UUID);
                }
                return ActionResultType.SUCCESS;
                
            }
        }
        return ActionResultType.PASS;
    }
    
    protected BlockState assembledBlockState() {
        BlockState state = getBlockState();
        //noinspection unchecked
        if (((BlockType) state.getBlock()).usesAssemblyState()) {
            state = state.with(MultiblockBlock.ASSEMBLED, true);
        }
        return state;
    }
    
    protected BlockState disassembledBlockState() {
        BlockState state = getBlockState();
        //noinspection unchecked
        if (((BlockType) state.getBlock()).usesAssemblyState()) {
            state = state.with(MultiblockBlock.ASSEMBLED, false);
        }
        return state;
    }
}
