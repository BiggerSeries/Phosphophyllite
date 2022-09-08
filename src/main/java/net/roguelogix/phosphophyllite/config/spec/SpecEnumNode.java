package net.roguelogix.phosphophyllite.config.spec;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.roguelogix.phosphophyllite.config.ConfigValue;
import net.roguelogix.phosphophyllite.parsers.Element;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@NonnullDefault
public class SpecEnumNode extends SpecValueNode {
    public final Class<?> enumClass;
    public final Enum<?> defaultValue;
    public final List<Enum<?>> allowedValues;
    
    SpecEnumNode(SpecObjectNode parent, Field field, ConfigOptionsDefaults defaults) {
        super(parent, field, defaults);
        enumClass = field.getType();
        defaultValue = currentValue();
        final var annotation = field.getAnnotation(ConfigValue.class);
        final var allowedValues = new ObjectArrayList<Enum<?>>();
        for (final var value : annotation.allowedValues()) {
            for (final var enumVal : enumClass.getEnumConstants()) {
                if (enumVal.toString().equalsIgnoreCase(value)) {
                    allowedValues.add((Enum<?>) enumVal);
                }
            }
        }
        if (allowedValues.isEmpty()) {
            for (final var value : enumClass.getEnumConstants()) {
                allowedValues.add((Enum<?>) value);
            }
        }
        this.allowedValues = Collections.unmodifiableList(allowedValues);
    }
    
    @Override
    public String defaultValueAsString() {
        return defaultValue.toString();
    }
    
    public List<String> allowedValuesAsStrings() {
        return allowedValues.stream().map(Enum::toString).toList();
    }
    
    public Enum<?> currentValue() {
        return (Enum<?>) currentValueObject();
    }
    
    @Override
    public String currentValueAsString() {
        return String.valueOf(currentValueObject());
    }
    
    @Override
    public void writeFromString(String string) {
        var enumToWrite = currentValue();
        for (final var value : allowedValues) {
            if (value.toString().equalsIgnoreCase(string)) {
                enumToWrite = value;
                break;
            }
        }
        writeObject(enumToWrite);
    }
    
    @Override
    public boolean isValueValid(String valueString) {
        for (final var value : allowedValues) {
            if (value.toString().equalsIgnoreCase(valueString)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void writeDefault() {
        writeObject(defaultValue);
    }
    
    @Override
    public Element generateDefaultElement() {
        return new Element(Element.Type.String, generateComment(), name, defaultValue.toString());
    }
    
    @Override
    public Element generateCurrentElement() {
        return new Element(Element.Type.String, generateComment(), name, currentValueObject().toString());
    }
    
    @Override
    public Element generateSyncElement() {
        return new Element(Element.Type.String, null, name, currentValueObject().toString());
    }
    
    @Override
    public String generateComment() {
        final StringBuilder comment = new StringBuilder(baseComment);
        
        if (comment.length() != 0) {
            comment.append('\n');
        }
        comment.append("Default: ");
        comment.append(defaultValue);
        
        comment.append("\nAllowed Values: ");
        for (final var value : allowedValuesAsStrings()) {
            comment.append(value);
            comment.append(", ");
        }
        
        return comment.toString();
    }
    
    @Override
    public Element correctToValidState(Element element) {
        if (element.type != Element.Type.String || !(element.value instanceof String)) {
            return generateDefaultElement();
        }
        if (!isValueValid(element.asString())) {
            return generateDefaultElement();
        }
        return new Element(Element.Type.String, Objects.requireNonNull(generateDefaultElement()).comment, name, element.value);
    }
    
    @Override
    public void writeElement(Element element) {
        writeFromString(element.asString());
    }
}
