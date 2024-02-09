package net.roguelogix.phosphophyllite.blocks.whiteholes;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.roguelogix.phosphophyllite.modular.tile.PhosphophylliteTile;
import net.roguelogix.phosphophyllite.registry.CapabilityRegistration;
import net.roguelogix.phosphophyllite.registry.RegisterCapability;
import net.roguelogix.phosphophyllite.registry.RegisterTile;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import static net.neoforged.neoforge.fluids.FluidStack.loadFluidStackFromNBT;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FluidWhiteHoleTile extends PhosphophylliteTile implements IFluidHandler {
    
    @RegisterTile("fluid_white_hole")
    public static final BlockEntityType.BlockEntitySupplier<FluidWhiteHoleTile> SUPPLIER = new RegisterTile.Producer<>(FluidWhiteHoleTile::new);
    
    @RegisterCapability
    private static final CapabilityRegistration FLUID_HANDLER_CAP_REGISTRATION = CapabilityRegistration.tileCap(Capabilities.FluidHandler.BLOCK, FluidWhiteHoleTile.class);
    
    public FluidWhiteHoleTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
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
    public CompoundTag writeNBT() {
        var compound = super.writeNBT();
        compound.put("fluidstack", fluidStack.writeToNBT(new CompoundTag()));
        return compound;
    }
    
    @Override
    public void readNBT(CompoundTag compound) {
        fluidStack = loadFluidStackFromNBT(compound.getCompound("fluidstack"));
        super.readNBT(compound);
    }
    
    public void tick() {
        assert level != null;
        for (Direction direction : Direction.values()) {
            var cap = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(direction), direction.getOpposite());
            if(cap != null){
                cap.fill(fluidStack.copy(), FluidAction.EXECUTE);
            }
        }
    }
}
