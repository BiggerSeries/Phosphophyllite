package net.roguelogix.phosphophyllite.energy;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.roguelogix.phosphophyllite.registry.OnModLoad;
import net.roguelogix.phosphophyllite.util.NonnullDefault;
import net.roguelogix.phosphophyllite.util.Pair;
import net.roguelogix.phosphophyllite.util.Util;

import javax.annotation.Nullable;
import java.lang.ref.Cleaner;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

@NonnullDefault
public class EnergyHandlerWrappers {
    
    private record WrapperRegistration<T>(BlockCapability<T, Direction> capability,
                                          Function<T, IPhosphophylliteEnergyHandler> toWrapperConstructor,
                                          Function<IPhosphophylliteEnergyHandler, T> fromWrapperConstructor,
                                          int priority) {
        IPhosphophylliteEnergyHandler wrapTo(T optional) {
            return toWrapperConstructor.apply(optional);
        }
        
        T wrapFrom(IPhosphophylliteEnergyHandler optional) {
            //noinspection ConstantConditions
            return fromWrapperConstructor.apply(optional);
        }
    }
    
    private static final ObjectArrayList<WrapperRegistration<?>> supportedCapabilitiesList = new ObjectArrayList<>();
    private static final Object2ObjectMap<BlockCapability<?, Direction>, WrapperRegistration<?>> supportedCapabilitiesMap = new Object2ObjectOpenHashMap<>();
    
    public static synchronized <T> void registerWrapper(BlockCapability<T, Direction> capability, Function<T, IPhosphophylliteEnergyHandler> toWrapperConstructor, Function<IPhosphophylliteEnergyHandler, T> fromWrapperConstructor, int priority) {
        registerWrapper(new WrapperRegistration<>(capability, toWrapperConstructor, fromWrapperConstructor, priority));
    }
    
    public static synchronized <T> void registerWrapper(WrapperRegistration<T> registration) {
        supportedCapabilitiesList.add(registration);
        final Comparator<WrapperRegistration<?>> comparator = Comparator.comparingInt(WrapperRegistration::priority);
        supportedCapabilitiesList.sort(comparator.reversed());
        supportedCapabilitiesMap.put(registration.capability, registration);
    }
    
    static {
        // ensure that identity always registered and is max priority
//        registerWrapper(IPhosphophylliteEnergyHandler.CAPABILITY, Function.identity(), Function.identity(), Integer.MAX_VALUE);
    }
    
    @Nullable
    public static IPhosphophylliteEnergyHandler findCapability(ServerLevel level, BlockPos pos, Direction direction) {
        for (int i = 0; i < supportedCapabilitiesList.size(); i++) {
            //noinspection unchecked
            @Nullable
            final var registration = (WrapperRegistration<Object>) supportedCapabilitiesList.get(i);
            if (registration == null) {
                continue;
            }
            @Nullable
            final var capabilityOptional = level.getCapability(registration.capability, pos, direction);
            if (capabilityOptional == null) {
                continue;
            }
            return registration.wrapTo(capabilityOptional);
        }
        return null;
    }
    
    @Nullable
    public static <T> T attemptWrap(BlockCapability<T, Direction> capability, @Nullable IPhosphophylliteEnergyHandler handler) {
        //noinspection unchecked
        @Nullable
        final var registration = (WrapperRegistration<T>) supportedCapabilitiesMap.get(capability);
        if (registration == null || handler == null) {
            return null;
        }
        return registration.wrapFrom(handler);
    }
    
