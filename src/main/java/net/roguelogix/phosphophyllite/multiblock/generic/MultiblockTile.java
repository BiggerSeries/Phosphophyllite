package net.roguelogix.phosphophyllite.multiblock.generic;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.items.DebugTool;

import javax.annotation.Nonnull;

import static net.roguelogix.phosphophyllite.multiblock.generic.MultiblockBlock.ASSEMBLED;

public abstract class MultiblockTile<ControllerType extends MultiblockController<ControllerType, TileType, BlockType>, TileType extends MultiblockTile<ControllerType, TileType, BlockType>, BlockType extends MultiblockBlock<ControllerType, TileType, BlockType>> extends BlockEntity {
    protected ControllerType controller;
    
    public TileType self() {
        //noinspection unchecked
        return (TileType) this;
    }
    
    long lastSavedTick = 0;
    
    public void attemptAttach() {
        controller = null;
        attemptAttach = true;
        assert level != null;
        if (!level.isClientSide) {
            Phosphophyllite.attachTile(this);
        }
    }
    
    private boolean attemptAttach = true;
    private boolean allowAttach = true;
    boolean isSaveDelegate = false;
    
    public MultiblockTile(@Nonnull BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }
    
    public void attachToNeighbors() {
        assert level != null;
        if (allowAttach && attemptAttach && !level.isClientSide) {
            attemptAttach = false;
            Block thisBlock = this.getBlockState().getBlock();
            if (!(thisBlock instanceof MultiblockBlock)) {
                // can happen if a block is broken in the same tick it is placed
                return;
            }
            if (((MultiblockBlock<?, ?, ?>) thisBlock).usesAssemblyState()) {
                level.setBlock(this.worldPosition, this.getBlockState().setValue(ASSEMBLED, false), 3);
            }
            if (controller != null) {
                controller.detach(self());
                controller = null;
            }
            // at this point, i need to get or create a controller
            BlockPos.MutableBlockPos possibleTilePos = new BlockPos.MutableBlockPos();
            for (Direction value : Direction.values()) {
                possibleTilePos.set(worldPosition);
                possibleTilePos.move(value);
                ChunkAccess chunk = level.getChunk(possibleTilePos.getX() >> 4, possibleTilePos.getZ() >> 4, ChunkStatus.FULL, false);
                if (chunk != null) {
                    BlockEntity possibleTile = chunk.getBlockEntity(possibleTilePos);
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
    public void clearRemoved() {
        super.clearRemoved();
        attemptAttach();
        if (level.isClientSide) {
            controllerData = null;
        }
    }
    
    @Override
    public void onLoad() {
        attemptAttach();
    }
    
    
    @Override
    public final void setRemoved() {
        if (controller != null) {
            controller.detach(self());
        }
        allowAttach = false;
        onRemoved(false);
        super.setRemoved();
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
    
    public void onRemoved(boolean chunkUnload) {
    }
    
    @Nonnull
    public abstract ControllerType createController();
    
    protected void readNBT(@Nonnull CompoundTag compound) {
    }
    
    @Nonnull
    protected CompoundTag writeNBT() {
        return new CompoundTag();
    }
    
    boolean preExistingBlock = false;
    CompoundTag controllerData = null;
    
    @Override
    public final void load(@Nonnull CompoundTag compound) {
        super.load(compound);
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
    public final CompoundTag save(@Nonnull CompoundTag compound) {
        super.save(compound);
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
    public InteractionResult onBlockActivated(@Nonnull Player player, @Nonnull InteractionHand handIn) {
        if (handIn == InteractionHand.MAIN_HAND) {
            if (player.getMainHandItem() == ItemStack.EMPTY && (!((MultiblockBlock) getBlockState().getBlock()).usesAssemblyState() || !getBlockState().getValue(ASSEMBLED))) {
                if (controller != null && controller.assemblyState() != MultiblockController.AssemblyState.ASSEMBLED) {
                    if (controller.lastValidationError != null) {
                        player.sendMessage(controller.lastValidationError.getTextComponent(), Util.NIL_UUID);
                    } else {
                        player.sendMessage(new TranslatableComponent("multiblock.error.phosphophyllite.unknown"), Util.NIL_UUID);
                    }
                    
                }
                return InteractionResult.SUCCESS;
                
            } else if (player.getMainHandItem().getItem() == DebugTool.INSTANCE) {
                // no its not getting translated, its debug info, *english*
                if (controller != null) {
                    player.sendMessage(new TextComponent(getDebugInfo()), Util.NIL_UUID);
                } else if (!level.isClientSide) {
                    player.sendMessage(new TextComponent("null controller on server"), Util.NIL_UUID);
                }
                return InteractionResult.SUCCESS;
                
            }
        }
        return InteractionResult.PASS;
    }
    
    protected BlockState assembledBlockState() {
        BlockState state = getBlockState();
        //noinspection unchecked
        if (((BlockType) state.getBlock()).usesAssemblyState()) {
            state = state.setValue(MultiblockBlock.ASSEMBLED, true);
        }
        return state;
    }
    
    protected BlockState disassembledBlockState() {
        BlockState state = getBlockState();
        //noinspection unchecked
        if (((BlockType) state.getBlock()).usesAssemblyState()) {
            state = state.setValue(MultiblockBlock.ASSEMBLED, false);
        }
        return state;
    }
}
