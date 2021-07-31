package net.roguelogix.phosphophyllite.modular.tile;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.roguelogix.phosphophyllite.debug.IDebuggable;
import net.roguelogix.phosphophyllite.modular.api.IModularTile;
import net.roguelogix.phosphophyllite.modular.api.TileModule;
import net.roguelogix.phosphophyllite.modular.api.ModuleRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PhosphophylliteTile extends BlockEntity implements IModularTile, IDebuggable {
    
    public static final Logger LOGGER = LogManager.getLogger("Phosphophyllite/ModularTile");
    
    private final LinkedHashMap<Class<?>, TileModule<?>> modules = new LinkedHashMap<>();
    private final ArrayList<TileModule<?>> moduleList = new ArrayList<>();
    private final List<TileModule<?>> moduleListRO = Collections.unmodifiableList(moduleList);
    
    public PhosphophylliteTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
        Class<?> thisClazz = this.getClass();
        ModuleRegistry.forEachTileModule((clazz, constructor) -> {
            if (clazz.isAssignableFrom(thisClazz)) {
                TileModule<?> module = constructor.apply(this);
                modules.put(clazz, module);
                moduleList.add(module);
            }
        });
    }
    
    public TileModule<?> module(Class<?> interfaceClazz) {
        return modules.get(interfaceClazz);
    }
    
    @Override
    public List<TileModule<?>> modules() {
        return moduleListRO;
    }
    
    @Override
    public final void clearRemoved() {
        super.clearRemoved();
        moduleList.forEach(TileModule::onAdded);
        onAdded();
    }
    
    public void onAdded() {
    }
    
    @Override
    public final void setRemoved() {
        super.setRemoved();
        moduleList.forEach(module -> module.onRemoved(false));
        onRemoved(false);
    }
    
    @Override
    public final void onChunkUnloaded() {
        super.onChunkUnloaded();
        moduleList.forEach(module -> module.onRemoved(true));
        onRemoved(true);
    }
    
    public void onRemoved(@SuppressWarnings("unused") boolean chunkUnload) {
    }
    
    @Override
    public final void load(CompoundTag compound) {
        super.load(compound);
        if (compound.contains("local")) {
            CompoundTag local = compound.getCompound("local");
            readNBT(local);
        }
        CompoundTag subNBTs = compound.getCompound("sub");
        for (var module : moduleList) {
            String key = module.saveKey();
            if (key != null && subNBTs.contains(key)) {
                CompoundTag nbt = subNBTs.getCompound(key);
                module.readNBT(nbt);
            }
        }
    }
    
    @Nullable
    private CompoundTag subNBTs(Function<TileModule<?>, CompoundTag> nbtSupplier) {
        CompoundTag subNBTs = new CompoundTag();
        for (var module : moduleList) {
            CompoundTag nbt = nbtSupplier.apply(module);
            if (nbt != null) {
                String key = module.saveKey();
                if (key != null) {
                    if (subNBTs.contains(key)) {
                        LOGGER.warn("Multiple modules with the same save key \"" + key + "\" for tile type \"" + getClass().getSimpleName() + "\" at " + getBlockPos());
                    }
                    subNBTs.put(key, nbt);
                }
            }
        }
        if (subNBTs.isEmpty()) {
            return null;
        }
        return subNBTs;
    }
    
    @Override
    public final CompoundTag save(CompoundTag compound) {
        CompoundTag nbt = super.save(compound);
        CompoundTag subNBTs = subNBTs(TileModule::writeNBT);
        
        if (subNBTs != null) {
            nbt.put("sub", subNBTs);
        }
        CompoundTag localNBT = writeNBT();
        if (!localNBT.isEmpty()) {
            nbt.put("local", localNBT);
        }
        return nbt;
    }
    
    protected void readNBT(CompoundTag compound) {
    }
    
    @Nonnull
    protected CompoundTag writeNBT() {
        return new CompoundTag();
    }
    
    @Override
    public final void handleUpdateTag(CompoundTag compound) {
        super.handleUpdateTag(compound.getCompound("super"));
        if (compound.contains("local")) {
            CompoundTag local = compound.getCompound("local");
            handleDataNBT(local);
        }
        CompoundTag subNBTs = compound.getCompound("sub");
        for (var module : moduleList) {
            String key = module.saveKey();
            if (key != null && subNBTs.contains(key)) {
                CompoundTag nbt = subNBTs.getCompound(key);
                module.handleDataNBT(nbt);
            }
        }
    }
    
    @Override
    public final CompoundTag getUpdateTag() {
        CompoundTag superNBT = super.getUpdateTag();
        CompoundTag subNBTs = subNBTs(TileModule::getDataNBT);
        CompoundTag nbt = new CompoundTag();
        nbt.put("super", superNBT);
        if (subNBTs != null) {
            nbt.put("sub", subNBTs);
        }
        CompoundTag localNBT = getDataNBT();
        if (localNBT != null) {
            nbt.put("local", localNBT);
        }
        return nbt;
    }
    
    protected void handleDataNBT(CompoundTag nbt) {
        // mimmicks behavior of IForgeTileEntity
        readNBT(nbt);
    }
    
    @Nullable
    protected CompoundTag getDataNBT() {
        return writeNBT();
    }
    
    @Override
    public final void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        assert level != null;
        // getters are client only, so, cant grab it on the server even if i want to
        if (!level.isClientSide) {
            return;
        }
        CompoundTag compound = pkt.getTag();
        if (compound.contains("local")) {
            CompoundTag local = compound.getCompound("local");
            handleUpdateNBT(local);
        }
        CompoundTag subNBTs = compound.getCompound("sub");
        for (var module : moduleList) {
            String key = module.saveKey();
            if (key != null && subNBTs.contains(key)) {
                CompoundTag nbt = subNBTs.getCompound(key);
                module.handleUpdateNBT(nbt);
            }
        }
    }
    
    @Nullable
    @Override
    public final ClientboundBlockEntityDataPacket getUpdatePacket() {
        boolean sendPacket = false;
        CompoundTag subNBTs = subNBTs(TileModule::getUpdateNBT);
        CompoundTag nbt = new CompoundTag();
        if (subNBTs != null) {
            nbt.put("sub", subNBTs);
        }
        CompoundTag localNBT = getUpdateNBT();
        if (localNBT != null) {
            sendPacket = true;
            nbt.put("local", localNBT);
        }
        if (!sendPacket) {
            return null;
        }
        return new ClientboundBlockEntityDataPacket(this.getBlockPos(), 0, nbt);
    }
    
    protected void handleUpdateNBT(@SuppressWarnings("unused") CompoundTag nbt) {
    }
    
    @Nullable
    protected CompoundTag getUpdateNBT() {
        return null;
    }
    
    @Nonnull
    public final <T> LazyOptional<T> getCapability(final Capability<T> cap, final @Nullable Direction side) {
        var optional = capability(cap, side);
        for (var module : moduleList) {
            var moduleOptional = module.capability(cap, side);
            if (moduleOptional.isPresent()) {
                if (optional.isPresent()) {
                    LOGGER.warn("Multiple implementations of same capability \"" + cap.getName() + "\" on " + side + " side for tile type \"" + getClass().getSimpleName() + "\" at " + getBlockPos());
                    continue;
                }
                optional = moduleOptional;
            }
        }
        return optional;
    }
    
    /**
     * coped from ICapabilityProvider
     * <p>
     * Retrieves the Optional handler for the capability requested on the specific side.
     * The return value <strong>CAN</strong> be the same for multiple faces.
     * Modders are encouraged to cache this value, using the listener capabilities of the Optional to
     * be notified if the requested capability get lost.
     *
     * @param cap  The capability to check
     * @param side The Side to check from,
     *             <strong>CAN BE NULL</strong>. Null is defined to represent 'internal' or 'self'
     * @return The requested an optional holding the requested capability.
     */
    protected <T> LazyOptional<T> capability(final Capability<T> cap, final @Nullable Direction side) {
        return LazyOptional.empty();
    }
    
    @Override
    public String getDebugString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Module Debug Info:\n");
        for (TileModule<?> tileTypeITileModule : moduleList) {
            String debugString = tileTypeITileModule.getDebugString();
            if (debugString != null) {
                var lines = debugString.split("\n");
                for (String line : lines) {
                    builder.append("\n    ");
                    builder.append(line);
                }
            }
        }
        builder.append("\n\n");
        return builder.toString();
    }
}