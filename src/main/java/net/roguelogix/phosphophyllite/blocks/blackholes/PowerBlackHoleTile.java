package net.roguelogix.phosphophyllite.blocks.blackholes;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.capability.CachedWrappedBlockCapability;
import net.roguelogix.phosphophyllite.debug.DebugInfo;
import net.roguelogix.phosphophyllite.energy.IEnergyTile;
import net.roguelogix.phosphophyllite.energy.IPhosphophylliteEnergyHandler;
import net.roguelogix.phosphophyllite.modular.tile.PhosphophylliteTile;
import net.roguelogix.phosphophyllite.registry.RegisterTile;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@NonnullDefault
public class PowerBlackHoleTile extends PhosphophylliteTile implements IEnergyTile {
    
    @RegisterTile("power_black_hole")
    public static final BlockEntityType.BlockEntitySupplier<PowerBlackHoleTile> SUPPLIER = new RegisterTile.Producer<>(PowerBlackHoleTile::new);
    
    private static final Direction[] directions = Direction.values();
    private final ObjectArrayList<CachedWrappedBlockCapability<IPhosphophylliteEnergyHandler, Direction>> handlers = new ObjectArrayList<>();
    
    private IPhosphophylliteEnergyHandler energyHandler;
    
    private long receivedLastLastTick = 0;
    private long receivedLastTick = 0;
    private boolean doPull = false;
    private boolean allowPush = true;
    
    
    public PowerBlackHoleTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        energyHandler = createNewEnergyHandler();
    }
    
    @Override
    public IPhosphophylliteEnergyHandler energyHandler(Direction direction) {
        return energyHandler;
    }
    
    public void tick() {
        assert level != null;
        if(level.isClientSide){
            return;
        }
        receivedLastLastTick = receivedLastTick;
        receivedLastTick = 0;
        if (doPull) {
            handlers.forEach(capCache -> {
                @Nullable
                var cap = capCache.getCapability();
                if (cap != null) {
                    pullEnergy(cap);
                }
            });
        }
    }
    
    private void pullEnergy(IPhosphophylliteEnergyHandler handler) {
        final var received = handler.extractEnergy(Long.MAX_VALUE, false);
        if (receivedLastTick + received < receivedLastTick) {
            receivedLastTick = Long.MAX_VALUE;
            return;
        }
        receivedLastTick += received;
    }
    
    public void rotateCapability(Player player) {
        if (!player.isLocalPlayer()) {
            energyHandler = createNewEnergyHandler();
            player.sendSystemMessage(Component.literal("Capability invalidated"));
        }
    }
    
    private IPhosphophylliteEnergyHandler createNewEnergyHandler() {
        return new IPhosphophylliteEnergyHandler() {
            private void ensureValid() {
                if (!Phosphophyllite.CONFIG.debugMode) {
                    return;
                }
                if (energyHandler != this) {
                    throw new IllegalStateException("Attempt to use capability after invalidate");
                }
            }
            
            @Override
            public long insertEnergy(long maxInsert, boolean simulate) {
                ensureValid();
                if (maxInsert < 0) {
                    if (Phosphophyllite.CONFIG.debugMode) {
                        throw new IllegalStateException("Something tried to insert negative power");
                    }
                    return 0;
                }
                if (!simulate) {
                    if (receivedLastTick + maxInsert < receivedLastTick) {
                        receivedLastTick = Long.MAX_VALUE;
                        return maxInsert;
                    }
                    receivedLastTick += maxInsert;
                }
                return maxInsert;
            }
            
            @Override
            public long extractEnergy(long maxExtract, boolean simulate) {
                ensureValid();
                if (maxExtract < 0) {
                    if (Phosphophyllite.CONFIG.debugMode) {
                        throw new IllegalStateException("Something tried to insert negative power");
                    }
                    return 0;
                }
                return 0;
            }
            
            @Override
            public long energyStored() {
                ensureValid();
                return 0;
            }
            
            @Override
            public long maxEnergyStored() {
                ensureValid();
                return Long.MAX_VALUE;
            }
        };
    }
    
    public void nextOption(Player player) {
        if (doPull) {
            if (allowPush) {
                allowPush = false;
            } else {
                allowPush = true;
                doPull = false;
            }
        } else {
            doPull = true;
            allowPush = true;
        }
        if (!player.isLocalPlayer()) {
            player.sendSystemMessage(Component.literal("doPull: " + doPull + ", allowPush: " + allowPush));
        }
    }
    
    @Override
    public void onAdded() {
        assert level != null;
        if(level.isClientSide){
            return;
        }
        for (var value : directions) {
            handlers.add(this.findEnergyCapability(value));
        }
    }
    
    @Override
    protected void readNBT(CompoundTag compound) {
        doPull = compound.getBoolean("doPull");
        allowPush = compound.getBoolean("allowPush");
    }
    
    @Override
    protected CompoundTag writeNBT() {
        final var tag = new CompoundTag();
        tag.putBoolean("doPull", doPull);
        tag.putBoolean("allowPush", allowPush);
        return tag;
    }
    
    @Nonnull
    @Override
    public DebugInfo getDebugInfo() {
        return super.getDebugInfo()
                .add("ReceivedLastTick: " + receivedLastLastTick)
                .add("DoPull " + doPull)
                .add("AllowPush " + allowPush)
                ;
    }

//    public void updateCapability(Direction updateDirection, @Nullable Block oldBlock, BlockPos updatePos) {
//        final var index = updateDirection.ordinal();
//        final var capability = handlers.get(index);
//        if (Phosphophyllite.CONFIG.debugMode && capability != null && oldBlock != null) {
//            assert level != null;
//            var currentBlockState = level.getBlockState(updatePos);
//            if (currentBlockState.getBlock() != oldBlock) {
//                Phosphophyllite.LOGGER.warn("Block updated from " + BuiltInRegistries.BLOCK.getKey(oldBlock) + " without invalidating capability");
//                handlers.set(index, null);
//            }
//        }
//        if (capability == null) {
//            handlers.set(index, findEnergyCapability(updateDirection));
//        }
//    }
}
