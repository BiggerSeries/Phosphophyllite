package net.roguelogix.phosphophyllite.util;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Collections;
import java.util.List;

@NonnullDefault

public class FastArraySet<T> {
    private final Object2IntOpenHashMap<T> indexMap = new Object2IntOpenHashMap<>();
    private final ObjectArrayList<T> elementList = new ObjectArrayList<>();
    private final List<T> unmodifiableList = Collections.unmodifiableList(elementList);
    
    public void add(T element) {
        if (indexMap.containsKey(element)) {
            return;
        }
        indexMap.put(element, elementList.size());
        elementList.add(element);
    }
    
    public void remove(T element) {
        if (!indexMap.containsKey(element)) {
            return;
        }
        int index = indexMap.removeInt(element);
        final var popped = elementList.pop();
        if (index == elementList.size()) {
            return;
        }
        // the element we popped off wasn't the one that is getting removed
        assert elementList.get(index) == element;
        elementList.set(index, popped);
        indexMap.put(popped, index);
    }
    
    public boolean contains(T element) {
        return indexMap.containsKey(element);
    }
    
    public List<T> elements() {
        return unmodifiableList;
    }
    
    public int size() {
        return elementList.size();
    }
    
    public T get(int i) {
        return elementList.get(i);
    }
}
