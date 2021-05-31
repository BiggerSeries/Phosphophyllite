package net.roguelogix.phosphophyllite.tile;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PhosphophylliteTile extends TileEntity implements IModularTile{
    
    public static final Logger LOGGER = LogManager.getLogger("Phosphophyllite/ModularTile");
    
    private static final LinkedHashMap<Class<?>, Function<TileEntity, Module>> moduleRegistry = new LinkedHashMap<>();
    private static final ArrayList<BiConsumer<Class<?>, Function<TileEntity, Module>>> externalRegistrars = new ArrayList<>();
    
    /**
     * Registers a module and the interface the tile class will implement to signal to create an instance at tile creation
     * <p>
     * Also passes this value to any external registries registered below
     *
     * @param moduleInterface: Interface class that will be implemented by the tile.
     * @param constructor:     Creates an instance of a module for the given tile with the interface implemented
     */
    public synchronized static void registerModule(Class<?> moduleInterface, Function<TileEntity, Module> constructor) {
        moduleRegistry.put(moduleInterface, constructor);
        externalRegistrars.forEach(c -> c.accept(moduleInterface, constructor));
    }
    
    /**
     * allows for external implementations of the module system, so extensions from PhosphophylliteTile and PhosphophylliteBlock aren't forced
     *
     * @param registrar external moduleRegistration function
     */
    public synchronized static void registerExternalRegistrar(BiConsumer<Class<?>, Function<TileEntity, Module>> registrar) {
        externalRegistrars.add(registrar);
        moduleRegistry.forEach(registrar);
    }
    
    public interface Module {
        
        TileEntity getTile();
        
        void onAdded();
        
        void onRemoved(boolean chunkUnload);
        
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
        default <T> LazyOptional<T> capability(final Capability<T> cap, final @Nullable Direction side) {
            return LazyOptional.empty();
        }
        
        String saveKey();
        
        /**
         * Standard world save NBT
         *
         * @param nbt
         */
        void readNBT(CompoundNBT nbt);
        
        @Nullable
        CompoundNBT writeNBT();
        
        /**
         * Initial server -> client sync on client side chunk load
         *
         * @param nbt
         */
        default void handleDataNBT(CompoundNBT nbt) {
            // mimmicks behavior of IForgeTileEntity
            readNBT(nbt);
        }
        
        @Nullable
        default CompoundNBT getDataNBT() {
            // mimmicks behavior of IForgeTileEntity
            return writeNBT();
        }
        
        /**
         * Updates while chunk is loaded
         *
         * @param nbt
         */
        default void handleUpdateNBT(CompoundNBT nbt) {
        }
        
        @Nullable
        default CompoundNBT getUpdateNBT() {
            return null;
        }
        
        default String getDebugInfo() {
            return "";
        }
    }
    
    private final LinkedHashMap<Class<?>, Module> modules = new LinkedHashMap<>();
    private final ArrayList<Module> moduleList = new ArrayList<>();
    
    public PhosphophylliteTile(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
        Class<?> thisClazz = this.getClass();
        moduleRegistry.forEach((clazz, constructor) -> {
            if (clazz.isAssignableFrom(thisClazz)) {
                Module module = constructor.apply(this);
                modules.put(clazz, module);
                moduleList.add(module);
            }
        });
    }
    
    public Module getModule(Class<?> interfaceClazz) {
        return modules.get(interfaceClazz);
    }
    
    @Override
    public final void onLoad() {
        super.onLoad();
        moduleList.forEach(Module::onAdded);
        onAdded();
    }
    
    public void onAdded() {
    }
    
    @Override
    public final void remove() {
        super.remove();
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
    public final void read(BlockState state, CompoundNBT compound) {
        super.read(state, compound.getCompound("super"));
        if (compound.contains("local")) {
            CompoundNBT local = compound.getCompound("local");
            readNBT(local);
        }
        CompoundNBT subNBTs = compound.getCompound("sub");
        for (Module module : moduleList) {
            String key = module.saveKey();
            if (subNBTs.contains(key)) {
                CompoundNBT nbt = subNBTs.getCompound(key);
                module.readNBT(nbt);
            }
        }
    }
    
    @Override
    public final CompoundNBT write(CompoundNBT compound) {
        CompoundNBT superNBT = super.write(compound);
        CompoundNBT subNBTs = new CompoundNBT();
        for (Module module : moduleList) {
            CompoundNBT nbt = module.writeNBT();
            if (nbt != null) {
                String key = module.saveKey();
                if (subNBTs.contains(key)) {
                    // TODO: log warning
                }
                subNBTs.put(key, nbt);
            }
        }
        CompoundNBT nbt = new CompoundNBT();
        nbt.put("super", superNBT);
        nbt.put("sub", subNBTs);
        CompoundNBT localNBT = writeNBT();
        if (localNBT != null) {
            nbt.put("local", localNBT);
        }
        return nbt;
    }
    
    protected void readNBT(CompoundNBT compound) {
    }
    
    @Nullable
    protected CompoundNBT writeNBT() {
        return null;
    }
    
    @Override
    public final void handleUpdateTag(BlockState state, CompoundNBT compound) {
        super.handleUpdateTag(state, compound.getCompound("super"));
        if (compound.contains("local")) {
            CompoundNBT local = compound.getCompound("local");
            handleDataNBT(local);
        }
        CompoundNBT subNBTs = compound.getCompound("sub");
        for (Module module : moduleList) {
            String key = module.saveKey();
            if (subNBTs.contains(key)) {
                CompoundNBT nbt = subNBTs.getCompound(key);
                module.handleDataNBT(nbt);
            }
        }
    }
    
    @Override
    public final CompoundNBT getUpdateTag() {
        CompoundNBT superNBT = super.getUpdateTag();
        CompoundNBT subNBTs = new CompoundNBT();
        for (Module module : moduleList) {
            CompoundNBT nbt = module.getDataNBT();
            if (nbt != null) {
                String key = module.saveKey();
                if (subNBTs.contains(key)) {
                    LOGGER.warn("Multiple modules with the same save key \"" + key + "\" for tile type \"" + getClass().getSimpleName() + "\" at " + getPos());
                }
                subNBTs.put(key, nbt);
            }
        }
        CompoundNBT nbt = new CompoundNBT();
        nbt.put("super", superNBT);
        nbt.put("sub", subNBTs);
        CompoundNBT localNBT = getDataNBT();
        if (localNBT != null) {
            nbt.put("local", localNBT);
        }
        return nbt;
    }
    
    protected void handleDataNBT(CompoundNBT nbt) {
        // mimmicks behavior of IForgeTileEntity
        readNBT(nbt);
    }
    
    @Nullable
    protected CompoundNBT getDataNBT() {
        return writeNBT();
    }
    
    @Override
    public final void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
        assert world != null;
        // getters are client only, so, cant grab it on the server even if i want to
        if (!world.isRemote) {
            return;
        }
        CompoundNBT compound = pkt.getNbtCompound();
        if (compound.contains("local")) {
            CompoundNBT local = compound.getCompound("local");
            handleUpdateNBT(local);
        }
        CompoundNBT subNBTs = compound.getCompound("sub");
        for (Module module : moduleList) {
            String key = module.saveKey();
            if (subNBTs.contains(key)) {
                CompoundNBT nbt = subNBTs.getCompound(key);
                module.handleUpdateNBT(nbt);
            }
        }
    }
    
    @Nullable
    @Override
    public final SUpdateTileEntityPacket getUpdatePacket() {
        boolean sendPacket = false;
        CompoundNBT subNBTs = new CompoundNBT();
        for (Module module : moduleList) {
            CompoundNBT nbt = module.getUpdateNBT();
            if (nbt != null) {
                sendPacket = true;
                String key = module.saveKey();
                if (subNBTs.contains(key)) {
                    LOGGER.warn("Multiple modules with the same save key \"" + key + "\" for tile type \"" + getClass().getSimpleName() + "\" at " + getPos());
                }
                subNBTs.put(key, nbt);
            }
        }
        CompoundNBT nbt = new CompoundNBT();
        nbt.put("sub", subNBTs);
        CompoundNBT localNBT = getUpdateNBT();
        if (localNBT != null) {
            sendPacket = true;
            nbt.put("local", localNBT);
        }
        if (!sendPacket) {
            return null;
        }
        return new SUpdateTileEntityPacket(this.getPos(), 0, nbt);
    }
    
    void handleUpdateNBT(CompoundNBT nbt) {
    }
    
    @Nullable
    CompoundNBT getUpdateNBT() {
        return null;
    }
    
    @Nonnull
    public final <T> LazyOptional<T> getCapability(final Capability<T> cap, final @Nullable Direction side) {
        LazyOptional<T> optional = capability(cap, side);
        for (Module module : moduleList) {
            LazyOptional<T> moduleOptional = module.capability(cap, side);
            if (moduleOptional.isPresent()) {
                if (optional.isPresent()) {
                    LOGGER.warn("Multiple implementations of same capability \"" + cap.getName() + "\" on " + side + " side for tile type \"" + getClass().getSimpleName() + "\" at " + getPos());
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
    
}
