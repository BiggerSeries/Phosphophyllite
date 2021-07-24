package net.roguelogix.phosphophyllite.blocks.whiteholes;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.roguelogix.phosphophyllite.energy.EnergyStorageWrapper;
import net.roguelogix.phosphophyllite.energy.IPhosphophylliteEnergyStorage;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@RegisterTileEntity(name = "power_white_hole")
public class PowerWhiteHoleTile extends BlockEntity implements IEnergyStorage {
    
    @RegisterTileEntity.Type
    public static BlockEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final BlockEntityType.BlockEntitySupplier<PowerWhiteHoleTile> SUPPLIER = PowerWhiteHoleTile::new;
    
    public PowerWhiteHoleTile(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, final @Nullable Direction side) {
        if (cap == CapabilityEnergy.ENERGY) {
            return LazyOptional.of(() -> this).cast();
        }
        return super.getCapability(cap, side);
    }
    
    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return 0;
    }
    
    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }
    
    @Override
    public int getEnergyStored() {
        return 0;
    }
    
    @Override
    public int getMaxEnergyStored() {
        return 0;
    }
    
    @Override
    public boolean canExtract() {
        return false;
    }
    
    @Override
    public boolean canReceive() {
        return false;
    }
    
    private final IEnergyStorage[] lastCapability = new IEnergyStorage[6];
    private final IPhosphophylliteEnergyStorage[] wrapped = new IPhosphophylliteEnergyStorage[6];
    
    public void tick() {
        assert level != null;
        for (Direction direction : Direction.values()) {
            BlockEntity te = level.getBlockEntity(worldPosition.relative(direction));
            if (te != null) {
                te.getCapability(CapabilityEnergy.ENERGY, direction.getOpposite()).ifPresent(c -> {
                    final int directionIndex = direction.get3DDataValue();
                    if (c != lastCapability[directionIndex]) {
                        lastCapability[directionIndex] = c;
                        wrapped[directionIndex] = EnergyStorageWrapper.wrap(c);
                    }
                    wrapped[directionIndex].insertEnergy(Long.MAX_VALUE, false);
                });
            }
        }
    }
}
