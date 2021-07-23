package net.roguelogix.phosphophyllite.fluids;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FluidHandlerWrapper implements IPhosphophylliteFluidHandler {
    
    public static IPhosphophylliteFluidHandler wrap(IFluidHandler handler) {
        if (handler instanceof IPhosphophylliteFluidHandler) {
            return (IPhosphophylliteFluidHandler) handler;
        }
        return new FluidHandlerWrapper(handler);
    }
    
    IFluidHandler handler;
    
    private FluidHandlerWrapper(IFluidHandler handler) {
        this.handler = handler;
    }
    
    @Override
    public int tankCount() {
        return handler.getTanks();
    }
    
    @Override
    public long tankCapacity(int tank) {
        return handler.getTankCapacity(tank);
    }
    
    @Override
    public Fluid fluidTypeInTank(int tank) {
        return handler.getFluidInTank(tank).getRawFluid();
    }
    
    @Nullable
    @Override
    public CompoundTag fluidTagInTank(int tank) {
        return handler.getFluidInTank(tank).getTag();
    }
    
    @Override
    public long fluidAmountInTank(int tank) {
        return handler.getFluidInTank(tank).getAmount();
    }
    
    PhosphophylliteFluidStack scratchStack = new PhosphophylliteFluidStack();
    
    @Override
    public boolean fluidValidForTank(int tank, Fluid fluid) {
        if(fluid == Fluids.EMPTY){
            return false;
        }
        scratchStack.setFluid(fluid);
        scratchStack.setAmount(0);
        return handler.isFluidValid(tank, scratchStack);
    }
    
    @Override
    public long fill(Fluid fluid, @Nullable CompoundTag tag, long amount, boolean simulate) {
        if(fluid == Fluids.EMPTY){
            return 0;
        }
        scratchStack.setFluid(fluid);
        scratchStack.setTag(tag);
        scratchStack.setAmount(amount);
        return handler.fill(scratchStack, simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE);
    }
    
    @Override
    public long drain(Fluid fluid, @Nullable CompoundTag tag, long amount, boolean simulate) {
        if(fluid == Fluids.EMPTY){
            return 0;
        }
        scratchStack.setFluid(fluid);
        scratchStack.setTag(tag);
        scratchStack.setAmount(amount);
        FluidStack drained = handler.drain(scratchStack, simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE);
        return drained.getAmount();
    }
}
