package net.roguelogix.phosphophyllite.multiblock2;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.modular.api.IModularTile;
import net.roguelogix.phosphophyllite.modular.api.ModuleRegistry;
import net.roguelogix.phosphophyllite.modular.api.TileModule;
import net.roguelogix.phosphophyllite.modular.tile.IIsTickingTracker;
import net.roguelogix.phosphophyllite.multiblock2.modular.ExtendedMultiblockTileModule;
import net.roguelogix.phosphophyllite.registry.OnModLoad;
import net.roguelogix.phosphophyllite.threading.Queues;
import net.roguelogix.phosphophyllite.util.NonnullDefault;
import net.roguelogix.phosphophyllite.util.Util;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;
import java.util.Objects;

import static net.roguelogix.phosphophyllite.multiblock.IAssemblyStateBlock.ASSEMBLED;
import static net.roguelogix.phosphophyllite.util.Util.DIRECTIONS;

@NonnullDefault
public final class MultiblockTileModule<
        TileType extends BlockEntity & IMultiblockTile<TileType, BlockType, ControllerType>,
        BlockType extends Block & IMultiblockBlock,
        ControllerType extends MultiblockController<TileType, BlockType, ControllerType>
        > extends TileModule<TileType> implements IIsTickingTracker {
    
    @Nullable
    private ControllerType controller;
    
    boolean preExistingBlock = false;
    boolean allowAttach = false;
    
    long lastSavedTick = 0;
    
    @SuppressWarnings("unchecked") // its fine
    final MultiblockTileModule<TileType, BlockType, ControllerType>[] neighbors = new MultiblockTileModule[6];
    final BlockEntity[] neighborTiles = new BlockEntity[6];
    
    private final ObjectArrayList<ExtendedMultiblockTileModule<TileType, BlockType, ControllerType>> extendedMultiblockTileModules = new ObjectArrayList<>();
    
    private final boolean ASSEMBLY_STATE = iface.getBlockState().hasProperty(ASSEMBLED);
    
    @OnModLoad
    private static void onModLoad() {
        ModuleRegistry.registerTileModule(IMultiblockTile.class, MultiblockTileModule::new);
    }
    
    public MultiblockTileModule(IModularTile blockEntity) {
        super(blockEntity);
    }
    
    @Override
    public void postModuleConstruction() {
        for (TileModule<?> module : iface.modules()) {
            if (module instanceof ExtendedMultiblockTileModule<?, ?, ?>) {
                //noinspection unchecked
                extendedMultiblockTileModules.add((ExtendedMultiblockTileModule<TileType, BlockType, ControllerType>) module);
            }
        }
    }
    
    /*
     * There is a potential edge case where a multiblock can have blocks half loaded be broken by non-player means (quarry)
     */
    @Override
    public void startTicking() {
        // this fires on server only
        allowAttach = true;
        attachToNeighborsLater();
    }
    
    @Override
    public void stopTicking() {
        if (controller != null) {
            allowAttach = false;
            // effectively chunk unload, if a player is going to remove it, it will be ticking again when that happens
            controller.detach(this, true, false, true);
        }
    }
    
    @Override
    public void onRemoved(boolean chunkUnload) {
        if (controller != null) {
            controller.detach(this, chunkUnload, false, true);
        }
    }
    
    @Nullable
    public ControllerType controller() {
        return controller;
    }
    
    @Contract
    @Nullable
    ControllerType controller(@Nullable ControllerType newController) {
        if (controller != newController) {
            controller = newController;
            extendedMultiblockTileModules.forEach(ExtendedMultiblockTileModule::onControllerChange);
        }
        return controller;
    }
    
    @Override
    public String saveKey() {
        return "phosphophyllite_multiblock";
    }
    
    @Override
    public void readNBT(CompoundTag nbt) {
        preExistingBlock = true;
    }
    
    @Override
    public CompoundTag writeNBT() {
        return new CompoundTag();
    }
    
    @Contract(pure = true)
    private boolean shouldConnectTo(IMultiblockTile<?, ?, ?> otherRawTile, Direction direction) {
        if(!allowAttach) {
            return false;
        }
        if (this.controller != null) {
            if (!controller.canAttachTile(otherRawTile)) {
                return false;
            }
        }
        if (otherRawTile.nullableController() != null) {
            if (!otherRawTile.controller().canAttachTile(iface)) {
                return false;
            }
        }
        
        // its safe to cast at this point because both (if existing) controllers agree that the they can be attached to each other
        //noinspection unchecked
        final var otherTile = (TileType) otherRawTile;
        //noinspection unchecked
        final var otherModule = (MultiblockTileModule<TileType, BlockType, ControllerType>) otherTile.module(IMultiblockTile.class);
        assert otherModule != null;
        final var oppositeDirection = direction.getOpposite();
        
        for (int i = 0; i < extendedMultiblockTileModules.size(); i++) {
            final var extensionModule = extendedMultiblockTileModules.get(i);
            if (!extensionModule.shouldConnectTo(otherTile, direction)) {
                return false;
            }
        }
        
        for (int i = 0; i < otherModule.extendedMultiblockTileModules.size(); i++) {
            final var extensionModule = otherModule.extendedMultiblockTileModules.get(i);
            if (!extensionModule.shouldConnectTo(iface, oppositeDirection)) {
                return false;
            }
        }
        
        return true;
    }
    
    void updateNeighbors() {
        if (controller == null) {
            return;
        }
        nullNeighbors();
        var pos = iface.getBlockPos();
        for (Direction value : DIRECTIONS) {
            var neighbor = controller.blocks.getModule(pos.getX() + value.getStepX(), pos.getY() + value.getStepY(), pos.getZ() + value.getStepZ());
            if (neighbor == null || !shouldConnectTo(neighbor.iface, value)) {
                continue;
            }
            neighbors[value.get3DDataValue()] = neighbor;
            neighborTiles[value.get3DDataValue()] = neighbor.iface;
        }
        for (int i = 0; i < neighbors.length; i++) {
            MultiblockTileModule<TileType, BlockType, ControllerType> neighbor = neighbors[i];
            if (neighbor != null) {
                neighbor.neighbors[Direction.from3DDataValue(i).getOpposite().get3DDataValue()] = this;
                neighbor.neighborTiles[Direction.from3DDataValue(i).getOpposite().get3DDataValue()] = iface;
            }
        }
    }
    
    void nullNeighbors() {
        for (int i = 0; i < neighbors.length; i++) {
            MultiblockTileModule<?, ?, ?> neighbor = neighbors[i];
            if (neighbor != null) {
                neighbor.neighbors[Direction.from3DDataValue(i).getOpposite().get3DDataValue()] = null;
                neighbor.neighborTiles[Direction.from3DDataValue(i).getOpposite().get3DDataValue()] = null;
            }
            neighbors[i] = null;
            neighborTiles[i] = null;
        }
    }
    
    @Nullable
    public MultiblockTileModule<TileType, BlockType, ControllerType> getNeighbor(Direction direction) {
        return neighbors[direction.get3DDataValue()];
    }
    
    public void attachToNeighborsLater() {
        if (Objects.requireNonNull(iface.getLevel()).isClientSide) {
            return;
        }
        Queues.serverThread.enqueueUntracked(this::attachToNeighborsNow);
    }
    
    public void attachToNeighborsNow() {
        final var level = iface.getLevel();
        assert level != null;
        if (level.isClientSide) {
            return;
        }
        if (iface.isRemoved()) {
            return;
        }
        final var pos = iface.getBlockPos();
        if (iface.getLevel().getBlockEntity(pos) != iface) {
            return;
        }
        extendedMultiblockTileModules.forEach(ExtendedMultiblockTileModule::aboutToAttemptAttach);
        BlockPos.MutableBlockPos possibleTilePos = new BlockPos.MutableBlockPos();
        for (Direction direction : DIRECTIONS) {
            possibleTilePos.set(pos);
            possibleTilePos.move(direction);
            final var tile = Util.getTile(level, possibleTilePos);
            if (tile instanceof IMultiblockTile<?, ?, ?> multiblockTile) {
                final MultiblockTileModule<?, ?, ?> multiblockModule = multiblockTile.multiblockModule();
                if (multiblockModule.controller == null) {
                    continue;
                }
                if (!shouldConnectTo(multiblockTile, direction)) {
                    continue;
                }
                multiblockModule.controller.attemptAttach(this);
            }
        }
        if (controller == null) {
            iface.createController().attemptAttach(this);
        }
    }
    
    @Contract(pure = true)
    BlockState assembledBlockState(BlockState state) {
        if (ASSEMBLY_STATE) {
            state = state.setValue(ASSEMBLED, true);
        }
        // TODO: this is going to have one hell of a performance impact
        for (var extendedMultiblockTileModule : extendedMultiblockTileModules) {
            state = extendedMultiblockTileModule.assembledBlockState(state);
        }
        return state;
    }
    
    @Contract(pure = true)
    BlockState disassembledBlockState(BlockState state) {
        if (ASSEMBLY_STATE) {
            state = state.setValue(ASSEMBLED, false);
        }
        for (var extendedMultiblockTileModule : extendedMultiblockTileModules) {
            state = extendedMultiblockTileModule.disassembledBlockState(state);
        }
        return state;
    }
    
    @Override
    public String getDebugString() {
        var controller = controller();
        //noinspection ConstantConditions
        if (controller == null) {
            return "Null controller";
        }
        return controller.getDebugString();
    }
}
