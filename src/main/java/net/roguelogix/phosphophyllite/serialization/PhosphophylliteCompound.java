package net.roguelogix.phosphophyllite.serialization;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.roguelogix.phosphophyllite.robn.ROBNObject;
import net.roguelogix.phosphophyllite.util.MethodsReturnNonnullByDefault;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PhosphophylliteCompound implements ROBNObject {
    
    private final Map<String, Object> data = new Object2ObjectOpenHashMap<>();
    
    public PhosphophylliteCompound() {
    }
    
    public PhosphophylliteCompound(byte[] ROBNbuffer) {
        this(ByteArrayList.wrap(ROBNbuffer));
    }
    
    public PhosphophylliteCompound(List<Byte> ROBNbuffer) {
        fromROBN(ROBNbuffer);
    }
    
    public void put(String key, @Nullable PhosphophylliteCompound compound) {
        if (compound == null) {
            return;
        }
        data.put(key, compound);
    }
    
    public void put(String key, @Nullable String value) {
        if (value == null) {
            return;
        }
        data.put(key, value);
    }
    
    public void put(String key, boolean value) {
        data.put(key, value);
    }
    
    public void put(String key, byte value) {
        data.put(key, value);
    }
    
    public void put(String key, short value) {
        data.put(key, value);
    }
    
    public void put(String key, int value) {
        data.put(key, value);
    }
    
    public void put(String key, long value) {
        data.put(key, value);
    }
    
    public void put(String key, float value) {
        data.put(key, value);
    }
    
    public void put(String key, double value) {
        data.put(key, value);
    }
    
    public void put(String key, List<?> value) {
        data.put(key, value);
    }
    
    public Object get(String key) {
        return data.get(key);
    }
    
    public PhosphophylliteCompound getCompound(String key) {
        var val = data.get(key);
        if (!(val instanceof PhosphophylliteCompound)) {
            if(val instanceof Map){
                var compound = new PhosphophylliteCompound();
                //noinspection unchecked
                compound.fromROBNMap((Map<String, Object>) val);
                data.put(key, compound);
                return compound;
            }
            return new PhosphophylliteCompound();
        }
        return (PhosphophylliteCompound) val;
    }
    
    public String getString(String key) {
        var val = data.get(key);
        if (!(val instanceof String)) {
            return "";
        }
        return (String) val;
    }
    
    public byte getByte(String key) {
        var val = data.get(key);
        if (!(val instanceof Number)) {
            return 0;
        }
        return ((Number) val).byteValue();
    }
    
    public short getShort(String key) {
        var val = data.get(key);
        if (!(val instanceof Number)) {
            return 0;
        }
        return ((Number) val).shortValue();
    }
    
    public int getInt(String key) {
        var val = data.get(key);
        if (!(val instanceof Number)) {
            return 0;
        }
        return ((Number) val).intValue();
    }
    
    public long getLong(String key) {
        var val = data.get(key);
        if (!(val instanceof Number)) {
            return 0;
        }
        return ((Number) val).longValue();
    }
    
    public float getFloat(String key) {
        var val = data.get(key);
        if (!(val instanceof Number)) {
            return 0;
        }
        return ((Number) val).floatValue();
    }
    
    public double getDouble(String key) {
        var val = data.get(key);
        if (!(val instanceof Number)) {
            return 0;
        }
        return ((Number) val).doubleValue();
    }
    
    public List<Object> getList(String key) {
        //noinspection unchecked
        return (List<Object>) data.get(key);
    }
    
    @Override
    public Map<String, Object> toROBNMap() {
        return data;
    }
    
    @Override
    public void fromROBNMap(Map<String, Object> map) {
        data.clear();
        data.putAll(map);
    }
}

