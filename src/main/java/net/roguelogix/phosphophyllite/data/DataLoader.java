package net.roguelogix.phosphophyllite.data;

import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.repack.tnjson.ParseException;
import net.roguelogix.phosphophyllite.repack.tnjson.TnJson;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class DataLoader<T> {
    
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Range {
        String value();
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Values {
        String[] value();
    }
    
    private enum ElementType {
        String,
        ResourceLocation,
        LONG,
        Double
    }
    
    private static class DataElement {
        Field field;
        ElementType type;
    }
    
    private static class NumberDataElement extends DataElement {
        double lowerBound = Double.MIN_VALUE;
        boolean lowerInclusive = true;
        double upperBound = Double.MAX_VALUE;
        boolean upperInclusive = true;
    }
    
    private static class StringDataElement extends DataElement {
        String[] allowedValues;
    }
    
    private final Map<String, DataElement> dataMap = new HashMap<>();
    private final Constructor<T> constructor;
    
    
    public DataLoader(@Nonnull Class<T> clazz) {
        try {
            constructor = clazz.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("no default constructor");
        }
        constructor.setAccessible(true);
        for (Field declaredField : clazz.getDeclaredFields()) {
            declaredField.setAccessible(true);
            String name = declaredField.getName();
            DataElement element;
            
            Class<?> fieldType = declaredField.getType();
            if (fieldType == long.class || fieldType == double.class) {
                NumberDataElement numberElement = new NumberDataElement();
                
                if (declaredField.isAnnotationPresent(Range.class)) {
                    Range rangeAnnotation = declaredField.getAnnotation(Range.class);
                    String range = rangeAnnotation.value();
                    
                    range = range.trim();
                    if (range.length() < 3) {
                        throw new IllegalArgumentException("Incomplete range given");
                    }
                    
                    char lowerInclusiveChar = range.charAt(0);
                    char higherInclusiveChar = range.charAt(range.length() - 1);
                    boolean lowerInclusive;
                    boolean higherInclusive;
                    
                    switch (lowerInclusiveChar) {
                        case '(': {
                            lowerInclusive = false;
                            break;
                        }
                        case '[': {
                            lowerInclusive = true;
                            break;
                        }
                        default: {
                            throw new IllegalArgumentException("Unknown lower bound inclusivity");
                        }
                    }
                    switch (higherInclusiveChar) {
                        case ')': {
                            higherInclusive = false;
                            break;
                        }
                        case ']': {
                            higherInclusive = true;
                            break;
                        }
                        default: {
                            throw new IllegalArgumentException("Unknown higher bound inclusivity");
                        }
                    }
                    
                    range = range.substring(1, range.length() - 1).trim();
                    String[] bounds = range.split(",");
                    if (bounds.length > 2) {
                        throw new IllegalArgumentException("Range cannot have more than two bounds");
                    }
                    String lowerBoundStr = "";
                    String higherBoundStr = "";
                    if (bounds.length == 2) {
                        lowerBoundStr = bounds[0].trim();
                        higherBoundStr = bounds[1].trim();
                    } else {
                        if (range.length() == 0) {
                            throw new IllegalArgumentException("Incomplete range given");
                        }
                        if (range.length() != 1) {
                            if (bounds.length != 1) {
                                throw new IllegalArgumentException("Incomplete range given");
                            }
                            if (range.charAt(0) == ',') {
                                higherBoundStr = bounds[0];
                            } else if (range.charAt(range.length() - 1) == ',') {
                                lowerBoundStr = bounds[0];
                            } else {
                                throw new IllegalArgumentException("Incomplete range given");
                            }
                        } else if (range.charAt(0) != ',') {
                            throw new IllegalArgumentException("Incomplete range given");
                        }
                    }
                    double lowerBound = Double.MIN_VALUE;
                    if (lowerBoundStr.length() != 0) {
                        lowerBound = Double.parseDouble(lowerBoundStr);
                    }
                    double higherBound = Double.MAX_VALUE;
                    if (higherBoundStr.length() != 0) {
                        higherBound = Double.parseDouble(higherBoundStr);
                    }
                    if (lowerBound > higherBound) {
                        throw new IllegalArgumentException("Higher bound must be greater or equal to lower bound");
                    }
                    
                    
                    numberElement.lowerInclusive = lowerInclusive;
                    numberElement.upperInclusive = higherInclusive;
                    numberElement.lowerBound = lowerBound;
                    numberElement.upperBound = higherBound;
                    
                }
                
                if (fieldType == long.class) {
                    numberElement.type = ElementType.LONG;
                } else {
                    numberElement.type = ElementType.Double;
                }
                
                element = numberElement;
                
            } else if (fieldType == String.class || fieldType == ResourceLocation.class) {
                StringDataElement stringElement = new StringDataElement();
                
                if (declaredField.isAnnotationPresent(Values.class)) {
                    stringElement.allowedValues = declaredField.getAnnotation(Values.class).value();
                }
                
                if (fieldType == ResourceLocation.class) {
                    stringElement.type = ElementType.ResourceLocation;
                } else {
                    stringElement.type = ElementType.String;
                }
                
                element = stringElement;
            } else {
                throw new IllegalArgumentException("Invalid type used");
            }
            
            element.field = declaredField;
            dataMap.put(name, element);
        }
    }
    
    @Nonnull
    public List<T> loadAll(ResourceLocation location) {
        if (Phosphophyllite.dataPackRegistries == null) {
            return new ArrayList<>();
        }
        
        ArrayList<T> elements = new ArrayList<>();
        
        ResourceManager resourceManager = Phosphophyllite.dataPackRegistries.getResourceManager();
        Collection<ResourceLocation> resourceLocations = resourceManager.listResources(location.getPath(), s -> s.contains(".json"));
        
        for (ResourceLocation resourceLocation : resourceLocations) {
            if (!resourceLocation.getNamespace().equals(location.getNamespace())) {
                continue;
            }
            T t = load(resourceLocation, resourceManager);
            if (t != null) {
                elements.add(t);
            }
        }
        
        return elements;
    }
    
    @Nullable
    public T load(ResourceLocation location) {
        if (Phosphophyllite.dataPackRegistries == null) {
            return null;
        }
        return load(location, Phosphophyllite.dataPackRegistries.getResourceManager());
    }
    
    @Nullable
    private T load(ResourceLocation location, ResourceManager resourceManager) {
        String json;
        try {
            Resource resource = resourceManager.getResource(location);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
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
        
        Map<String, Object> map;
        try {
            map = TnJson.parse(json);
        } catch (ParseException e) {
            Phosphophyllite.LOGGER.error("Error parsing json at " + location.toString());
            e.printStackTrace();
            return null;
        }
        
        T newT;
        try {
            newT = constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            Phosphophyllite.LOGGER.error("Failed to create object instance");
            e.printStackTrace();
            return null;
        }
        for (Map.Entry<String, DataElement> dataElementEntry : dataMap.entrySet()) {
            String name = dataElementEntry.getKey();
            DataElement dataElement = dataElementEntry.getValue();
            Object object = map.get(name);
            if (object == null) {
                Phosphophyllite.LOGGER.error("Data member \"" + name + "\" not found in json at " + location.toString());
                return null;
            }
            if (dataElement.type == ElementType.LONG || dataElement.type == ElementType.Double) {
                NumberDataElement numberDataElement = (NumberDataElement) dataElement;
                
                if (!(object instanceof Number)) {
                    Phosphophyllite.LOGGER.error("Data member \"" + name + "\" required to be a number given as " + object.getClass().getSimpleName() + " in json at " + location.toString());
                    return null;
                }
                double val = ((Number) object).doubleValue();
                if (dataElement.type == ElementType.LONG) {
                    long realVal = Math.round(val);
                    if (realVal < numberDataElement.lowerBound || realVal > numberDataElement.upperBound ||
                            (realVal <= numberDataElement.lowerBound && !(numberDataElement.lowerInclusive)) ||
                            (realVal >= numberDataElement.upperBound && !numberDataElement.upperInclusive)) {
                        Phosphophyllite.LOGGER.error("Data member \"" + name + "\" given out of range value " + val + " in json at " + location.toString() + ". Valid range is " +
                                ((numberDataElement.lowerInclusive ? "[" : "(" + ((numberDataElement.lowerBound == Double.MIN_VALUE) ? "" : numberDataElement.lowerBound))) +
                                "," +
                                (((numberDataElement.upperBound == Double.MAX_VALUE) ? "" : numberDataElement.upperBound) + (numberDataElement.upperInclusive ? "]" : ")")) +
                                ". Clamping to range");
                        if (realVal <= numberDataElement.lowerBound) {
                            realVal = Math.round(numberDataElement.lowerBound);
                            if (!numberDataElement.lowerInclusive) {
                                realVal++;
                            }
                        } else if (realVal >= numberDataElement.upperBound) {
                            realVal = Math.round(numberDataElement.upperBound);
                            if (!numberDataElement.upperInclusive) {
                                realVal--;
                            }
                        }
                    }
                    try {
                        dataElement.field.setLong(newT, realVal);
                    } catch (IllegalAccessException e) {
                        Phosphophyllite.LOGGER.error("Illegal access while setting field");
                        e.printStackTrace();
                        return null;
                    }
                } else {
                    if (val < numberDataElement.lowerBound || val > numberDataElement.upperBound ||
                            (val <= numberDataElement.lowerBound && !(numberDataElement.lowerInclusive)) ||
                            (val >= numberDataElement.upperBound && !numberDataElement.upperInclusive)) {
                        Phosphophyllite.LOGGER.error("Data member \"" + name + "\" given out of range value " + val + " in json at " + location.toString() + ". Valid range is " +
                                ((numberDataElement.lowerInclusive ? "[" : "(" + ((numberDataElement.lowerBound == Double.MIN_VALUE) ? "" : numberDataElement.lowerBound))) +
                                "," +
                                (((numberDataElement.upperBound == Double.MAX_VALUE) ? "" : numberDataElement.upperBound) + (numberDataElement.upperInclusive ? "]" : ")")) +
                                ". Clamping to range");
                        if (val <= numberDataElement.lowerBound) {
                            val = numberDataElement.lowerBound;
                            if (!numberDataElement.lowerInclusive) {
                                val = Math.nextAfter(val, Double.POSITIVE_INFINITY);
                            }
                        } else if (val >= numberDataElement.upperBound) {
                            val = numberDataElement.upperBound;
                            if (!numberDataElement.upperInclusive) {
                                val = Math.nextAfter(val, Double.NEGATIVE_INFINITY);
                            }
                        }
                    }
                    try {
                        dataElement.field.setDouble(newT, val);
                    } catch (IllegalAccessException e) {
                        Phosphophyllite.LOGGER.error("Illegal access while setting field");
                        e.printStackTrace();
                        return null;
                    }
                }
                
            } else if (dataElement.type == ElementType.String) {
                StringDataElement stringDataElement = (StringDataElement) dataElement;
                
                if (!(object instanceof String)) {
                    Phosphophyllite.LOGGER.error("Data member \"" + name + "\" required to be a string given as " + object.getClass().getSimpleName() + " in json at " + location.toString());
                    return null;
                }
                
                String data = (String) object;
                
                try {
                    stringDataElement.field.set(newT, data);
                } catch (IllegalAccessException e) {
                    Phosphophyllite.LOGGER.error("Illegal access while setting field");
                    e.printStackTrace();
                    return null;
                }
                
                if (stringDataElement.allowedValues != null) {
                    boolean allowed = false;
                    for (String allowedValue : stringDataElement.allowedValues) {
                        if (data.equals(allowedValue)) {
                            allowed = true;
                            break;
                        }
                    }
                    if (!allowed) {
                        Phosphophyllite.LOGGER.error("Invalid value for member \"" + name + "\" given in json at " + location.toString());
                        return null;
                    }
                }
            } else if (dataElement.type == ElementType.ResourceLocation) {
                StringDataElement stringDataElement = (StringDataElement) dataElement;
                
                if (!(object instanceof String)) {
                    Phosphophyllite.LOGGER.error("Data member \"" + name + "\" required to be a resource location string given as " + object.getClass().getSimpleName() + " in json at " + location.toString());
                    return null;
                }
                
                String data = (String) object;
                
                try {
                    stringDataElement.field.set(newT, new ResourceLocation(data));
                } catch (ResourceLocationException e) {
                    Phosphophyllite.LOGGER.error("Invalid resource location given for \"" + name + "\" in json at " + location.toString());
                    Phosphophyllite.LOGGER.error(e.getMessage());
                    return null;
                } catch (IllegalAccessException e) {
                    Phosphophyllite.LOGGER.error("Illegal access while setting field");
                    e.printStackTrace();
                    return null;
                }
            } else {
                Phosphophyllite.LOGGER.error("Unknown data element type");
                return null;
            }
        }
        
        return newT;
    }
}
