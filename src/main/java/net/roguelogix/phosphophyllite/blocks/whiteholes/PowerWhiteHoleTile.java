package net.roguelogix.phosphophyllite.blocks.whiteholes;

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
public class PowerWhiteHoleTile extends PhosphophylliteTile implements IEnergyTile {
    
    @RegisterTile("power_white_hole")
    public static final BlockEntityType.BlockEntitySupplier<PowerWhiteHoleTile> SUPPLIER = new RegisterTile.Producer<>(PowerWhiteHoleTile::new);
    
    public PowerWhiteHoleTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        rotateCapability(null);
    }
    
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
    
    private long sentLastTick = 0;
    private boolean doPush;
    private boolean allowPull;
    
    @Override
    public LazyOptional<IPhosphophylliteEnergyHandler> energyHandler() {
        return energyHandler;
    }
    
    public void tick() {
        assert level != null;
        sentLastTick = 0;
        if (doPush) {
            handlers.forEach(cap -> cap.ifPresent(this::sendEnergy));
        }
    }
    
    private void sendEnergy(IPhosphophylliteEnergyHandler handler) {
        final var sent = handler.insertEnergy(Long.MAX_VALUE, false);
        if (sentLastTick + sent < sentLastTick) {
            sentLastTick = Long.MAX_VALUE;
            return;
        }
        sentLastTick += sent;
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
                return 0;
            }
            
            @Override
            public long extractEnergy(long maxExtract, boolean simulate) {
                ensureValid();
                if (maxExtract < 0) {
                    if (Phosphophyllite.CONFIG.debugMode) {
                        throw new IllegalStateException("Something tried to extract negative power");
                    }
                    return 0;
                }
                if (!allowPull) {
                    return 0;
                }
                if (!simulate) {
                    if (sentLastTick + maxExtract < sentLastTick) {
                        sentLastTick = Long.MAX_VALUE;
                        return maxExtract;
                    }
                    sentLastTick += maxExtract;
                }
                return maxExtract;
            }
            
            @Override
            public long energyStored() {
                ensureValid();
                return Long.MAX_VALUE;
            }
            
            @Override
            public long maxEnergyStored() {
                ensureValid();
                return Long.MAX_VALUE;
            }
        });
        if(player != null) {
            player.sendSystemMessage(Component.literal("Capability invalidated"));
        }
    }
    
    public void nextOption(Player player) {
        if (doPush) {
            if (allowPull) {
                allowPull = false;
            } else {
                allowPull = true;
                doPush = false;
            }
        } else {
            doPush = true;
            allowPull = true;
        }
        if (player.isLocalPlayer()) {
            player.sendSystemMessage(Component.literal("doPush: " + doPush + ", allowPull: " + allowPull));
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
        doPush = compound.getBoolean("doPush");
        allowPull = compound.getBoolean("allowPull");
    }
    
    @Override
    protected CompoundTag writeNBT() {
        final var tag = new CompoundTag();
        tag.putBoolean("doPush", doPush);
        tag.putBoolean("allowPull", allowPull);
        return tag;
    }
    
    @Override
    public String getDebugString() {
        return super.getDebugString() + "\n" +
                "SentLastTick: " + sentLastTick + "\n" +
                "DoPush " + doPush + "\n" +
                "AllowPull " + allowPull;
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