    public static <O> List<Pair<BlockCapability<?, ?>, ICapabilityProvider<O, ?, ?>>> wrap(ICapabilityProvider<O, Direction, IPhosphophylliteEnergyHandler> baseProducer) {
        var capList = new ReferenceArrayList<Pair<BlockCapability<?, ?>, ICapabilityProvider<O, ?, ?>>>();
        for (var wrapperRegistration : supportedCapabilitiesList) {
            //noinspection unchecked
            var wrapperRegistrationGeneric = (WrapperRegistration<Object>)wrapperRegistration;
            ICapabilityProvider<O, Direction, ?> provider = (object, direction) -> {
                @Nullable
                var baseCap = baseProducer.getCapability(object, direction);
                if(baseCap == null){
                    return null;
                }
                return wrapperRegistrationGeneric.fromWrapperConstructor().apply(baseCap);
            };
            capList.add(new Pair<>(wrapperRegistration.capability, provider));
        }
        return capList;
    }
    
    private static class Neo {
        
        public static final BlockCapability<IEnergyStorage, Direction> FORGE_ENERGY_CAPABILITY = Capabilities.EnergyStorage.BLOCK;

        @OnModLoad
        public static void onModLoad() {
            registerWrapper(FORGE_ENERGY_CAPABILITY, ToWrapper::new, FromWrapper::new, Integer.MIN_VALUE);
        }

        private static class ToWrapper implements IPhosphophylliteEnergyHandler {
            
            private final IEnergyStorage forgeStorage;
            
            ToWrapper(IEnergyStorage forgeStorage) {
                this.forgeStorage = forgeStorage;
            }
            
            @Override
            public long insertEnergy(long maxInsert, boolean simulate) {
                return forgeStorage.receiveEnergy(Util.clampToInt(maxInsert), simulate);
            }
            
            @Override
            public long extractEnergy(long maxExtract, boolean simulate) {
                return forgeStorage.extractEnergy(Util.clampToInt(maxExtract), simulate);
            }
            
            @Override
            public long energyStored() {
                return forgeStorage.getEnergyStored();
            }
            
            @Override
            public long maxEnergyStored() {
                return forgeStorage.getMaxEnergyStored();
            }
        }
        
        private static class FromWrapper implements IEnergyStorage {
            
            private final IPhosphophylliteEnergyHandler phosHandler;
            
            private FromWrapper(IPhosphophylliteEnergyHandler phosHandler) {
                this.phosHandler = phosHandler;
            }
            
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                // toIntExact is fine here because it is an exceptional case if its not within the int range,
                // as you cant ask for anything outside the int range
                return Math.toIntExact(phosHandler.insertEnergy(maxReceive, simulate));
            }
            
            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                return (int) phosHandler.extractEnergy(maxExtract, simulate);
            }
            
            @Override
            public int getEnergyStored() {
                return Util.clampToInt(phosHandler.energyStored());
            }
            
            @Override
            public int getMaxEnergyStored() {
                return Util.clampToInt(phosHandler.maxEnergyStored());
            }
            
            @Override
            public boolean canExtract() {
                return true;
            }
            
            @Override
            public boolean canReceive() {
                return true;
            }
        }
    }

