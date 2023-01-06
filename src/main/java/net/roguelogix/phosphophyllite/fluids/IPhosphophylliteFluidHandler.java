package net.roguelogix.phosphophyllite.fluids;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;


import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IPhosphophylliteFluidHandler extends IFluidHandler {
    // TODO: fix this race condition
    //       in single player, this stack can be used by both the server and client at the same time
    //       also cant use this in multiple threads, but lots of things in MC are like that
    //       real solution will probably be similar to what i did for energy, *wrappers*
    
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
        if(stack.getRawFluid() == Fluids.EMPTY){
            return FluidStack.EMPTY;
        }
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
    CompoundTag fluidTagInTank(int tank);
    
    long fluidAmountInTank(int tank);
    
    boolean fluidValidForTank(int tank, Fluid fluid);
    
    long fill(Fluid fluid, @Nullable CompoundTag tag, long amount, boolean simulate);
    
    long drain(Fluid fluid, @Nullable CompoundTag tag, long amount, boolean simulate);
}
