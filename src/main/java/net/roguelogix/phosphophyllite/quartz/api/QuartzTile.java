package net.roguelogix.phosphophyllite.quartz.api;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class QuartzTile extends TileEntity implements IQuartzTile {
    
    private final QuartzState quartzState;
    
    public QuartzTile(@Nonnull TileEntityType<?> tileEntityTypeIn) {
        this(tileEntityTypeIn, tileEntityTypeIn.getRegistryName().getNamespace() + ":quartzstates/" + tileEntityTypeIn.getRegistryName().getPath());
    }
    public QuartzTile(@Nonnull TileEntityType<?> tileEntityTypeIn, String stateLocation) {
        super(tileEntityTypeIn);
        quartzState = new QuartzState(stateLocation);
        buildDefaultQuartzState(quartzState);
    }
    
    @Override
    public void onLoad() {
        Quartz.registerTileEntity(this);
        pushQuartzStateUpdate();
    }
    
    @Override
    public void remove() {
        Quartz.unregisterTileEntity(this);
    }
    
    @Override
    public void onChunkUnloaded() {
        Quartz.unregisterTileEntity(this);
    }
    
    protected void buildDefaultQuartzState(QuartzState state) {
    }
    
    private void pushQuartzStateUpdate() {
        Quartz.requestBlockUpdate(this);
    }
    
    @Override
    public final SUpdateTileEntityPacket getUpdatePacket() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.put("quartzState", quartzState.serializeNBT());
        CompoundNBT userNBT = createUpdatePacketCompound();
        if (userNBT != null) {
            nbt.put("userNBT", userNBT);
        }
        return new SUpdateTileEntityPacket(this.getPos(), -1, nbt);
    }
    
    @Override
    public final void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
        CompoundNBT nbt = pkt.getNbtCompound();
        if (nbt.contains("userNBT")) {
            onUpdatePacketCompound(nbt.getCompound("userNBT"));
        }
        if (nbt.contains("quartzState")) {
            quartzState.deserializeNBT(nbt.getCompound("quartzState"));
            pushQuartzStateUpdate();
        }
    }
    
    public final void setQuartzState(QuartzState state) {
        assert world != null;
        quartzState.copyFrom(state);
        world.notifyBlockUpdate(pos, getBlockState(), getBlockState(), 6);
    }
    
    public final QuartzState getQuartzState() {
        return quartzState;
    }
    
    @Nullable
    public CompoundNBT createUpdatePacketCompound() {
        return null;
    }
    
    public void onUpdatePacketCompound(@Nonnull CompoundNBT compoundNBT) {
    }
    
    @Override
    public void read(BlockState state, CompoundNBT nbt) {
        if (nbt.contains("quartzState")) {
            quartzState.deserializeNBT(nbt.getCompound("quartzState"));
            pushQuartzStateUpdate();
        }
        super.read(state, nbt);
    }
    
    @Override
    public CompoundNBT write(CompoundNBT nbt) {
        nbt.put("quartzState", quartzState.serializeNBT());
        return super.write(nbt);
    }
    
    @Override
    public QuartzState getState() {
        return quartzState;
    }
}
