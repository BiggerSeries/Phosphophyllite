package net.roguelogix.phosphophyllite.quartz.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class QuartzState {
    public String blockName;
    // yes yes, you *can* poke at it, *dont*
    public final Map<String, String> values = new HashMap<>();
    
    // oh yes, you can use *anything* you want
    // values are matched in file order
    // if one is not found, matching is *stopped* and the last found one is used
    public <T> void with(T value) {
        values.put(value.getClass().getName().toLowerCase(), value.toString().toLowerCase());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuartzState that = (QuartzState) o;
        return Objects.equals(values, that.values);
    }
    
    @Override
    public int hashCode() {
        return values.hashCode();
    }
}

