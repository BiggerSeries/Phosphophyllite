package net.roguelogix.phosphophyllite.registry;

import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.ForgeFlowingFluid;

import javax.annotation.Nonnull;

public class PhosphophylliteFluid extends ForgeFlowingFluid {
    
    protected PhosphophylliteFluid(Properties properties) {
        super(properties);
//        setDefaultState(getDefaultState().with(LEVEL_1_8, 8));
    }
    
    @Override
    protected void createFluidStateDefinition(@Nonnull StateDefinition.Builder<Fluid, FluidState> builder) {
        super.createFluidStateDefinition(builder);
//        builder.add(LEVEL_1_8);
    }
    
    boolean isSource = false;
    PhosphophylliteFluid flowingVariant;
    
    
    @Override
    public boolean isSource(FluidState state) {
        return isSource;
    }
    
    @Override
    public int getAmount(FluidState state) {
        return state.getAmount();
    }
}
