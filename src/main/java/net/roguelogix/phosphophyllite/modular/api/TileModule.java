package net.roguelogix.phosphophyllite.modular.api;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.roguelogix.phosphophyllite.debug.DebugInfo;
import net.roguelogix.phosphophyllite.debug.IDebuggable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class TileModule<InterfaceType extends IModularTile> implements IDebuggable {
    
    @Nonnull
    public final InterfaceType iface;
    
    public TileModule(IModularTile iface) {
        //noinspection unchecked
        this.iface = (InterfaceType) iface;
    }
    
    public void postModuleConstruction() {
    }
    
    
    public void onAdded() {
    }
    
    public void onRemoved(boolean chunkUnload) {
    }
    
    @Nullable
    public String saveKey() {
        return null;
    }
    
    /**
     * Standard world save NBT
     *
     * @param nbt
     */
    public void readNBT(CompoundTag nbt) {
    }
    
    @Nullable
    public CompoundTag writeNBT() {
        return null;
    }
    
    /**
     * Initial server -> client sync on client side chunk load
     *
     * @param nbt
     */
    public void handleDataNBT(CompoundTag nbt) {
        // mimmicks behavior of IForgeTileEntity
        readNBT(nbt);
    }
    
    @Nullable
    public CompoundTag getDataNBT() {
        // mimmicks behavior of IForgeTileEntity
        return writeNBT();
    }
    
    /**
     * Updates while chunk is loaded
     *
     * @param nbt
     */
    public void handleUpdateNBT(CompoundTag nbt) {
    }
    
    @Nullable
    public CompoundTag getUpdateNBT() {
        return null;
    }
    
    @Nullable
    public DebugInfo getDebugInfo() {
        return null;
    }
}