package net.roguelogix.phosphophyllite.blocks.whiteholes;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.roguelogix.phosphophyllite.modular.tile.PhosphophylliteTile;
import net.roguelogix.phosphophyllite.registry.RegisterTile;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ItemWhiteHoleTile extends PhosphophylliteTile implements IItemHandler {
    
    @RegisterTile("item_white_hole")
    public static final BlockEntityType.BlockEntitySupplier<ItemWhiteHoleTile> SUPPLIER = new RegisterTile.Producer<>(ItemWhiteHoleTile::new);
    
    public ItemWhiteHoleTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
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
            compound.putString("item", Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(item)).toString());
        }
        return compound;
    }
    
    @Override
    public void readNBT(@Nonnull CompoundTag compound) {
        super.readNBT(compound);
        if (compound.contains("item")) {
            item = BuiltInRegistries.ITEM.get(new ResourceLocation(compound.getString("item")));
        }
    }
    
    public void tick() {
        if (item != null) {
            assert level != null;
            for (Direction direction : Direction.values()) {
                var cap = level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(direction), direction.getOpposite());
                if (cap == null) {
                    continue;
                }
                for (int i = 0; i < cap.getSlots(); i++) {
                    //noinspection deprecation
                    cap.insertItem(i, new ItemStack(item, item.getMaxStackSize()), false);
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
