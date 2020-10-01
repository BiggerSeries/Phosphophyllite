package net.roguelogix.phosphophyllite.quartz.api;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.HashMap;
import java.util.Objects;

public class QuartzState implements INBTSerializable<CompoundNBT> {
    public String blockName;
    // yes yes, you *can* poke at it, *dont*
    public final HashMap<String, String> values = new HashMap<>();
    
    public QuartzState(String blockName) {
        this.blockName = blockName;
    }
    
    // oh yes, you can use *anything* you want
    // values are matched in file order
    // if one is not found, matching is *stopped* and the last found one is used
    public <T> void with(T value) {
        with(value.getClass().getName().toLowerCase(), value.toString().toLowerCase());
    }
    
    public void with(String name, String value) {
        values.put(name.toLowerCase(), value.toLowerCase());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QuartzState that = (QuartzState) o;
        if (!this.blockName.equals(that.blockName)) {
            return false;
        }
        return Objects.equals(values, that.values);
    }
    
    @Override
    public int hashCode() {
        return values.hashCode();
    }
    
    public QuartzState copy() {
        QuartzState newState = new QuartzState(this.blockName);
        newState.values.putAll(values);
        return newState;
    }
    
    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putString("blockName", blockName);
        CompoundNBT valueMap = new CompoundNBT();
        values.forEach((k, v) -> {
            valueMap.putString(k, v);
        });
        nbt.put("values", valueMap);
        return nbt;
    }
    
    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        if (nbt.contains("blockName")) {
            blockName = nbt.getString("blockName");
        }
        if (nbt.contains("values")) {
            CompoundNBT valueMap = nbt.getCompound("values");
            valueMap.getTagMap().keySet().forEach(k -> {
                String value = valueMap.getString(k);
                if(!value.equals("")){
                    values.put(k, value);
                }
            });
        }
    }
}

