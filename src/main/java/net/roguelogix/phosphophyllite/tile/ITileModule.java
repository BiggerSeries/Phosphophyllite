package net.roguelogix.phosphophyllite.tile;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;

public interface ITileModule {
    
    TileEntity getTile();
    
    default void onAdded(){
    }
    
    default void onRemoved(boolean chunkUnload){
    }
    
    default void onBlockUpdate(BlockState neighborBlockState, BlockPos neighborPos){
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
    default <T> LazyOptional<T> capability(final Capability<T> cap, final @Nullable Direction side) {
        return LazyOptional.empty();
    }
    
    String saveKey();
    
    /**
     * Standard world save NBT
     *
     * @param nbt
     */
    default void readNBT(CompoundNBT nbt){
    }
    
    @Nullable
    default CompoundNBT writeNBT(){
        return null;
    }
    
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