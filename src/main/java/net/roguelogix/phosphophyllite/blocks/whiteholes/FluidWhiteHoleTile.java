package net.roguelogix.phosphophyllite.blocks.whiteholes;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.registry.TileSupplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static net.minecraftforge.fluids.FluidStack.loadFluidStackFromNBT;

import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@RegisterTileEntity(name = "fluid_white_hole")
public class FluidWhiteHoleTile extends BlockEntity implements IFluidHandler {
    
    @RegisterTileEntity.Type
    public static BlockEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final TileSupplier SUPPLIER = FluidWhiteHoleTile::new;
    
    public FluidWhiteHoleTile(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, final @Nullable Direction side) {
        if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return LazyOptional.of(() -> this).cast();
        }
        return super.getCapability(cap, side);
    }
    
    FluidStack fluidStack = FluidStack.EMPTY;
    
    @Override
    public int getTanks() {
        return 1;
    }
    
    @Nonnull
    @Override
    public FluidStack getFluidInTank(int tank) {
        return fluidStack;
    }
    
    @Override
    public int getTankCapacity(int tank) {
        return Integer.MAX_VALUE;
    }
    
    @Override
    public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
        return true;
    }
    
    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return 0;
    }
    
    @Nonnull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.getFluid() == fluidStack.getFluid()) {
            return resource.copy();
        }
        return FluidStack.EMPTY;
    }
    
    @Nonnull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return new FluidStack(fluidStack.getFluid(), maxDrain);
    }
    
    public void setFluid(Fluid fluid) {
        fluidStack = new FluidStack(fluid, Integer.MAX_VALUE);
    }
    
    @Override
    public CompoundTag save(CompoundTag compound) {
        compound.put("fluidstack", fluidStack.writeToNBT(new CompoundTag()));
        return super.save(compound);
    }
    
    @Override
    public void load(CompoundTag compound) {
        fluidStack = loadFluidStackFromNBT(compound.getCompound("fluidstack"));
        super.load(compound);
    }
    
    public void tick() {
        assert level != null;
        for (Direction direction : Direction.values()) {
            BlockEntity te = level.getBlockEntity(worldPosition.relative(direction));
            if (te != null) {
                te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, direction.getOpposite()).ifPresent(c -> c.fill(fluidStack.copy(), FluidAction.EXECUTE));
            }
        }
    }
}
