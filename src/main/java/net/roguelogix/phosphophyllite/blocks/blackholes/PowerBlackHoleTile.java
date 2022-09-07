package net.roguelogix.phosphophyllite.blocks.blackholes;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.ForgeRegistries;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.energy.IEnergyTile;
import net.roguelogix.phosphophyllite.energy.IPhosphophylliteEnergyHandler;
import net.roguelogix.phosphophyllite.modular.tile.PhosphophylliteTile;
import net.roguelogix.phosphophyllite.registry.RegisterTile;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PowerBlackHoleTile extends PhosphophylliteTile implements IEnergyTile {
    
    @RegisterTile("power_black_hole")
    public static final BlockEntityType.BlockEntitySupplier<PowerBlackHoleTile> SUPPLIER = new RegisterTile.Producer<>(PowerBlackHoleTile::new);
    
    private static final Direction[] directions = Direction.values();
    private final ObjectArrayList<LazyOptional<IPhosphophylliteEnergyHandler>> handlers = new ObjectArrayList<>();
    
    {
        handlers.add(LazyOptional.empty());
        handlers.add(LazyOptional.empty());
        handlers.add(LazyOptional.empty());
        handlers.add(LazyOptional.empty());
        handlers.add(LazyOptional.empty());
        handlers.add(LazyOptional.empty());
    }
    
    private LazyOptional<IPhosphophylliteEnergyHandler> energyHandler = LazyOptional.empty();
    
    private long receivedLastTick = 0;
    private boolean doPull = false;
    private boolean allowPush = true;
    
    
    public PowerBlackHoleTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        rotateCapability(null);
    }
    
    @Override
    public LazyOptional<IPhosphophylliteEnergyHandler> energyHandler() {
        return energyHandler;
    }
    
    public void tick() {
        assert level != null;
        receivedLastTick = 0;
        if (doPull) {
            handlers.forEach(cap -> cap.ifPresent(this::sendEnergy));
        }
    }
    
    private void sendEnergy(IPhosphophylliteEnergyHandler handler) {
        final var received = handler.extractEnergy(Long.MAX_VALUE, false);
        if (receivedLastTick + received < receivedLastTick) {
            receivedLastTick = Long.MAX_VALUE;
            return;
        }
        receivedLastTick += received;
    }
    
    public void rotateCapability(@Nullable Player player) {
        energyHandler.invalidate();
        energyHandler = LazyOptional.of(() -> new IPhosphophylliteEnergyHandler() {
            
            private void ensureValid() {
                if (!Phosphophyllite.CONFIG.debugMode) {
                    return;
                }
                if (!energyHandler.isPresent()) {
                    throw new IllegalStateException("Attempt to use capability when not present");
                }
                if (energyHandler.orElse(this) != this) {
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
        });
        if (player != null) {
            player.sendSystemMessage(Component.literal("Capability invalidated"));
        }
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
        if (player.isLocalPlayer()) {
            player.sendSystemMessage(Component.literal("doPull: " + doPull + ", allowPush: " + allowPush));
        }
    }
    
    @Override
    public void onAdded() {
        for (Direction direction : directions) {
            updateCapability(direction, null, getBlockPos());
        }
    }
    
    @Override
    public void onRemoved(boolean chunkUnload) {
        energyHandler.invalidate();
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
    
    @Override
    public String getDebugString() {
        return super.getDebugString() + "\n" +
                "ReceivedLastTick: " + receivedLastTick + "\n" +
                "DoPull " + doPull + "\n" +
                "AllowPush " + allowPush;
    }
    
    public void updateCapability(Direction updateDirection, @Nullable Block oldBlock, BlockPos updatePos) {
        final var index = updateDirection.ordinal();
        final var capability = handlers.get(index);
        if (Phosphophyllite.CONFIG.debugMode && capability.isPresent() && oldBlock != null) {
            assert level != null;
            var currentBlockState = level.getBlockState(updatePos);
            if (currentBlockState.getBlock() != oldBlock) {
                Phosphophyllite.LOGGER.warn("Block updated from " + ForgeRegistries.BLOCKS.getKey(oldBlock) + " without invalidating capability");
                handlers.set(index, LazyOptional.empty());
            }
        }
        if (!capability.isPresent()) {
            handlers.set(index, findEnergyCapability(updateDirection));
        }
    }
}
