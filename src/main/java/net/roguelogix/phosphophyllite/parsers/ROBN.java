package net.roguelogix.phosphophyllite.parsers;

import net.roguelogix.phosphophyllite.util.MethodsReturnNonnullByDefault;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ROBN {
    
    @Nullable
    public static Element parseROBN(ArrayList<Byte> robn) {
        try {
            var robnObject = net.roguelogix.phosphophyllite.robn.ROBN.fromROBN(robn);
            if (robnObject instanceof Map<?, ?> map) {
                //noinspection unchecked
                return parseROBNMap((Map<String, Object>) map);
            }
            return null;
        } catch (IllegalArgumentException | ClassCastException ignored) {
            return null;
        }
    }
    
    private static Element parseROBNMap(Map<String, Object> map) {
        var type = Element.Type.valueOf((String) map.get("type"));
        var name = (String) map.get("name");
        Object val;
        switch (type) {
            case String, Number, Boolean -> {
                val = map.get("value");
                if (!(val instanceof String)) {
                    throw new IllegalArgumentException();
                }
            }
            case Array, Section -> {
                int length = (int) map.get("length");
                var array = new Element[length];
                for (int i = 0; i < length; i++) {
                    //noinspection unchecked
                    array[i] = parseROBNMap((Map<String, Object>) map.get(Integer.toString(i)));
                }
                val = array;
            }
            default -> throw new IllegalStateException("Unexpected value: " + type);
        }
        return new Element(type, null, name, val);
    }
    
    @Nullable
    public static ArrayList<Byte> parseElement(Element element) {
        try {
            return net.roguelogix.phosphophyllite.robn.ROBN.toROBN(parseElementInternal(element));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
    
    private static HashMap<String, Object> parseElementInternal(Element element) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("type", element.type.toString());
        if (element.name != null) {
            map.put("name", element.name);
        }
        // comment is skipped, ROBN doesnt need it
        switch (element.type) {
            case String, Number, Boolean -> {
                map.put("value", element.asString());
            }
            case Array, Section -> {
                var array = element.asArray();
                map.put("length", array.length);
                for (int i = 0; i < array.length; i++) {
                    map.put(Integer.toString(i), parseElementInternal(array[i]));
                }
            }
        }
        return map;
    }
}
