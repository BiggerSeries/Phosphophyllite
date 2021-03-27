package net.roguelogix.phosphophyllite.fluids;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;


@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IPhosphophylliteFluidHandler extends IFluidHandler {
    
    PhosphophylliteFluidStack stack = new PhosphophylliteFluidStack();
    
    default LazyOptional<IFluidHandler> fluidHandlerCapability() {
        return LazyOptional.of(() -> this);
    }
    
    @Override
    default int getTanks() {
        return tankCount();
    }
    
    
    @Nonnull
    default FluidStack getFluidInTank(int tank) {
        stack.setFluid(fluidTypeInTank(tank));
        stack.setAmount(fluidAmountInTank(tank));
        stack.setTag(fluidTagInTank(tank));
        return stack;
    }
    
    @Override
    default int getTankCapacity(int tank) {
        return (int) Math.min(Integer.MAX_VALUE, tankCapacity(tank));
    }
    
    @Override
    default boolean isFluidValid(int tank, FluidStack resource) {
        return fluidValidForTank(tank, resource.getRawFluid());
    }
    
    @Override
    default int fill(FluidStack resource, FluidAction action) {
        return (int) fill(resource.getRawFluid(), resource.getTag(), resource.getAmount(), action.simulate());
    }
    
    
    @Nonnull
    @Override
    default FluidStack drain(FluidStack resource, FluidAction action) {
        int drainedAmount = (int) drain(resource.getRawFluid(), resource.getTag(), resource.getAmount(), action.simulate());
        if (drainedAmount == 0) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(resource, drainedAmount);
    }
    
    
    @Nonnull
    @Override
    default FluidStack drain(int maxDrain, FluidAction action) {
        
        int drainedAmount = 0;
        Fluid fluid = null;
        for (int i = 0; i < tankCount(); i++) {
            fluid = fluidTypeInTank(i);
            drainedAmount = (int) drain(fluid, fluidTagInTank(i), maxDrain, action.simulate());
            if (drainedAmount != 0) {
                break;
            }
        }
        if (drainedAmount == 0) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(fluid, drainedAmount);
    }
    
    int tankCount();
    
    long tankCapacity(int tank);
    
    Fluid fluidTypeInTank(int tank);
    
    @Nullable
    CompoundNBT fluidTagInTank(int tank);
    
    long fluidAmountInTank(int tank);
    
    boolean fluidValidForTank(int tank, Fluid fluid);
    
    long fill(Fluid fluid, @Nullable CompoundNBT tag, long amount, boolean simulate);
    
    long drain(Fluid fluid, @Nullable CompoundNBT tag, long amount, boolean simulate);
}
