package net.roguelogix.phosphophyllite.fluids;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

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
    public CompoundNBT fluidTagInTank(int tank) {
        return handler.getFluidInTank(tank).getTag();
    }
    
    @Override
    public long fluidAmountInTank(int tank) {
        return handler.getFluidInTank(tank).getAmount();
    }
    
    PhosphophylliteFluidStack scratchStack = new PhosphophylliteFluidStack();
    
    @Override
    public boolean fluidValidForTank(int tank, Fluid fluid) {
        scratchStack.setFluid(fluid);
        scratchStack.setAmount(0);
        return handler.isFluidValid(tank, scratchStack);
    }
    
    @Override
    public long fill(Fluid fluid, @Nullable CompoundNBT tag, long amount, boolean simulate) {
        scratchStack.setFluid(fluid);
        scratchStack.setTag(tag);
        scratchStack.setAmount(amount);
        return handler.fill(scratchStack, simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE);
    }
    
    @Override
    public long drain(Fluid fluid, @Nullable CompoundNBT tag, long amount, boolean simulate) {
        scratchStack.setFluid(fluid);
        scratchStack.setTag(tag);
        scratchStack.setAmount(amount);
        FluidStack drained = handler.drain(scratchStack, simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE);
        return drained.getAmount();
    }
}
