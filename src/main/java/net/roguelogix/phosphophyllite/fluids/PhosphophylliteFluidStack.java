package net.roguelogix.phosphophyllite.fluids;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Objects;

/**
 * Re-writable long capable version of a FluidStack
 */
@NonnullDefault
public class PhosphophylliteFluidStack extends FluidStack {
    
    // forge, you dont need to be a PITA about this
    private static final Field delegateField;
    
    static {
        try {
            delegateField = FluidStack.class.getDeclaredField("fluidDelegate");
            delegateField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }
    
    private void setDelegate(Holder.Reference<Fluid> delegate) {
        try {
            delegateField.set(this, delegate);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }
    
    public PhosphophylliteFluidStack() {
        super(Fluids.WATER, 0);
        setDelegate(delegateWrapper);
        setFluid(Fluids.EMPTY);
    }
    
    public PhosphophylliteFluidStack(Fluid fluid, int amount) {
        super(Fluids.WATER, 0);
        setDelegate(delegateWrapper);
        setFluid(fluid);
        setAmount(amount);
    }
    
    public PhosphophylliteFluidStack(Fluid fluid, int amount, @Nullable CompoundTag nbt) {
        super(Fluids.WATER, 0, nbt);
        setDelegate(delegateWrapper);
        setFluid(fluid);
        setAmount(amount);
    }
    
    public PhosphophylliteFluidStack(Fluid fluid, long amount, @Nullable CompoundTag nbt) {
        super(Fluids.WATER, 0, nbt);
        setDelegate(delegateWrapper);
        setFluid(fluid);
        setAmount(amount);
    }
    
    public PhosphophylliteFluidStack(FluidStack stack, int amount) {
        this(stack, (long) amount);
    }
    
    public PhosphophylliteFluidStack(FluidStack stack, long amount) {
        super(Fluids.WATER, 0, stack.getTag());
        setDelegate(delegateWrapper);
        setFluid(stack.getRawFluid());
        setAmount(amount);
    }
    
    public PhosphophylliteFluidStack(PhosphophylliteFluidStack stack) {
        this(stack, stack.getLongAmount());
    }
    
    public PhosphophylliteFluidStack(FluidStack stack) {
        this(stack, stack instanceof PhosphophylliteFluidStack ? ((PhosphophylliteFluidStack) stack).getLongAmount() : stack.getAmount());
    }
    
    @Nullable
    Fluid fluid;
    
    Holder.Reference<Fluid> delegateWrapper = new Holder.Reference<Fluid>(Holder.Reference.Type.STAND_ALONE, BuiltInRegistries.FLUID.holderOwner(), null, null) {
        @Override
        public void bindKey(ResourceKey<Fluid> key) {
            this.key = key;
        }
        
        @Override
        public void bindValue(Fluid fluid) {
            this.value = fluid;
        }
    };
    
    private long amount = 0;
    
    public void setFluid(Fluid fluid) {
        this.fluid = fluid;
        delegateWrapper.bindKey(ResourceKey.create(Registries.FLUID, Objects.requireNonNull(BuiltInRegistries.FLUID.getKey(fluid))));
        delegateWrapper.bindValue(fluid);
        updateEmpty();
    }
    
    public static FluidStack loadFromNBT(@Nullable CompoundTag nbt) {
        if (nbt == null) {
            return EMPTY;
        }
        if (!nbt.contains("FluidName", Tag.TAG_STRING)) {
            return EMPTY;
        }
        
        ResourceLocation fluidName = new ResourceLocation(nbt.getString("FluidName"));
        Fluid fluid = BuiltInRegistries.FLUID.get(fluidName);
        if (fluid == null) {
            return EMPTY;
        }
        PhosphophylliteFluidStack stack = new PhosphophylliteFluidStack(fluid, nbt.getInt("Amount"));
        
        if (nbt.contains("LongAmount")) {
            stack.amount = nbt.getLong("LongAmount");
        }
        
        if (nbt.contains("Tag", Tag.TAG_STRING)) {
            stack.setTag(nbt.getCompound("Tag"));
        }
        return stack;
    }
    
    public CompoundTag writeToNBT(CompoundTag nbt) {
        nbt.putString("FluidName", BuiltInRegistries.FLUID.getKey(getFluid()).toString());
        nbt.putInt("Amount", (int) Math.min(amount, Integer.MAX_VALUE));
        nbt.putLong("LongAmount", amount);
        
        if (getTag() != null) {
            nbt.put("Tag", getTag());
        }
        return nbt;
    }
    
    public void writeToPacket(FriendlyByteBuf buf) {
        buf.writeResourceLocation(BuiltInRegistries.FLUID.getKey(getFluid()));
        buf.writeVarInt(getAmount());
        buf.writeNbt(getTag());
    }
    
    public void writeToLongPacket(FriendlyByteBuf buf) {
        buf.writeResourceLocation(BuiltInRegistries.FLUID.getKey(getFluid()));
        buf.writeVarLong(getAmount());
        buf.writeNbt(getTag());
    }
    
    public static FluidStack readFromLongPacket(FriendlyByteBuf buf) {
        var fluid = BuiltInRegistries.FLUID.get(buf.readResourceLocation());
        long amount = buf.readVarLong();
        CompoundTag tag = buf.readNbt();
        if (fluid == Fluids.EMPTY) {
            return EMPTY;
        }
        return new PhosphophylliteFluidStack(fluid, amount, tag);
    }
    
    public boolean isEmpty() {
        return amount <= 0 || fluid == Fluids.EMPTY;
    }
    
    public int getAmount() {
        return (int) Math.min(amount, Integer.MAX_VALUE);
    }
    
    public long getLongAmount() {
        return amount;
    }
    
    public void setAmount(int amount) {
        setAmount((long) amount);
    }
    
    public void setAmount(long amount) {
        this.amount = amount;
        super.setAmount(getAmount());
    }
    
    @Nullable
    private CompoundTag tag;
    
    public boolean hasTag() {
        return tag != null;
    }
    
    @Nullable
    public CompoundTag getTag() {
        return tag;
    }
    
    public void setTag(CompoundTag tag) {
        this.tag = tag;
    }
    
    public CompoundTag getOrCreateTag() {
        if (tag == null) {
            setTag(new CompoundTag());
        }
        return tag;
    }
    
    @Nullable
    public CompoundTag getChildTag(String childName) {
        if (tag == null) {
            return null;
        }
        return tag.getCompound(childName);
    }
    
    
    public CompoundTag getOrCreateChildTag(String childName) {
        getOrCreateTag();
        assert tag != null;
        CompoundTag child = tag.getCompound(childName);
        if (!tag.contains(childName, Tag.TAG_STRING)) {
            tag.put(childName, child);
        }
        return child;
    }
    
    public void removeChildTag(String childName) {
        if (tag != null) {
            tag.remove(childName);
        }
    }
    
    public void grow(int amount) {
        setAmount(this.amount + amount);
    }
    
    public void grow(long amount) {
        setAmount(this.amount + amount);
    }
    
    public void shrink(int amount) {
        setAmount(this.amount - amount);
    }
    
    public void shrink(long amount) {
        setAmount(this.amount - amount);
    }
    
    public boolean isFluidStackIdentical(FluidStack other) {
        if (other instanceof PhosphophylliteFluidStack) {
            return isFluidEqual(other) && amount == ((PhosphophylliteFluidStack) other).getLongAmount();
        }
        return isFluidEqual(other) && amount == other.getAmount();
    }
    
    public boolean containsFluid(@Nonnull FluidStack other) {
        if (other instanceof PhosphophylliteFluidStack) {
            return isFluidEqual(other) && amount >= ((PhosphophylliteFluidStack) other).getLongAmount();
        }
        return isFluidEqual(other) && amount >= other.getAmount();
    }
    
    public FluidStack copy() {
        return new PhosphophylliteFluidStack(this);
    }
    
}
