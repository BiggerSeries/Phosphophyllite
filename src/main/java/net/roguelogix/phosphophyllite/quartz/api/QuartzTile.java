package net.roguelogix.phosphophyllite.quartz.api;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class QuartzTile extends TileEntity {
    
    private QuartzState quartzState =
            null;
    
    public QuartzTile(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
        ResourceLocation registryName = this.getType().getRegistryName();
        assert registryName != null;
        quartzState = new QuartzState(registryName.getNamespace() + ":quartzstates/" + registryName.getPath());
        buildDefaultQuartzState(quartzState);
    }
    
    @Override
    public void onLoad() {
        pushQuartzStateUpdate();
    }
    
    @Override
    public void remove() {
        assert world != null;
        if (world.isRemote) {
            Quartz.setQuartzState(world, new Vector3i(pos.getX(), pos.getY(), pos.getZ()), null);
        }
    }
    
    @Override
    public void onChunkUnloaded() {
        assert world != null;
        if(world.isRemote){
            Quartz.setQuartzState(world, new Vector3i(pos.getX(), pos.getY(), pos.getZ()), null);
        }
    }
    
    protected void buildDefaultQuartzState(QuartzState state) {
    }
    
    private void pushQuartzStateUpdate() {
        if (world != null) {
            if (world.isRemote) {
                Quartz.setQuartzState(world, new Vector3i(pos.getX(), pos.getY(), pos.getZ()), quartzState);
            }
        }
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
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
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
        quartzState = state.copy();
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
}