//    private static class Mekanism {
//
//        private static final IEnergyConversion JouleFEConversion = EnergyConversionHelper.feConversion();
//        public static final Capability<IStrictEnergyHandler> STRICT_ENERGY = CapabilityManager.get(new CapabilityToken<>() {
//        });
//
//        @OnModLoad(required = false)
//        public static void onModLoad() {
//            registerWrapper(STRICT_ENERGY, ToWrapper::new, FromWrapper::new, -1);
//        }
//
//        private static class ToWrapper implements IPhosphophylliteEnergyHandler {
//            private final IStrictEnergyHandler mekHandler;
//
//            ToWrapper(IStrictEnergyHandler mekHandler) {
//                this.mekHandler = mekHandler;
//            }
//
//            @Override
//            public long insertEnergy(long maxInsert, boolean simulate) {
//                long inserted = 0;
//                final int containers = mekHandler.getEnergyContainerCount();
//                for (int i = 0; i < containers; i++) {
//                    final var toInsertRF = FloatingLong.create(maxInsert - inserted);
//                    final var toInsertJ = JouleFEConversion.convertInPlaceFrom(toInsertRF);
//                    final var simulatedInsertRemainingJ = mekHandler.insertEnergy(i, toInsertJ, Action.SIMULATE);
//                    final var simulatedInsertedRF = JouleFEConversion.convertInPlaceTo(toInsertJ.minusEqual(simulatedInsertRemainingJ)).floor();
//                    inserted += simulatedInsertedRF.longValue();
//                    if (!simulate) {
//                        final var JAfterInsert = mekHandler.insertEnergy(i, JouleFEConversion.convertInPlaceFrom(simulatedInsertedRF), Action.EXECUTE);
//                        if (!JAfterInsert.isZero()) {
//                            throw new IllegalStateException("Mekanism energy handler state changed between simulation and push");
//                        }
//                    }
//                }
//                return inserted;
//            }
//
//            @Override
//            public long extractEnergy(long maxExtract, boolean simulate) {
//                long extracted = 0;
//                final int containers = mekHandler.getEnergyContainerCount();
//                for (int i = 0; i < containers; i++) {
//                    final var attemptExtractRF = FloatingLong.create(maxExtract - extracted);
//                    final var attemptExtractJ = JouleFEConversion.convertInPlaceFrom(attemptExtractRF);
//                    final var amountExtractedJ = mekHandler.extractEnergy(i, attemptExtractJ, Action.SIMULATE);
//                    final var ableToExtractRF = JouleFEConversion.convertInPlaceTo(amountExtractedJ).floor();
//                    extracted += ableToExtractRF.longValue();
//                    if (!simulate) {
//                        final var ableToExtractJ = JouleFEConversion.convertInPlaceTo(ableToExtractRF);
//                        final var totalExtracted = mekHandler.extractEnergy(i, ableToExtractJ, Action.EXECUTE);
//                        if (!totalExtracted.equals(ableToExtractJ)) {
//                            throw new IllegalStateException("Mekanism energy handler state changed between simulation and pull");
//                        }
//                    }
//                }
//                return extracted;
//            }
//
//            @Override
//            public long energyStored() {
//                long stored = 0;
//                final int containers = mekHandler.getEnergyContainerCount();
//                var scratchFloatingLong = FloatingLong.create(0);
//                for (int i = 0; i < containers; i++) {
//                    scratchFloatingLong = scratchFloatingLong.minusEqual(scratchFloatingLong);
//                    scratchFloatingLong = scratchFloatingLong.plusEqual(mekHandler.getEnergy(i));
//                    scratchFloatingLong = JouleFEConversion.convertInPlaceTo(scratchFloatingLong);
//                    final var containerStored = scratchFloatingLong.longValue();
//                    // overflow check
//                    if (stored + containerStored < stored) {
//                        return Long.MAX_VALUE;
//                    }
//                    stored += containerStored;
//                }
//                return stored;
//            }
//
//            @Override
//            public long maxEnergyStored() {
//                long maxStored = 0;
//                final int containers = mekHandler.getEnergyContainerCount();
//                var scratchFloatingLong = FloatingLong.create(0);
//                for (int i = 0; i < containers; i++) {
//                    scratchFloatingLong = scratchFloatingLong.minusEqual(scratchFloatingLong);
//                    scratchFloatingLong = scratchFloatingLong.plusEqual(mekHandler.getMaxEnergy(i));
//                    scratchFloatingLong = JouleFEConversion.convertInPlaceTo(scratchFloatingLong);
//                    final var containerMaxStored = scratchFloatingLong.longValue();
//                    // overflow check
//                    if (maxStored + containerMaxStored < maxStored) {
//                        return Long.MAX_VALUE;
//                    }
//                    maxStored += containerMaxStored;
//                }
//                return maxStored;
//            }
//        }
//
//        private static class FromWrapper implements IStrictEnergyHandler {
//
//            private final IPhosphophylliteEnergyHandler phosHandler;
//            private final FloatingLong referenceValue = FloatingLong.create(0);
//            private final FloatingLong returnedValue = FloatingLong.create(0);
//
//            FromWrapper(IPhosphophylliteEnergyHandler phosHandler) {
//                this.phosHandler = phosHandler;
//            }
//
//            private void verifyUnchanged() {
//                if (!referenceValue.equals(returnedValue)) {
//                    throw new IllegalStateException("Previous caller to getEnergy, getMaxEnergy, or getNeededEnergy modified returned value, this behavior is not allowed");
//                }
//            }
//
//            @Override
//            public int getEnergyContainerCount() {
//                return 1;
//            }
//
//            @Override
//            public FloatingLong getEnergy(int container) {
//                verifyUnchanged();
//                if (container != 0) {
//                    return FloatingLong.ZERO;
//                }
//                referenceValue.minusEqual(referenceValue);
//                returnedValue.minusEqual(returnedValue);
//
//                referenceValue.plusEqual(FloatingLong.create(phosHandler.energyStored()));
//                returnedValue.plusEqual(referenceValue);
//                return returnedValue;
//            }
//
//            @Override
//            public void setEnergy(int container, FloatingLong energy) {
//                verifyUnchanged();
//                throw new RuntimeException("setEnergy not implemented");
//            }
//
//            @Override
//            public FloatingLong getMaxEnergy(int container) {
//                verifyUnchanged();
//                if (container != 0) {
//                    return FloatingLong.ZERO;
//                }
//                referenceValue.minusEqual(referenceValue);
//                returnedValue.minusEqual(returnedValue);
//
//                referenceValue.plusEqual(JouleFEConversion.convertInPlaceFrom(FloatingLong.create(phosHandler.maxEnergyStored())));
//                returnedValue.plusEqual(referenceValue);
//
//                return returnedValue;
//            }
//
//            @Override
//            public FloatingLong getNeededEnergy(int container) {
//                verifyUnchanged();
//                if (container != 0) {
//                    return FloatingLong.ZERO;
//                }
//
//                referenceValue.minusEqual(referenceValue);
//                returnedValue.minusEqual(returnedValue);
//
//                referenceValue.plusEqual(JouleFEConversion.convertInPlaceFrom(FloatingLong.create(phosHandler.maxEnergyStored() - phosHandler.energyStored())));
//                returnedValue.plusEqual(referenceValue);
//
//                return returnedValue;
//            }
//
//            @Override
//            public FloatingLong insertEnergy(int container, FloatingLong amount, Action action) {
//                verifyUnchanged();
//                if (container != 0) {
//                    return FloatingLong.ZERO;
//                }
//                return insertEnergy(amount, action);
//            }
//
//            @Override
//            public FloatingLong extractEnergy(int container, FloatingLong amount, Action action) {
//                verifyUnchanged();
//                if (container != 0) {
//                    return FloatingLong.ZERO;
//                }
//                return extractEnergy(amount, action);
//            }
//
//            @Override
//            public FloatingLong insertEnergy(FloatingLong amount, Action action) {
//                verifyUnchanged();
//                final var toInsert = JouleFEConversion.convertTo(amount).longValue();
//                final var inserted = phosHandler.insertEnergy(toInsert, action.simulate());
//                final var remaining = toInsert - inserted;
//                if (remaining == 0) {
//                    return FloatingLong.ZERO;
//                }
//                return JouleFEConversion.convertInPlaceFrom(FloatingLong.create(remaining));
//            }
//
//            @Override
//            public FloatingLong extractEnergy(FloatingLong amount, Action action) {
//                verifyUnchanged();
//                final var toExtract = JouleFEConversion.convertTo(amount).longValue();
//                final var extracted = phosHandler.extractEnergy(toExtract, action.simulate());
//                if (extracted == 0) {
//                    return FloatingLong.ZERO;
//                }
//                return JouleFEConversion.convertInPlaceFrom(FloatingLong.create(extracted));
//            }
//        }
//    }
}
