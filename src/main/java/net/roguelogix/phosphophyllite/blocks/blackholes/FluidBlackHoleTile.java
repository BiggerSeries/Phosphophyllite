package net.roguelogix.phosphophyllite.blocks.blackholes;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.capabilities.Capabilities;
import net.neoforged.neoforge.common.capabilities.Capability;
import net.neoforged.neoforge.common.util.LazyOptional;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.roguelogix.phosphophyllite.modular.tile.PhosphophylliteTile;
import net.roguelogix.phosphophyllite.registry.RegisterTile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FluidBlackHoleTile extends PhosphophylliteTile implements IFluidHandler {
    
    @RegisterTile("fluid_black_hole")
    public static final BlockEntityType.BlockEntitySupplier<FluidBlackHoleTile> SUPPLIER = new RegisterTile.Producer<>(FluidBlackHoleTile::new);
    
    public FluidBlackHoleTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
    @Nonnull
    @Override
    public <T> LazyOptional<T> capability(@Nonnull Capability<T> cap, final @Nullable Direction side) {
        if (cap == Capabilities.FLUID_HANDLER) {
            return LazyOptional.of(() -> this).cast();
        }
        return super.capability(cap, side);
    }
    
    @Override
    public int getTanks() {
        return 1;
    }
    
    @Nonnull
    @Override
    public FluidStack getFluidInTank(int tank) {
        return FluidStack.EMPTY;
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
        return resource.getAmount();
    }
    
    @Nonnull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return FluidStack.EMPTY;
    }
    
    @Nonnull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return FluidStack.EMPTY;
    }
}
