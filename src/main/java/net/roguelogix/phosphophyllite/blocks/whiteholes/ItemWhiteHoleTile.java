package net.roguelogix.phosphophyllite.blocks.whiteholes;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import net.roguelogix.phosphophyllite.modular.tile.PhosphophylliteTile;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@RegisterTileEntity(name = "item_white_hole")
public class ItemWhiteHoleTile extends PhosphophylliteTile implements IItemHandler {
    
    @RegisterTileEntity.Type
    public static BlockEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final BlockEntityType.BlockEntitySupplier<ItemWhiteHoleTile> SUPPLIER = ItemWhiteHoleTile::new;
    
    public ItemWhiteHoleTile(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
    @Nonnull
    @Override
    public <T> LazyOptional<T> capability(@Nonnull Capability<T> cap, final @Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return LazyOptional.of(() -> this).cast();
        }
        return super.capability(cap, side);
    }
    
    Item item = null;
    
    public void setItem(Item item) {
        this.item = item;
    }
    
    @Nonnull
    @Override
    public CompoundTag writeNBT() {
        var compound = super.writeNBT();
        if (item != null) {
            compound.putString("item", Objects.requireNonNull(item.getRegistryName()).toString());
        }
        return compound;
    }
    
    @Override
    public void readNBT(@Nonnull CompoundTag compound) {
        super.readNBT(compound);
        if (compound.contains("item")) {
            item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(compound.getString("item")));
        }
    }
    
    public void tick() {
        if (item != null) {
            assert level != null;
            for (Direction direction : Direction.values()) {
                BlockEntity te = level.getBlockEntity(worldPosition.relative(direction));
                if (te != null) {
                    te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction.getOpposite()).ifPresent(c -> {
                        for (int i = 0; i < c.getSlots(); i++) {
                            //noinspection deprecation
                            c.insertItem(i, new ItemStack(item, item.getMaxStackSize()), false);
                        }
                    });
                }
            }
        }
    }
    
    @Override
    public int getSlots() {
        return 128;
    }
    
    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        //noinspection deprecation
        return new ItemStack(item, item.getMaxStackSize());
    }
    
    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        return stack;
    }
    
    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return new ItemStack(item, amount);
    }
    
    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }
    
    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return true;
    }
}
