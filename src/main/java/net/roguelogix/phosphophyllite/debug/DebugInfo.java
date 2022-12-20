package net.roguelogix.phosphophyllite.debug;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

import javax.annotation.Nullable;

@NonnullDefault
public class DebugInfo {
    
    private final String name;
    private final ObjectArrayList<Object> values = new ObjectArrayList<>();
    
    public DebugInfo(String name) {
        this.name = name;
    }
    
    public DebugInfo(String name, DebugInfo toCopy) {
        this.name = name;
        values.addAll(toCopy.values);
    }
    
    public String name() {
        return name;
    }
    
    public DebugInfo add(@Nullable DebugInfo subInfo) {
        if (subInfo == null) {
            return this;
        }
        values.add(subInfo);
        return this;
    }
    
    public DebugInfo add(@Nullable String str) {
        if (str == null) {
            return this;
        }
        values.add(str);
        return this;
    }
    
    private ObjectArrayList<String> valuesAsStrings() {
        final var strings = new ObjectArrayList<String>(values.size());
        for (final var value : values) {
            strings.add(value.toString());
        }
        return strings;
    }
    
    public String toString() {
        final var valueStrings = valuesAsStrings();
        
        final var builder = new StringBuilder();
        
        builder.append(name).append(":\n");
        for (int j = 0; j < valueStrings.size(); j++) {
            final var value = valueStrings.get(j);
            final var hasNext = j != valueStrings.size() - 1;
            final var valueLines = value.split("\n");
            for (int i = 0; i < valueLines.length; i++) {
                if (i == 0) {
                    builder.append(hasNext ? "├─" : "└─");
                } else {
                    builder.append(hasNext ? "│ " : "  ");
                }
                builder.append(valueLines[i]);
                builder.append('\n');
            }
        }
        
        return builder.toString();
    }
}
