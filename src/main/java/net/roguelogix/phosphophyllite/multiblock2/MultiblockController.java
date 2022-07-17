package net.roguelogix.phosphophyllite.multiblock2;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.debug.IDebuggable;
import net.roguelogix.phosphophyllite.modular.api.TileModule;
import net.roguelogix.phosphophyllite.multiblock.Validator;
import net.roguelogix.phosphophyllite.multiblock2.modular.IModularMultiblockController;
import net.roguelogix.phosphophyllite.multiblock2.modular.MultiblockControllerModule;
import net.roguelogix.phosphophyllite.multiblock2.modular.MultiblockControllerModuleRegistry;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3ic;
import net.roguelogix.phosphophyllite.util.*;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static net.roguelogix.phosphophyllite.util.Util.DIRECTIONS;

@NonnullDefault
public class MultiblockController<
        TileType extends BlockEntity & IMultiblockTile<TileType, ControllerType>,
        ControllerType extends MultiblockController<TileType, ControllerType>
        > implements IModularMultiblockController<TileType, ControllerType>, IDebuggable {
    
    public enum AssemblyState {
        ASSEMBLED,
        DISASSEMBLED,
        // TODO: it would be good if this could be part of a module alone and not the core controller
        PAUSED,
    }
    
    public final Level level;
    @SuppressWarnings("unchecked")
    protected final ModuleMap<MultiblockTileModule<TileType, ControllerType>, TileType> blocks = new ModuleMap<MultiblockTileModule<TileType, ControllerType>, TileType>(new MultiblockTileModule[0]);
    
    public final Validator<BlockEntity> tileTypeValidator;
    public final Validator<Block> blockTypeValidator;
    
    private long updateAssemblyAtTick = Long.MAX_VALUE;
    protected AssemblyState assemblyState = AssemblyState.DISASSEMBLED;
    
    @Nullable
    protected ValidationException lastValidationError = null;
    
    private boolean updateExtremes = true;
    private final Vector3i minCoord = new Vector3i();
    private final Vector3i maxCoord = new Vector3i();
    private final Vector3i minExtremeBlocks = new Vector3i();
    private final Vector3i maxExtremeBlocks = new Vector3i();
    
    private long lastTick = -1;
    private long lastFullTick = -1;
    private long checkForDetachmentsAtTick = Long.MAX_VALUE;
    
    private record Detachment(BlockPos pos, byte directions) {
        Detachment(BlockPos pos, BlockEntity[] neighbors) {
            this(pos, arrayToByte(neighbors));
        }
        
        public boolean linked(Direction direction) {
            return (directions & (1 << direction.get3DDataValue())) != 0;
        }
        
        private static byte arrayToByte(BlockEntity[] neighbors) {
            byte directions = 0;
            for (int i = 0; i < neighbors.length; i++) {
                if (neighbors[i] != null) {
                    directions |= 1 << i;
                }
            }
            return directions;
        }
    }
    
    protected final List<Detachment> removedBlocks = new LinkedList<>();
    
    @Nullable
    private MultiblockController<TileType, ControllerType> mergedInto = null;
    protected final Set<MultiblockController<TileType, ControllerType>> controllersToMerge = new ObjectOpenHashSet<>();
    
    public MultiblockController(Level level, Validator<BlockEntity> tileTypeValidator, Validator<Block> blockTypeValidator) {
        this.level = level;
        this.tileTypeValidator = tileTypeValidator;
        this.blockTypeValidator = blockTypeValidator;
        MultiblockRegistry.addController(this);
        
        final var moduleList = new ArrayList<MultiblockControllerModule<TileType, ControllerType>>();
        moduleListRO = Collections.unmodifiableList(moduleList);
        
        final Class<?> thisClazz = this.getClass();
        MultiblockControllerModuleRegistry.forEach((clazz, constructor) -> {
            if (clazz.isAssignableFrom(thisClazz)) {
                //noinspection unchecked
                var module = (MultiblockControllerModule<TileType, ControllerType>) constructor.apply(this);
                modules.put(clazz, module);
                moduleList.add(module);
            }
        });
        moduleListRO.forEach(MultiblockControllerModule::postModuleConstruction);
    }
    
    ControllerType self() {
        //noinspection unchecked
        return (ControllerType) this;
    }
    
    public final Vector3ic min() {
        return minCoord;
    }
    
    public final Vector3ic max() {
        return maxCoord;
    }
    
    private final Object2ObjectOpenHashMap<Class<?>, MultiblockControllerModule<TileType, ControllerType>> modules = new Object2ObjectOpenHashMap<>();
    private final List<MultiblockControllerModule<TileType, ControllerType>> moduleListRO;
    
    @Override
    @Nullable
    public MultiblockControllerModule<TileType, ControllerType> module(Class<?> interfaceClazz) {
        return modules.get(interfaceClazz);
    }
    
    @Override
    public List<MultiblockControllerModule<TileType, ControllerType>> modules() {
        return moduleListRO;
    }
    
    public AssemblyState assemblyState() {
        return assemblyState;
    }
    
    @Nullable
    public MultiblockTileModule<TileType, ControllerType> tileModule(int x, int y, int z) {
        return blocks.getModule(x, y, z);
    }
    
    @Nullable
    public TileType tileEntity(int x, int y, int z) {
        return blocks.getTile(x, y, z);
    }
    
    public TileType randomTile() {
        return Objects.requireNonNull(blocks.getOne()).iface;
    }
    
    @Contract(pure = true)
    public boolean canAttachTile(IMultiblockTile<?, ?> tile) {
        return tileTypeValidator.validate((BlockEntity) tile);
    }
    
    public void attemptAttach(@Nonnull MultiblockTileModule<?, ?> toAttachGeneric) {
        attemptAttach(toAttachGeneric, false);
    }
    
    public void attemptAttach(@Nonnull MultiblockTileModule<?, ?> toAttachGeneric, boolean merging) {
        if (!canAttachTile(toAttachGeneric.iface)) {
            return;
        }
        
        // TODO: state updating check
        
        //noinspection unchecked
        var toAttachModule = (MultiblockTileModule<TileType, ControllerType>) toAttachGeneric;
        var toAttachTile = toAttachModule.iface;
        
        for (final var module : modules()) {
            if (!module.canAttachPart(toAttachTile)) {
                return;
            }
        }
        
        if (toAttachModule.controller() != null && toAttachModule.controller() != this) {
            if (toAttachModule.controller().blocks.size() > blocks.size()) {
                toAttachModule.controller().controllersToMerge.add(self());
            } else {
                controllersToMerge.add(toAttachModule.controller());
            }
            return;
        }
        
        // check if already attached
        if (toAttachModule.controller() == this) {
            return;
        }
        
        toAttachModule.controller(self());
        
        if (!blocks.addModule(toAttachModule)) {
            // weird edge case that happened, clearing merged controller's lists of blocks fixed this, but just in case
            return;
        }
        
        BlockPos toAttachPos = toAttachTile.getBlockPos();
        // update minmax
        if (toAttachPos.getX() < minCoord.x) {
            minCoord.x = toAttachPos.getX();
            minExtremeBlocks.x = 1;
        } else if (toAttachPos.getX() == minCoord.x) {
            minExtremeBlocks.x++;
        }
        if (toAttachPos.getY() < minCoord.y) {
            minCoord.y = toAttachPos.getY();
            minExtremeBlocks.y = 1;
        } else if (toAttachPos.getY() == minCoord.y) {
            minExtremeBlocks.y++;
        }
        if (toAttachPos.getZ() < minCoord.z) {
            minCoord.z = toAttachPos.getZ();
            minExtremeBlocks.z = 1;
        } else if (toAttachPos.getZ() == minCoord.z) {
            minExtremeBlocks.z++;
        }
        if (toAttachPos.getX() > maxCoord.x) {
            maxCoord.x = toAttachPos.getX();
            maxExtremeBlocks.x = 1;
        } else if (toAttachPos.getX() == maxCoord.x) {
            maxExtremeBlocks.x++;
        }
        if (toAttachPos.getY() > maxCoord.y) {
            maxCoord.y = toAttachPos.getY();
            maxExtremeBlocks.y = 1;
        } else if (toAttachPos.getY() == maxCoord.y) {
            maxExtremeBlocks.y++;
        }
        if (toAttachPos.getZ() > maxCoord.z) {
            maxCoord.z = toAttachPos.getZ();
            maxExtremeBlocks.z = 1;
        } else if (toAttachPos.getZ() == maxCoord.z) {
            maxExtremeBlocks.z++;
        }
        
        for (var module : modules()) {
            module.onPartAdded(toAttachTile);
        }
        onPartAdded(toAttachTile);
        if (merging) {
            for (var module : modules()) {
                module.onPartAttached(toAttachTile);
            }
            onPartAttached(toAttachTile);
        } else if (toAttachModule.preExistingBlock) {
            for (var module : modules()) {
                module.onPartLoaded(toAttachTile);
            }
            onPartLoaded(toAttachTile);
        } else {
            for (var module : modules()) {
                module.onPartPlaced(toAttachTile);
            }
            onPartPlaced(toAttachTile);
        }
        
        toAttachModule.updateNeighbors();
        requestValidation();
    }
    
    public void detach(@Nonnull MultiblockTileModule<TileType, ControllerType> toDetachModule, boolean chunkUnload, boolean merging, boolean checkForDetachments) {
        
        if (!blocks.removeModule(toDetachModule)) {
            return;
        }
        
        final var toDetachTile = toDetachModule.iface;
        
        if (merging) {
            for (var module : modules()) {
                module.onPartDetached(toDetachTile);
            }
            onPartDetached(toDetachTile);
        } else if (chunkUnload) {
            for (var module : modules()) {
                module.onPartUnloaded(toDetachTile);
            }
            onPartUnloaded(toDetachTile);
        } else {
            for (var module : modules()) {
                module.onPartBroken(toDetachTile);
            }
            onPartBroken(toDetachTile);
        }
        for (var module : modules()) {
            module.onPartRemoved(toDetachTile);
        }
        onPartRemoved(toDetachTile);
        
        BlockPos toDetachPos = toDetachTile.getBlockPos();
        if (toDetachPos.getX() == minCoord.x) {
            minExtremeBlocks.x--;
            if (minExtremeBlocks.x == 0) {
                updateExtremes = true;
            }
        }
        if (toDetachPos.getY() == minCoord.y) {
            minExtremeBlocks.y--;
            if (minExtremeBlocks.y == 0) {
                updateExtremes = true;
            }
        }
        if (toDetachPos.getZ() == minCoord.z) {
            minExtremeBlocks.z--;
            if (minExtremeBlocks.z == 0) {
                updateExtremes = true;
            }
        }
        if (toDetachPos.getX() == maxCoord.x) {
            maxExtremeBlocks.x--;
            if (maxExtremeBlocks.x == 0) {
                updateExtremes = true;
            }
        }
        if (toDetachPos.getY() == maxCoord.y) {
            maxExtremeBlocks.y--;
            if (maxExtremeBlocks.y == 0) {
                updateExtremes = true;
            }
        }
        if (toDetachPos.getZ() == maxCoord.z) {
            maxExtremeBlocks.z--;
            if (maxExtremeBlocks.z == 0) {
                updateExtremes = true;
            }
        }
        
        if (checkForDetachments) {
            this.checkForDetachmentsAtTick = Phosphophyllite.tickNumber() + 2;
            if (!chunkUnload) {
                this.checkForDetachmentsAtTick = Long.MIN_VALUE;
            }
            removedBlocks.add(new Detachment(toDetachPos, toDetachModule.neighborTiles));
        }
        
        if (!merging) {
            toDetachModule.nullNeighbors();
        }
        toDetachModule.controller(null);
        
        requestValidation();
    }
    
    private void updateMinMaxCoordinates() {
        if (blocks.isEmpty() || !updateExtremes) {
            return;
        }
        updateExtremes = false;
        minCoord.set(Integer.MAX_VALUE);
        maxCoord.set(Integer.MIN_VALUE);
        blocks.forEachPos(pos -> {
            if (pos.getX() < minCoord.x) {
                minCoord.x = pos.getX();
                minExtremeBlocks.x = 1;
            } else if (pos.getX() == minCoord.x) {
                minExtremeBlocks.x++;
            }
            if (pos.getY() < minCoord.y) {
                minCoord.y = pos.getY();
                minExtremeBlocks.y = 1;
            } else if (pos.getY() == minCoord.y) {
                minExtremeBlocks.y++;
            }
            if (pos.getZ() < minCoord.z) {
                minCoord.z = pos.getZ();
                minExtremeBlocks.z = 1;
            } else if (pos.getZ() == minCoord.z) {
                minExtremeBlocks.z++;
            }
            if (pos.getX() > maxCoord.x) {
                maxCoord.x = pos.getX();
                maxExtremeBlocks.x = 1;
            } else if (pos.getX() == maxCoord.x) {
                maxExtremeBlocks.x++;
            }
            if (pos.getY() > maxCoord.y) {
                maxCoord.y = pos.getY();
                maxExtremeBlocks.y = 1;
            } else if (pos.getY() == maxCoord.y) {
                maxExtremeBlocks.y++;
                
            }
            if (pos.getZ() > maxCoord.z) {
                maxCoord.z = pos.getZ();
                maxExtremeBlocks.z = 1;
            } else if (pos.getZ() == maxCoord.z) {
                maxExtremeBlocks.z++;
            }
        });
    }
    
    private void updateAssemblyState() {
        if (updateAssemblyAtTick > Phosphophyllite.tickNumber()) {
            return;
        }
        updateAssemblyAtTick = Long.MAX_VALUE;
        lastValidationError = null;
        try {
            for (final var tileTypeControllerTypeMultiblockControllerModule : modules()) {
                tileTypeControllerTypeMultiblockControllerModule.preValidate();
            }
            preValidate();
            for (final var tileTypeControllerTypeMultiblockControllerModule : modules()) {
                tileTypeControllerTypeMultiblockControllerModule.validate();
            }
            validate();
        } catch (ValidationException validationError) {
            lastValidationError = validationError;
        }
        transitionToState(lastValidationError == null ? AssemblyState.ASSEMBLED : assemblyState == AssemblyState.PAUSED ? AssemblyState.PAUSED : AssemblyState.DISASSEMBLED);
    }
    
    private void processDetachments() {
        if (checkForDetachmentsAtTick > Phosphophyllite.tickNumber()) {
            return;
        }
        
        checkForDetachmentsAtTick = Long.MAX_VALUE;
        
        AStarList<MultiblockTileModule<?, ?>> aStarList = new AStarList<>(module -> module.iface.getBlockPos());
        
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        for (Detachment removedBlock : removedBlocks) {
            for (Direction value : DIRECTIONS) {
                if (!removedBlock.linked(value)) {
                    continue;
                }
                mutableBlockPos.set(removedBlock.pos);
                mutableBlockPos.move(value);
                final var module = blocks.getModule(mutableBlockPos);
                if (module != null && module.controller() == this) {
                    aStarList.addTarget(module);
                }
            }
        }
        removedBlocks.clear();
        
        while (!aStarList.done()) {
            final var node = aStarList.nextNode();
            for (int i = 0; i < 6; i++) {
                node.lastSavedTick = this.lastTick;
                var module = node.getNeighbor(DIRECTIONS[i]);
                if (module != null && module.controller() == this && module.lastSavedTick != this.lastTick) {
                    module.lastSavedTick = this.lastTick;
                    aStarList.addNode(module);
                }
            }
        }
        
        if (aStarList.foundAll()) {
            return;
        }
        
        ObjectOpenHashSet<MultiblockTileModule<TileType, ControllerType>> toOrphan = new ObjectOpenHashSet<>();
        blocks.forEachModule(module -> {
            if (module.lastSavedTick != this.lastTick) {
                toOrphan.add(module);
            }
        });
        if (toOrphan.isEmpty()) {
            return;
        }
        
        for (MultiblockTileModule<TileType, ControllerType> tile : toOrphan) {
            detach(tile, false, true, false);
        }
        final var newMultiblocks = new ObjectArrayList<ControllerType>();
        // TODO: move to A* because its likely that its split into two
        final var tempModuleArrays = new ObjectArrayList<MultiblockTileModule<TileType, ControllerType>>();
        final var newMultiblockModules = new ObjectOpenHashSet<MultiblockTileModule<TileType, ControllerType>>();
        while (!toOrphan.isEmpty()) {
            final var first = toOrphan.iterator().next();
            tempModuleArrays.add(first);
            toOrphan.remove(first);
            while (!tempModuleArrays.isEmpty()) {
                final var popped = tempModuleArrays.pop();
                newMultiblockModules.add(popped);
                toOrphan.remove(popped);
                for (final var neighbor : popped.neighbors) {
                    if (neighbor == null || newMultiblockModules.contains(neighbor)) {
                        continue;
                    }
                    tempModuleArrays.add(neighbor);
                }
            }
            var newController = first.iface.createController();
            for (final var newMultiblockModule : newMultiblockModules) {
                newController.attemptAttach(newMultiblockModule, true);
            }
            newMultiblockModules.clear();
            newMultiblocks.add(newController);
        }
        
        for (final var module : modules()) {
            module.split(newMultiblocks);
        }
        split(newMultiblocks);
        
        updateAssemblyAtTick = Long.MIN_VALUE;
    }
    
    private void processMerges() {
        while (!controllersToMerge.isEmpty()) {
            ObjectOpenHashSet<MultiblockController<TileType, ControllerType>> newToMerge = new ObjectOpenHashSet<>();
            for (MultiblockController<TileType, ControllerType> otherController : controllersToMerge) {
                //noinspection unchecked
                final var otherCased = (ControllerType) otherController;
                otherController.disassembledBlockStates();
                MultiblockRegistry.removeController(otherController);
                otherController.controllersToMerge.remove(self());
                newToMerge.addAll(otherController.controllersToMerge);
                otherController.controllersToMerge.clear();
                if (otherController.mergedInto != null && otherController.mergedInto != this) {
                    newToMerge.add(otherController.mergedInto);
                    otherController.mergedInto.controllersToMerge.add(this);
                    continue;
                }
                if (otherController.blocks.size() == 0) {
                    continue;
                }
                for (var module : modules()) {
                    module.merge(otherCased);
                }
                this.merge(otherCased);
                final var otherElements = otherController.blocks.moduleElements().clone();
                final var size = otherController.blocks.size();
                for (int i = 0; i < size; i++) {
                    final var module = otherElements[i];
                    otherController.detach(module, false, true, false);
                    module.controller(null);
                    module.preExistingBlock = true;
                    attemptAttach(module, true);
                }
                otherController.blocks.clear();
                updateAssemblyAtTick = Long.MIN_VALUE;
                otherController.mergedInto = this;
            }
            controllersToMerge.clear();
            controllersToMerge.addAll(newToMerge);
        }
    }
    
    public final void suicide() {
        if (blocks.isEmpty()) {
            return;
        }
        ModuleMap<MultiblockTileModule<TileType, ControllerType>, TileType> blocks = new ModuleMap<MultiblockTileModule<TileType, ControllerType>, TileType>(new MultiblockTileModule[0]);
        blocks.addAll(this.blocks);
        blocks.forEachModule(module -> module.onRemoved(true));
        MultiblockRegistry.removeController(this);
    }
    
    public final void update() {
        if (lastTick >= Phosphophyllite.tickNumber()) {
            return;
        }
        lastTick = Phosphophyllite.tickNumber();
        
        if (blocks.isEmpty()) {
            // why are we being ticked?
            MultiblockRegistry.removeController(this);
            checkForDetachmentsAtTick = Long.MAX_VALUE;
            return;
        }
        
        processDetachments();
        processMerges();
        updateMinMaxCoordinates();
        updateAssemblyState();
        
        if (!Util.isEntireAreaLoaded(level, min(), max())) {
            return;
        }
        if (lastFullTick < lastTick - 1) {
            requestValidation();
        }
        lastFullTick = lastTick;
        
        modules().forEach(MultiblockControllerModule::preTick);
        if (assemblyState == AssemblyState.ASSEMBLED) {
            tick();
        } else if (assemblyState == AssemblyState.DISASSEMBLED) {
            disassembledTick();
        }
        modules().forEach(MultiblockControllerModule::postTick);
    }
    
    public final void transitionToState(AssemblyState newAssemblyState) {
        switch (newAssemblyState) {
            case ASSEMBLED -> assembledBlockStates();
            case DISASSEMBLED -> disassembledBlockStates();
        }
        final var oldAssemblyState = assemblyState;
        for (var module : modules()) {
            module.onStateTransition(oldAssemblyState, newAssemblyState);
        }
        onStateTransition(oldAssemblyState, newAssemblyState);
        assemblyState = newAssemblyState;
    }
    
    private final Long2ObjectLinkedOpenHashMap<BlockState> newStates = new Long2ObjectLinkedOpenHashMap<>();
    
    private void assembledBlockStates() {
        newStates.clear();
        
        final int size = blocks.size();
        final TileType[] tileElements = blocks.tileElements();
        final MultiblockTileModule<TileType, ControllerType>[] moduleElements = blocks.moduleElements();
        final var posElements = blocks.posElements();
        if (tileElements.length < size || moduleElements.length < size || posElements.length < size) {
            throw new IllegalStateException("Arrays too short");
        }
        for (int i = 0; i < size; i++) {
            final var entity = tileElements[i];
            final var module = moduleElements[i];
            final var pos = posElements[i];
            final BlockState oldState = entity.getBlockState();
            BlockState newState = module.assembledBlockState(oldState);
            if (newState != oldState) {
                newStates.put(pos, newState);
                entity.setBlockState(newState);
            }
        }
        if (!newStates.isEmpty()) {
            Util.setBlockStates(newStates, level);
        }
    }
    
    private void disassembledBlockStates() {
        newStates.clear();
        final int size = blocks.size();
        final TileType[] tileElements = blocks.tileElements();
        final MultiblockTileModule<TileType, ControllerType>[] moduleElements = blocks.moduleElements();
        final var posElements = blocks.posElements();
        if (tileElements.length < size || moduleElements.length < size || posElements.length < size) {
            throw new IllegalStateException("Arrays too short");
        }
        for (int i = 0; i < size; i++) {
            final var entity = tileElements[i];
            final var module = moduleElements[i];
            final var pos = posElements[i];
            final BlockState oldState = entity.getBlockState();
            BlockState newState = module.disassembledBlockState(oldState);
            if (newState != oldState) {
                newStates.put(pos, newState);
                entity.setBlockState(newState);
            }
        }
        if (!newStates.isEmpty()) {
            Util.setBlockStates(newStates, level);
        }
    }
    
    /**
     * Gets the info printed to chat when block is clicked with the @DebugTool
     * safe to override, just append to the string returned by super.getDebugString
     * not my fault if you manage to break it
     *
     * @return string to print
     */
    @Nonnull
    public String getDebugString() {
        return "BlockCount: " + blocks.size() + "\n" +
                "Min " + minCoord + "\n" +
                "Max " + maxCoord + "\n" +
                "Controller: " + this + "\n" +
                "Last Error: " + (lastValidationError == null ? "N/A" : lastValidationError.getTextComponent().getString()) + "\n" +
                "AssemblyState: " + assemblyState + "\n";
    }
    
    // ------------------------------------ API ------------------------------------
    
    public void requestValidation() {
        updateAssemblyAtTick = Phosphophyllite.tickNumber() + 1;
    }
    
    protected void preValidate() throws ValidationException {
    }
    
    protected void validate() throws ValidationException {
    }
    
    // TODO: potentially remove ticking from core multiblock
    protected void tick() {
    }
    
    protected void disassembledTick() {
    }
    
    protected void onPartAdded(@Nonnull TileType tile) {
    }
    
    protected void onPartRemoved(@Nonnull TileType tile) {
    }
    
    protected void onPartLoaded(@Nonnull TileType tile) {
    }
    
    protected void onPartUnloaded(@Nonnull TileType tile) {
    }
    
    protected void onPartAttached(@Nonnull TileType tile) {
    }
    
    protected void onPartDetached(@Nonnull TileType tile) {
    }
    
    protected void onPartPlaced(@Nonnull TileType tile) {
    }
    
    protected void onPartBroken(@Nonnull TileType tile) {
    }
    
    protected void merge(ControllerType other) {
    }
    
    protected void split(List<ControllerType> others) {
    }
    
    protected void onStateTransition(AssemblyState oldAssemblyState, AssemblyState newAssemblyState) {
        switch (oldAssemblyState) {
            case ASSEMBLED -> {
                switch (newAssemblyState) {
                    case DISASSEMBLED -> onDisassembled();
                    case ASSEMBLED -> onReassembled();
                    case PAUSED -> undefinedStateTransition(oldAssemblyState, newAssemblyState);
                }
            }
            case DISASSEMBLED -> {
                switch (newAssemblyState) {
                    case ASSEMBLED -> onAssembled();
                    case DISASSEMBLED -> undefinedStateTransition(oldAssemblyState, newAssemblyState);
                    case PAUSED -> undefinedStateTransition(oldAssemblyState, newAssemblyState);
                }
            }
            case PAUSED -> {
                switch (newAssemblyState) {
                    case ASSEMBLED -> onUnpaused();
                    case DISASSEMBLED -> undefinedStateTransition(oldAssemblyState, newAssemblyState);
                    case PAUSED -> undefinedStateTransition(oldAssemblyState, newAssemblyState);
                }
            }
        }
    }
    
    protected void undefinedStateTransition(AssemblyState oldAssemblyState, AssemblyState newAssemblyState) {
    }
    
    protected void onAssembled() {
    }
    
    protected void onReassembled() {
    }
    
    protected void onDisassembled() {
    }
    
    protected void onUnpaused() {
    }
}
