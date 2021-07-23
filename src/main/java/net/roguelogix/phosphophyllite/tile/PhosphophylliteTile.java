package net.roguelogix.phosphophyllite.tile;

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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.LinkedHashMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PhosphophylliteTile extends BlockEntity implements IModularTile {
    
    public static final Logger LOGGER = LogManager.getLogger("Phosphophyllite/ModularTile");
    
    private final LinkedHashMap<Class<?>, ITileModule> modules = new LinkedHashMap<>();
    private final ArrayList<ITileModule> moduleList = new ArrayList<>();
    
    public PhosphophylliteTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
        if (!(state.getBlock() instanceof PhosphophylliteBlock)) {
            throw new IllegalStateException("PhosphophylliteTiles must only be on a PhosphophylliteBlock");
        }
        Class<?> thisClazz = this.getClass();
        ModuleRegistry.forEach((clazz, constructor) -> {
            if (clazz.isAssignableFrom(thisClazz)) {
                ITileModule module = constructor.apply(this);
                modules.put(clazz, module);
                moduleList.add(module);
            }
        });
    }
    
    public ITileModule getModule(Class<?> interfaceClazz) {
        return modules.get(interfaceClazz);
    }
    
    @Override
    public final void onLoad() {
        super.onLoad();
        moduleList.forEach(ITileModule::onAdded);
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
    
    public void onRemoved(boolean chunkUnload) {
    }
    
    @Override
    public final void load(CompoundTag compound) {
        super.load(compound.getCompound("super"));
        if (compound.contains("local")) {
            CompoundTag local = compound.getCompound("local");
            readNBT(local);
        }
        CompoundTag subNBTs = compound.getCompound("sub");
        for (ITileModule module : moduleList) {
            String key = module.saveKey();
            if (subNBTs.contains(key)) {
                CompoundTag nbt = subNBTs.getCompound(key);
                module.readNBT(nbt);
            }
        }
    }
    
    @Override
    public final CompoundTag save(CompoundTag compound) {
        CompoundTag superNBT = super.save(compound);
        CompoundTag subNBTs = new CompoundTag();
        for (ITileModule module : moduleList) {
            CompoundTag nbt = module.writeNBT();
            if (nbt != null) {
                String key = module.saveKey();
                if (subNBTs.contains(key)) {
                    LOGGER.warn("Multiple modules with the same save key \"" + key + "\" for tile type \"" + getClass().getSimpleName() + "\" at " + getBlockPos());
                }
                subNBTs.put(key, nbt);
            }
        }
        CompoundTag nbt = new CompoundTag();
        nbt.put("super", superNBT);
        nbt.put("sub", subNBTs);
        CompoundTag localNBT = writeNBT();
        if (localNBT != null) {
            nbt.put("local", localNBT);
        }
        return nbt;
    }
    
    protected void readNBT(CompoundTag compound) {
    }
    
    @Nullable
    protected CompoundTag writeNBT() {
        return null;
    }
    
    @Override
    public final void handleUpdateTag(CompoundTag compound) {
        super.handleUpdateTag(compound.getCompound("super"));
        if (compound.contains("local")) {
            CompoundTag local = compound.getCompound("local");
            handleDataNBT(local);
        }
        CompoundTag subNBTs = compound.getCompound("sub");
        for (ITileModule module : moduleList) {
            String key = module.saveKey();
            if (subNBTs.contains(key)) {
                CompoundTag nbt = subNBTs.getCompound(key);
                module.handleDataNBT(nbt);
            }
        }
    }
    
    @Override
    public final CompoundTag getUpdateTag() {
        CompoundTag superNBT = super.getUpdateTag();
        CompoundTag subNBTs = new CompoundTag();
        for (ITileModule module : moduleList) {
            CompoundTag nbt = module.getDataNBT();
            if (nbt != null) {
                String key = module.saveKey();
                if (subNBTs.contains(key)) {
                    LOGGER.warn("Multiple modules with the same save key \"" + key + "\" for tile type \"" + getClass().getSimpleName() + "\" at " + getBlockPos());
                }
                subNBTs.put(key, nbt);
            }
        }
        CompoundTag nbt = new CompoundTag();
        nbt.put("super", superNBT);
        nbt.put("sub", subNBTs);
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
        for (ITileModule module : moduleList) {
            String key = module.saveKey();
            if (subNBTs.contains(key)) {
                CompoundTag nbt = subNBTs.getCompound(key);
                module.handleUpdateNBT(nbt);
            }
        }
    }
    
    @Nullable
    @Override
    public final ClientboundBlockEntityDataPacket getUpdatePacket() {
        boolean sendPacket = false;
        CompoundTag subNBTs = new CompoundTag();
        for (ITileModule module : moduleList) {
            CompoundTag nbt = module.getUpdateNBT();
            if (nbt != null) {
                sendPacket = true;
                String key = module.saveKey();
                if (subNBTs.contains(key)) {
                    LOGGER.warn("Multiple modules with the same save key \"" + key + "\" for tile type \"" + getClass().getSimpleName() + "\" at " + getBlockPos());
                }
                subNBTs.put(key, nbt);
            }
        }
        CompoundTag nbt = new CompoundTag();
        nbt.put("sub", subNBTs);
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
    
    void handleUpdateNBT(CompoundTag nbt) {
    }
    
    @Nullable
    CompoundTag getUpdateNBT() {
        return null;
    }
    
    @Nonnull
    public final <T> LazyOptional<T> getCapability(final Capability<T> cap, final @Nullable Direction side) {
        LazyOptional<T> optional = capability(cap, side);
        for (ITileModule module : moduleList) {
            LazyOptional<T> moduleOptional = module.capability(cap, side);
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
    <T> LazyOptional<T> capability(final Capability<T> cap, final @Nullable Direction side) {
        return LazyOptional.empty();
    }
    
    void onBlockUpdate(BlockPos neighborPos) {
        assert level != null;
        BlockState neighborBlockState = level.getBlockState(neighborPos);
        for (ITileModule module : moduleList) {
            module.onBlockUpdate(neighborBlockState, neighborPos);
        }
    }
}
