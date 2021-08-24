package net.roguelogix.phosphophyllite.multiblock;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.modular.api.IModularTile;
import net.roguelogix.phosphophyllite.modular.api.TileModule;
import net.roguelogix.phosphophyllite.modular.api.ModuleRegistry;
import net.roguelogix.phosphophyllite.registry.OnModLoad;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static net.roguelogix.phosphophyllite.multiblock.IAssemblyStateBlock.ASSEMBLED;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MultiblockTileModule<
        TileType extends BlockEntity & IMultiblockTile<TileType, ControllerType>,
        ControllerType extends MultiblockController<TileType, ControllerType>
        > extends TileModule<TileType> {
    
    protected ControllerType controller;
    protected final MultiblockTileModule<?, ?>[] neighbors = new MultiblockTileModule<?, ?>[6];
    
    public ControllerType controller() {
        return controller;
    }
    
    @Nullable
    public MultiblockTileModule<?, ?> getNeighbor(Direction direction) {
        return neighbors[direction.get3DDataValue()];
    }
    
    void updateNeighbors() {
        if (controller == null) {
            return;
        }
        var pos = iface.getBlockPos();
        for (Direction value : Direction.values()) {
            neighbors[value.get3DDataValue()] = controller.blocks.getModule(pos.getX() + value.getStepX(), pos.getY() + value.getStepY(), pos.getZ() + value.getStepZ());
        }
        for (int i = 0; i < neighbors.length; i++) {
            MultiblockTileModule<?, ?> neighbor = neighbors[i];
            if(neighbor != null){
                neighbor.neighbors[Direction.from3DDataValue(i).getOpposite().get3DDataValue()] = this;
            }
        }
    }
    
    void nullNeighbors(){
        for (int i = 0; i < neighbors.length; i++) {
            MultiblockTileModule<?, ?> neighbor = neighbors[i];
            if(neighbor != null){
                neighbor.neighbors[Direction.from3DDataValue(i).getOpposite().get3DDataValue()] = null;
            }
        }
    
        for (Direction value : Direction.values()) {
            neighbors[value.get3DDataValue()] = null;
        }
    }
    
    @OnModLoad
    static void onModLoad() {
        ModuleRegistry.registerTileModule(IMultiblockTile.class, IMultiblockTile::createMultiblockModule);
    }
    
    public MultiblockTileModule(IModularTile blockEntity) {
        super(blockEntity);
    }
    
    @Override
    public String saveKey() {
        return "phosphophyllite_multiblock";
    }
    
    long lastSavedTick = 0;
    
    public void attemptAttach() {
        controller = null;
        attemptAttach = true;
        assert iface.getLevel() != null;
        if (!iface.getLevel().isClientSide) {
            Phosphophyllite.attachTile(this);
        }
    }
    
    private boolean attemptAttach = true;
    private boolean allowAttach = true;
    boolean isSaveDelegate = false;
    
    protected BlockState assembledBlockState() {
        BlockState state = iface.getBlockState();
        if (state.hasProperty(ASSEMBLED)) {
            state = state.setValue(ASSEMBLED, true);
        }
        return state;
    }
    
    protected BlockState disassembledBlockState() {
        BlockState state = iface.getBlockState();
        if (state.hasProperty(ASSEMBLED)) {
            state = state.setValue(ASSEMBLED, false);
        }
        return state;
    }
    
    public void attachToNeighbors() {
        assert iface.getLevel() != null;
        if (allowAttach && attemptAttach && !iface.getLevel().isClientSide) {
            attemptAttach = false;
            if (iface.getLevel().getBlockEntity(iface.getBlockPos()) != iface) {
                return;
            }
            
            if (iface.getBlockState().hasProperty(ASSEMBLED)) {
                iface.getLevel().setBlockAndUpdate(iface.getBlockPos(), iface.getBlockState().setValue(ASSEMBLED, false));
            }
            if (controller != null) {
                controller.detach(this);
                controller = null;
            }
            // at this point, i need to get or create a controller
            BlockPos.MutableBlockPos possibleTilePos = new BlockPos.MutableBlockPos();
            for (Direction value : Direction.values()) {
                possibleTilePos.set(iface.getBlockPos());
                possibleTilePos.move(value);
                ChunkAccess chunk = iface.getLevel().getChunk(possibleTilePos.getX() >> 4, possibleTilePos.getZ() >> 4, ChunkStatus.FULL, false);
                if (chunk != null) {
                    BlockEntity possibleTile = chunk.getBlockEntity(possibleTilePos);
                    if (possibleTile instanceof IMultiblockTile) {
                        if (((IMultiblockTile<?, ?>) possibleTile).multiblockModule().controller != null) {
                            ((IMultiblockTile<?, ?>) possibleTile).multiblockModule().controller.attemptAttach(this);
                        } else {
                            ((IMultiblockTile<?, ?>) possibleTile).multiblockModule().attemptAttach = true;
                        }
                    }
                }
            }
            if (controller == null) {
                iface.createController().attemptAttach(this);
            }
        }
    }
    
    boolean preExistingBlock = false;
    CompoundTag controllerData = null;
    
    @Override
    public void readNBT(CompoundTag compound) {
        if (compound.contains("controllerData")) {
            controllerData = compound.getCompound("controllerData");
        }
        if (compound.contains("userdata")) {
            readNBT(compound.getCompound("userdata"));
        }
        isSaveDelegate = compound.getBoolean("isSaveDelegate");
        preExistingBlock = true;
    }
    
    @Nullable
    @Override
    public CompoundTag writeNBT() {
        if (isSaveDelegate && controller != null && controller.blocks.containsModule(this)) {
            CompoundTag compound = new CompoundTag();
            compound.put("controllerData", controller.getNBT());
            compound.putBoolean("isSaveDelegate", isSaveDelegate);
            return compound;
        }
        return null;
    }
    
    @Override
    public void onAdded() {
        assert iface.getLevel() != null;
        attemptAttach();
        if (iface.getLevel().isClientSide) {
            controllerData = null;
        }
    }
    
    @Override
    public void onRemoved(boolean chunkUnload) {
        if (controller != null) {
            controller.detach(this, chunkUnload);
        }
        allowAttach = false;
    }
    
    @Nullable
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
