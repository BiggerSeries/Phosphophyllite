package net.roguelogix.phosphophyllite.fluids;

import mekanism.api.Action;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.api.recipes.RotaryRecipe;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.roguelogix.phosphophyllite.registry.OnModLoad;
import net.roguelogix.phosphophyllite.threading.WorkQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MekanismGasWrappers {
    
    public static IGasHandler wrap(IPhosphophylliteFluidHandler fluidHandler) {
        return new MekanismGasWrappers.FluidToGasWrapper(fluidHandler);
    }
    
    public static IPhosphophylliteFluidHandler wrap(IGasHandler gasHandler) {
        return new MekanismGasWrappers.GasToFluidWrapper(gasHandler);
    }
    
    public static IGasHandler EMPTY_TANK;
    
    static {
        EMPTY_TANK = new IGasHandler() {
            @Override
            public int getTanks() {
                return 0;
            }
            
            @Override
            public GasStack getChemicalInTank(int tank) {
                return GasStack.EMPTY;
            }
            
            @Override
            public void setChemicalInTank(int tank, GasStack stack) {
            }
            
            @Override
            public long getTankCapacity(int tank) {
                return 0;
            }
            
            @Override
            public boolean isValid(int tank, GasStack stack) {
                return false;
            }
            
            @Override
            public GasStack insertChemical(int tank, GasStack stack, Action action) {
                return stack;
            }
            
            @Override
            public GasStack extractChemical(int tank, long amount, Action action) {
                return GasStack.EMPTY;
            }
        };
    }
    
    private static final Logger LOGGER = LogManager.getLogger("Phosphophyllite/MekanismIntegration");
    
    private static class Mapping {
        final List<Gas> gases = new ArrayList<>();
        final List<GasStack> gasStacks = new ArrayList<>();
        long gasToFluidGasUnits = -1;
        long gasToFluidFluidUnits = -1;
        final List<Fluid> fluids = new ArrayList<>();
        final List<FluidStack> fluidStacks = new ArrayList<>();
        long fluidToGasGasUnits = -1;
        long fluidToGasFluidUnits = -1;
    }
    
    private static final Map<Gas, Mapping> gasToFluidMap = new HashMap<>();
    private static final Map<Fluid, Mapping> fluidToGasMap = new HashMap<>();
    
    @OnModLoad
    private static void onModLoad() {
        MinecraftForge.EVENT_BUS.addListener(MekanismGasWrappers::addReloadEventListener);
        MinecraftForge.EVENT_BUS.addListener(MekanismGasWrappers::serverAboutToStart);
        MinecraftForge.EVENT_BUS.addListener(MekanismGasWrappers::serverStopped);
    }
    
    private static void addReloadEventListener(AddReloadListenerEvent event) {
        reloadQueue.enqueue(MekanismGasWrappers::reloadMappings);
        if (server != null) {
            reloadQueue.runAll();
        }
    }
    
    @Nullable
    private static MinecraftServer server;
    
    private static final WorkQueue reloadQueue = new WorkQueue();
    
    private static void serverAboutToStart(ServerAboutToStartEvent event) {
        server = event.getServer();
        reloadQueue.runAll();
    }
    
    
    private static void serverStopped(ServerStoppedEvent event) {
        server = null;
        reloadQueue.runAll();
    }
    
    
    private static long GCD(long A, long B) {
        long A2 = A >> 1;
        long B2 = B >> 1;
        
        long GCD = 1;
        for (long i = 2; i < A2 && i < B2; i++) {
            long Ai = (A / i) * i;
            long Bi = (B / i) * i;
            if (Ai == A && Bi == B) {
                GCD = i;
            }
        }
        
        return GCD;
    }
    
    private static void reloadMappings() {
        
        // forces the wrappers to update their mappings
        gasToFluidMap.forEach((k, v) -> {
            v.fluids.clear();
            v.gases.clear();
        });
        fluidToGasMap.forEach((k, v) -> {
            v.fluids.clear();
            v.gases.clear();
        });
        gasToFluidMap.clear();
        fluidToGasMap.clear();
        
        @SuppressWarnings("unchecked")
        RecipeType<RotaryRecipe> type = (RecipeType<RotaryRecipe>) Registry.RECIPE_TYPE.get(new ResourceLocation("mekanism:rotary"));
        if (type == null || server == null) {
            return;
        }
        List<RotaryRecipe> recipes = server.getRecipeManager().getAllRecipesFor(type);
        
        for (RotaryRecipe recipe : recipes) {
            Mapping mapping = new Mapping();
            if (recipe.hasGasToFluid()) {
                
                List<GasStack> inputs = recipe.getGasInput().getRepresentations();
                for (GasStack input : inputs) {
                    Gas gas = input.getType();
                    long amount = input.getAmount();
                    if (mapping.gasToFluidGasUnits != -1 && mapping.gasToFluidGasUnits != amount) {
                        LOGGER.warn("Input amount discrepancy in rotary recipe " + recipe.getId() + " with gas " + gas.getName() + " wanting " + amount + " input while a different gas wants " + mapping.gasToFluidGasUnits);
                        continue;
                    }
                    if (!mapping.gases.contains(gas)) {
                        mapping.gases.add(gas);
                    }
                    mapping.gasToFluidGasUnits = amount;
                }
                
                FluidStack output = recipe.getFluidOutputDefinition().get(0);
                if (!mapping.fluids.contains(output.getRawFluid())) {
                    mapping.fluids.add(output.getRawFluid());
                }
                mapping.gasToFluidFluidUnits = output.getAmount();
                
                if (mapping.gasToFluidGasUnits <= 0 || mapping.gasToFluidFluidUnits <= 0) {
                    mapping.gasToFluidGasUnits = -1;
                    mapping.gasToFluidFluidUnits = -1;
                }
                
                long GCD = GCD(mapping.gasToFluidGasUnits, mapping.gasToFluidFluidUnits);
                mapping.gasToFluidGasUnits /= GCD;
                mapping.gasToFluidFluidUnits /= GCD;
            }
            if (recipe.hasFluidToGas()) {
                
                List<FluidStack> inputs = recipe.getFluidInput().getRepresentations();
                for (FluidStack input : inputs) {
                    Fluid fluid = input.getRawFluid();
                    long amount = input.getAmount();
                    if (mapping.gasToFluidGasUnits != -1 && mapping.gasToFluidGasUnits != amount) {
                        LOGGER.warn("Input amount discrepancy in rotary recipe " + recipe.getId() + " with fluid " + ForgeRegistries.FLUIDS.getKey(fluid) + " wanting " + amount + " input while a different gas wants " + mapping.fluidToGasFluidUnits);
                        continue;
                    }
                    if (!mapping.fluids.contains(fluid)) {
                        mapping.fluids.add(fluid);
                    }
                    mapping.fluidToGasFluidUnits = amount;
                }
                
                GasStack output = recipe.getGasOutputDefinition().get(0);
                if (!mapping.gases.contains(output.getRaw())) {
                    mapping.gases.add(output.getRaw());
                }
                mapping.fluidToGasGasUnits = output.getAmount();
                
                if (mapping.fluidToGasGasUnits <= 0 || mapping.fluidToGasFluidUnits <= 0) {
                    mapping.fluidToGasGasUnits = -1;
                    mapping.fluidToGasFluidUnits = -1;
                }
                
                long GCD = GCD(mapping.fluidToGasGasUnits, mapping.fluidToGasFluidUnits);
                mapping.fluidToGasGasUnits /= GCD;
                mapping.fluidToGasFluidUnits /= GCD;
            }
            
            for (Gas gas : mapping.gases) {
                Mapping oldMapping = gasToFluidMap.put(gas, mapping);
                if (oldMapping != null) {
                    LOGGER.warn("Duplicate gas entry for gas " + gas.getName());
                }
                mapping.gasStacks.add(gas.getStack(0));
            }
            for (Fluid fluid : mapping.fluids) {
                Mapping oldMapping = fluidToGasMap.put(fluid, mapping);
                if (oldMapping != null) {
                    LOGGER.warn("Duplicate fluid entry for fluid " + ForgeRegistries.FLUIDS.getKey(fluid));
                }
                mapping.fluidStacks.add(new FluidStack(fluid, 0));
            }
        }
    }
    
    private static void removeMapping(Mapping mapping) {
        for (Gas gas : mapping.gases) {
            gasToFluidMap.remove(gas);
        }
        for (Fluid fluid : mapping.fluids) {
            fluidToGasMap.remove(fluid);
        }
        mapping.gases.clear();
        mapping.fluids.clear();
    }
    
    
    private static class GasToFluidWrapper implements IPhosphophylliteFluidHandler {
        IGasHandler gasHandler;
        @Nullable
        Mapping lastMapping;
        
        GasToFluidWrapper(IGasHandler handler) {
            gasHandler = handler;
        }
        
        @Override
        public int tankCount() {
            return gasHandler.getTanks();
        }
        
        @Override
        public long tankCapacity(int tank) {
            // this isn't 100% accurate, but i cant do much better
            // in most cases its 1:1 anyway, so, shouldn't matter
            return gasHandler.getTankCapacity(tank);
        }
        
        @Override
        public Fluid fluidTypeInTank(int tank) {
            GasStack gasStack = gasHandler.getChemicalInTank(tank);
            if (lastMapping == null || !lastMapping.gases.contains(gasStack.getRaw())) {
                Mapping map = gasToFluidMap.get(gasStack.getRaw());
                if (map == null) {
                    return Fluids.EMPTY;
                }
                lastMapping = map;
            }
            if (lastMapping.fluids.isEmpty()) {
                LOGGER.error("Gas mapping for " + gasStack.getRaw().getName() + " has zero fluid elements, removing");
                removeMapping(lastMapping);
                lastMapping = null;
                return Fluids.EMPTY;
            }
            return lastMapping.fluids.get(0);
        }
        
        @Override
        public CompoundTag fluidTagInTank(int tank) {
            return null;
        }
        
        @Override
        public long fluidAmountInTank(int tank) {
            if (tank > tankCount()) {
                return 0;
            }
            GasStack gasStack = gasHandler.getChemicalInTank(tank);
            if (lastMapping == null || !lastMapping.gases.contains(gasStack.getRaw())) {
                Mapping map = gasToFluidMap.get(gasStack.getRaw());
                if (map == null) {
                    return 0;
                }
                lastMapping = map;
            }
            long amount = gasStack.getAmount();
            amount *= lastMapping.gasToFluidFluidUnits;
            amount /= lastMapping.gasToFluidGasUnits;
            return amount;
        }
        
        @Override
        public boolean fluidValidForTank(int tank, Fluid fluid) {
            if (lastMapping == null || !lastMapping.fluids.contains(fluid)) {
                Mapping map = fluidToGasMap.get(fluid);
                if (map == null) {
                    return false;
                }
                lastMapping = map;
            }
            if (lastMapping.gases.isEmpty()) {
                LOGGER.error("Fluid mapping for " + ForgeRegistries.FLUIDS.getKey(fluid) + " has zero gas elements, removing");
                removeMapping(lastMapping);
                lastMapping = null;
                return false;
            }
            // ok, *technically* i should check against all of them, but chances are, i dont need to
            return gasHandler.isValid(tank, lastMapping.gasStacks.get(0));
        }
        
        @Override
        public long fill(Fluid fluid, @Nullable CompoundTag tag, long amount, boolean simulate) {
            if (tag != null || fluid == Fluids.EMPTY) {
                return 0;
            }
            if (lastMapping == null || !lastMapping.fluids.contains(fluid)) {
                Mapping map = fluidToGasMap.get(fluid);
                if (map == null) {
                    return 0;
                }
                lastMapping = map;
            }
            if (lastMapping.gases.isEmpty()) {
                LOGGER.error("Fluid mapping for " + ForgeRegistries.FLUIDS.getKey(fluid) + " has zero gas elements, removing");
                removeMapping(lastMapping);
                lastMapping = null;
                return 0;
            }
            if (lastMapping.fluidToGasGasUnits <= 0) {
                return 0;
            }
            GasStack stack = lastMapping.gasStacks.get(0);
            long gasAmount = amount;
            gasAmount *= lastMapping.fluidToGasGasUnits;
            gasAmount /= lastMapping.fluidToGasFluidUnits;
            stack.setAmount(gasAmount);
            stack = gasHandler.insertChemical(stack, Action.get(!simulate));
            long remainingFluid = stack.getAmount();
            remainingFluid *= lastMapping.fluidToGasFluidUnits;
            remainingFluid /= lastMapping.fluidToGasGasUnits;
            return amount - remainingFluid;
        }
        
        @Override
        public long drain(Fluid fluid, @Nullable CompoundTag tag, long amount, boolean simulate) {
            if (tag != null || fluid == Fluids.EMPTY) {
                return 0;
            }
            if (lastMapping == null || !lastMapping.fluids.contains(fluid)) {
                Mapping map = fluidToGasMap.get(fluid);
                if (map == null) {
                    return 0;
                }
                lastMapping = map;
            }
            if (lastMapping.gases.isEmpty()) {
                LOGGER.error("Fluid mapping for " + ForgeRegistries.FLUIDS.getKey(fluid) + " has zero gas elements, removing");
                removeMapping(lastMapping);
                lastMapping = null;
                return 0;
            }
            if (lastMapping.gasToFluidFluidUnits <= 0) {
                return 0;
            }
            GasStack stack = lastMapping.gasStacks.get(0);
            long gasAmount = amount;
            gasAmount *= lastMapping.gasToFluidFluidUnits;
            gasAmount /= lastMapping.gasToFluidGasUnits;
            stack.setAmount(gasAmount);
            stack = gasHandler.extractChemical(stack, Action.get(!simulate));
            long drained = stack.getAmount();
            drained *= lastMapping.gasToFluidGasUnits;
            drained /= lastMapping.gasToFluidFluidUnits;
            return drained;
        }
    }
    
    private static class FluidToGasWrapper implements IGasHandler {
        
        IPhosphophylliteFluidHandler fluidHandler;
        Mapping lastMapping;
        
        private FluidToGasWrapper(IPhosphophylliteFluidHandler handler) {
            fluidHandler = handler;
        }
        
        @Override
        public int getTanks() {
            return fluidHandler.tankCount();
        }
        
        @Override
        public GasStack getChemicalInTank(int tank) {
            Fluid fluid = fluidHandler.fluidTypeInTank(tank);
            long amount = fluidHandler.fluidAmountInTank(tank);
            if (lastMapping == null || !lastMapping.fluids.contains(fluid)) {
                Mapping map = fluidToGasMap.get(fluid);
                if (map == null) {
                    return GasStack.EMPTY;
                }
                lastMapping = map;
            }
            if (lastMapping.gases.isEmpty()) {
                LOGGER.error("Fluid mapping for " + ForgeRegistries.FLUIDS.getKey(fluid) + " has zero gas elements, removing");
                removeMapping(lastMapping);
                lastMapping = null;
                return GasStack.EMPTY;
            }
            amount *= lastMapping.fluidToGasGasUnits;
            amount /= lastMapping.fluidToGasFluidUnits;
            GasStack stack = lastMapping.gasStacks.get(0);
            stack.setAmount(amount);
            return stack;
        }
        
        @Override
        public void setChemicalInTank(int tank, GasStack stack) {
            throw new RuntimeException("Not implemented for this handler");
        }
        
        @Override
        public long getTankCapacity(int tank) {
            return fluidHandler.getTankCapacity(tank);
        }
        
        @Override
        public boolean isValid(int tank, GasStack stack) {
            if (lastMapping == null || !lastMapping.gases.contains(stack.getRaw())) {
                Mapping map = gasToFluidMap.get(stack.getRaw());
                if (map == null) {
                    return false;
                }
                lastMapping = map;
            }
            if (lastMapping.fluids.isEmpty()) {
                LOGGER.error("Gas mapping for " + stack.getRaw().getRegistryName() + " has zero fluid elements, removing");
                removeMapping(lastMapping);
                lastMapping = null;
                return false;
            }
            // ok, *technically* i should check against all of them, but chances are, i dont need to
            return fluidHandler.fluidValidForTank(tank, lastMapping.fluids.get(0));
        }
        
        @Override
        public GasStack insertChemical(int tank, GasStack stack, Action action) {
            if (lastMapping == null || !lastMapping.gases.contains(stack.getRaw())) {
                Mapping map = gasToFluidMap.get(stack.getRaw());
                if (map == null) {
                    return stack;
                }
                lastMapping = map;
            }
            if (lastMapping.fluids.isEmpty()) {
                LOGGER.error("Gas mapping for " + stack.getRaw().getRegistryName() + " has zero fluid elements, removing");
                removeMapping(lastMapping);
                lastMapping = null;
                return stack;
            }
            if (lastMapping.gasToFluidFluidUnits <= 0) {
                return stack;
            }
            Fluid fluid = lastMapping.fluids.get(0);
            long amount = stack.getAmount();
            amount *= lastMapping.gasToFluidFluidUnits;
            amount /= lastMapping.gasToFluidGasUnits;
            long filled = fluidHandler.fill(fluid, null, amount, action.simulate());
            if (filled == 0) {
                return stack;
            }
            if (filled == amount) {
                return GasStack.EMPTY;
            }
            long remaining = amount - filled;
            remaining *= lastMapping.gasToFluidGasUnits;
            remaining /= lastMapping.gasToFluidFluidUnits;
            return new GasStack(stack, remaining);
        }
        
        @Override
        public GasStack extractChemical(int tank, long gasAmount, Action action) {
            Fluid fluid = fluidHandler.fluidTypeInTank(tank);
            if (lastMapping == null || !lastMapping.fluids.contains(fluid)) {
                Mapping map = fluidToGasMap.get(fluid);
                if (map == null) {
                    return GasStack.EMPTY;
                }
                lastMapping = map;
            }
            if (lastMapping.gases.isEmpty()) {
                LOGGER.error("Fluid mapping for " + fluid + " has zero gas elements, removing");
                removeMapping(lastMapping);
                lastMapping = null;
                return GasStack.EMPTY;
            }
            if (lastMapping.fluidToGasFluidUnits <= 0) {
                return GasStack.EMPTY;
            }
            long amount = gasAmount;
            amount *= lastMapping.fluidToGasFluidUnits;
            amount /= lastMapping.fluidToGasGasUnits;
            long drained = fluidHandler.drain(fluid, null, amount, action.simulate());
            if (drained == 0) {
                return GasStack.EMPTY;
            }
            long gasDrained = amount - drained;
            gasDrained *= lastMapping.fluidToGasGasUnits;
            gasDrained /= lastMapping.fluidToGasFluidUnits;
            return new GasStack(lastMapping.gases.get(0), gasDrained);
        }
    }
    
}
