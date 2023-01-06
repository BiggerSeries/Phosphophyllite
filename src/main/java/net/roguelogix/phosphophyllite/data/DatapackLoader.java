package net.roguelogix.phosphophyllite.data;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.config.ConfigType;
import net.roguelogix.phosphophyllite.config.spec.ConfigOptionsDefaults;
import net.roguelogix.phosphophyllite.config.spec.SpecObjectNode;
import net.roguelogix.phosphophyllite.parsers.JSON5;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@NonnullDefault
public class DatapackLoader<T> {
    
    private final SpecObjectNode baseSpecNode;
    private final Supplier<T> objectSupplier;
    
    public DatapackLoader(Supplier<T> objectSupplier) {
        final var templateObject = objectSupplier.get();
        baseSpecNode = new SpecObjectNode(templateObject, "", ConfigType.NULL, new ConfigOptionsDefaults(ConfigType.NULL, false, false, false));
        this.objectSupplier = objectSupplier;
    }
    
    public Map<ResourceLocation, List<T>> loadAllMappedStack(ResourceLocation baseResourceLocation) {
        if (Phosphophyllite.serverResourceManager == null) {
            return new Object2ObjectOpenHashMap<>();
        }
        
        final var map = new Object2ObjectOpenHashMap<ResourceLocation, List<T>>();
        
        final var resourceLocations = Phosphophyllite.serverResourceManager.listResourceStacks(baseResourceLocation.getPath(), s -> s.getPath().contains(".json"));
        
        for (final var entry : resourceLocations.entrySet()) {
            final var resourceLocation = entry.getKey();
            // TODO: 9/8/22 add an option for enforcing this
            if (!resourceLocation.getNamespace().equals(baseResourceLocation.getNamespace())) {
                continue;
            }
            final var list = new ObjectArrayList<T>();
            for (Resource resource : entry.getValue()) {
                T t = load(resourceLocation, resource);
                if (t != null) {
                    list.add(t);
                }
            }
            if (list.isEmpty()) {
                continue;
            }
            map.put(resourceLocation, list);
        }
        
        return map;
    }
    
    public List<T> loadAll(ResourceLocation baseResourceLocation) {
        if (Phosphophyllite.serverResourceManager == null) {
            return new ObjectArrayList<>();
        }
        
        final var list = new ObjectArrayList<T>();
        
        Map<ResourceLocation, Resource> resourceLocations = Phosphophyllite.serverResourceManager.listResources(baseResourceLocation.getPath(), s -> s.getPath().contains(".json"));
        
        for (Map.Entry<ResourceLocation, Resource> entry : resourceLocations.entrySet()) {
            final var resourceLocation = entry.getKey();
            // TODO: 9/8/22 add an option for enforcing this
            if (!resourceLocation.getNamespace().equals(baseResourceLocation.getNamespace())) {
                continue;
            }
            T t = load(resourceLocation, entry.getValue());
            if (t != null) {
                list.add(t);
            }
        }
        
        return list;
    }
    
    @Nullable
    public T load(ResourceLocation location) {
        if (Phosphophyllite.serverResourceManager == null) {
            return null;
        }
        return load(location, Phosphophyllite.serverResourceManager.getResource(location).orElseThrow());
    }
    
    @Nullable
    private T load(ResourceLocation location, Resource resource) {
        String json;
        try {
            try (BufferedReader reader = resource.openAsReader()) {
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                    builder.append("\n");
                }
                json = builder.toString();
            }
            
        } catch (IOException e) {
            Phosphophyllite.LOGGER.error("Error reading json at " + location.toString());
            e.printStackTrace();
            return null;
        }
        
        final var object = objectSupplier.get();
        
        final var elements = JSON5.parseString(json);
        
        if (!baseSpecNode.setActiveObject(object)) {
            return null;
        }
        
        if (elements == null) {
            return object;
        }
        
        final var correctedElements = baseSpecNode.correctToValidState(elements);
        
        if (correctedElements == null) {
            return object;
        }
        
        baseSpecNode.writeElement(correctedElements);
        
        return object;
    }
}
