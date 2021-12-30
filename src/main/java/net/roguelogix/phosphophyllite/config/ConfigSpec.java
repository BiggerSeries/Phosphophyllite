package net.roguelogix.phosphophyllite.config;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.parsers.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.*;
import java.util.*;

@SuppressWarnings({"SameParameterValue", "unused", "DuplicatedCode"})
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ConfigSpec {
    
    public static class DefinitionError extends RuntimeException {
        public DefinitionError(String message) {
            super(message);
        }
    }
    
    public static abstract class SpecNode {
        @Nullable
        final String comment;
        final boolean advanced;
        final boolean hidden;
        final boolean reloadable;
        
        private SpecNode(@Nullable String comment, boolean advanced, boolean hidden, boolean reloadable) {
            this.comment = comment;
            this.advanced = advanced;
            this.hidden = hidden;
            this.reloadable = reloadable;
        }
    }
    
    public static class SpecClazzNode extends SpecNode {
        final Class<?> clazz;
        final Map<String, SpecClazzNode> clazzNodes;
        final Map<String, SpecFieldNode> fieldNodes;
        
        private SpecClazzNode(Class<?> clazz, Map<String, SpecClazzNode> clazzNodes, Map<String, SpecFieldNode> fieldNodes, @Nullable String comment, boolean advanced, boolean hidden, boolean reloadable) {
            super(comment, advanced, hidden, reloadable);
            this.clazz = clazz;
            this.clazzNodes = Collections.unmodifiableMap(clazzNodes);
            this.fieldNodes = Collections.unmodifiableMap(fieldNodes);
        }
    }
    
    public static class SpecFieldNode extends SpecNode {
        @Nonnull
        final Field field;
        
        private SpecFieldNode(Field field, @Nullable String comment, boolean advanced, boolean hidden, boolean reloadable) {
            super(comment, advanced, hidden, reloadable);
            this.field = field;
        }
    }
    
    public static class SpecObjectNode extends SpecFieldNode {
        final Class<?> clazz;
        final Map<String, SpecFieldNode> subNodes;
        
        private SpecObjectNode(Class<?> clazz, Map<String, SpecFieldNode> subNodes, Field field, @Nullable String comment, boolean advanced, boolean hidden, boolean reloadable) {
            super(field, comment, advanced, hidden, reloadable);
            this.clazz = clazz;
            this.subNodes = Collections.unmodifiableMap(subNodes);
        }
    }
    
    public static class SpecElementNode extends SpecNode {
        final Class<?> clazz;
        final Map<String, SpecFieldNode> subNodes;
        
        private SpecElementNode(Class<?> clazz, Map<String, SpecFieldNode> subNodes, boolean advanced, boolean hidden, boolean reloadable) {
            super(null, advanced, hidden, reloadable);
            this.clazz = clazz;
            this.subNodes = Collections.unmodifiableMap(subNodes);
        }
    }
    
    public static class SpecMapNode extends SpecFieldNode {
        final Class<?> elementClass;
        final SpecElementNode nodeType;
        final Map<String, SpecElementNode> defaultSubNodes;
        
        private SpecMapNode(Class<?> elementClass, SpecElementNode nodeType, Map<String, SpecElementNode> defaultSubNodes, Field field, @Nullable String comment, boolean advanced, boolean hidden, boolean reloadable) {
            super(field, comment, advanced, hidden, reloadable);
            this.elementClass = elementClass;
            this.nodeType = nodeType;
            this.defaultSubNodes = Collections.unmodifiableMap(defaultSubNodes);
        }
    }
    
    public static class SpecListNode extends SpecFieldNode {
        final Class<?> elementClass;
        final SpecElementNode subNodeType;
        final List<SpecElementNode> defaultSubNodes;
        
        private SpecListNode(Class<?> elementClass, SpecElementNode subNodeType, List<SpecElementNode> defaultSubNodes, Field field, @Nullable String comment, boolean advanced, boolean hidden, boolean reloadable) {
            super(field, comment, advanced, hidden, reloadable);
            this.elementClass = elementClass;
            this.subNodeType = subNodeType;
            this.defaultSubNodes = Collections.unmodifiableList(defaultSubNodes);
        }
    }
    
    public static class SpecStringNode extends SpecFieldNode {
        final String defaultString;
        
        private SpecStringNode(String defaultString, Field field, @Nullable String comment, boolean advanced, boolean hidden, boolean reloadable) {
            super(field, comment, advanced, hidden, reloadable);
            this.defaultString = defaultString;
        }
    }
    
    public static class SpecEnumNode extends SpecFieldNode {
        final Class<?> enumClass;
        final String defaultValue;
        final List<String> allowedValues;
        
        private SpecEnumNode(Class<?> enumClass, String defaultValue, List<String> allowedValues, Field field, @Nullable String comment, boolean advanced, boolean hidden, boolean reloadable) {
            super(field, comment, advanced, hidden, reloadable);
            this.enumClass = enumClass;
            this.defaultValue = defaultValue;
            this.allowedValues = Collections.unmodifiableList(allowedValues);
        }
    }
    
    public static class SpecNumberNode extends SpecFieldNode {
        final boolean integral;
        final boolean lowerInclusive;
        final double lowerBound;
        final boolean upperInclusive;
        final double upperBound;
        final double defaultValue;
        
        private SpecNumberNode(boolean integral, boolean lowerInclusive, double lowerBound, boolean upperInclusive, double upperBound, double defaultValue, Field field, String comment, boolean advanced, boolean hidden, boolean reloadable) {
            super(field, comment, advanced, hidden, reloadable);
            this.integral = integral;
            this.lowerInclusive = lowerInclusive;
            this.lowerBound = lowerBound;
            this.upperInclusive = upperInclusive;
            this.upperBound = upperBound;
            this.defaultValue = defaultValue;
        }
    }
    
    public static class SpecBooleanNode extends SpecFieldNode {
        final boolean defaultValue;
        
        private SpecBooleanNode(boolean defaultValue, Field field, String comment, boolean advanced, boolean hidden, boolean reloadable) {
            super(field, comment, advanced, hidden, reloadable);
            this.defaultValue = defaultValue;
        }
    }
    
    @Nonnull
    public final SpecNode masterNode;
    
    ConfigSpec(Field field, Object object) {
        field.setAccessible(true);
        var masterNode = buildNodeForObject(object.getClass(), object, field, null, false, false, true);
        if (masterNode == null) {
            throw new IllegalArgumentException("Unable to build root node");
        }
        this.masterNode = masterNode;
    }
    
    public ConfigSpec(SpecNode masterNode) {
        this.masterNode = masterNode;
    }
    
    public Element trimAndRegenerateTree(Element tree, boolean enableAdvanced) {
        return regenerateElementTree(trimElementTree(tree), enableAdvanced);
    }
    
    @Nullable
    public Element trimElementTree(Element tree) {
        return trimElementForNode(tree, masterNode);
    }
    
    @Nullable
    public Element trimElementForNode(Element element, SpecNode node) {
        if (node instanceof SpecClazzNode clazzNode) {
            if (element.type != Element.Type.Section) {
                return null;
            }
            final var trimmedElements = new ArrayList<Element>();
            for (final var subElement : element.asArray()) {
                SpecNode subNode = clazzNode.fieldNodes.get(subElement.name);
                if (subNode == null) {
                    subNode = clazzNode.clazzNodes.get(subElement.name);
                }
                if (subNode == null) {
                    continue;
                }
                final var newElement = trimElementForNode(subElement, subNode);
                if (newElement == null) {
                    continue;
                }
                trimmedElements.add(newElement);
            }
            if (trimmedElements.size() == 0) {
                return null;
            }
            return new Element(Element.Type.Section, clazzNode.comment, element.name, trimmedElements.toArray());
        } else if (node instanceof SpecObjectNode objectNode) {
            if (element.type != Element.Type.Section) {
                return null;
            }
            
            final var trimmedElements = new ArrayList<Element>();
            for (final var subElement : element.asArray()) {
                final var subNode = objectNode.subNodes.get(subElement.name);
                if (subNode == null) {
                    continue;
                }
                final var newElement = trimElementForNode(subElement, subNode);
                if (newElement == null) {
                    continue;
                }
                trimmedElements.add(newElement);
            }
            if (trimmedElements.size() == 0) {
                return null;
            }
            return new Element(Element.Type.Section, objectNode.comment, element.name, trimmedElements.toArray());
        } else if (node instanceof SpecElementNode elementNode) {
            if (element.type != Element.Type.Section) {
                return null;
            }
            
            final var trimmedElements = new ArrayList<Element>();
            for (final var subElement : element.asArray()) {
                final var subNode = elementNode.subNodes.get(subElement.name);
                if (subNode == null) {
                    continue;
                }
                final var newElement = trimElementForNode(subElement, subNode);
                if (newElement == null) {
                    continue;
                }
                trimmedElements.add(newElement);
            }
            if (trimmedElements.size() == 0) {
                return null;
            }
            return new Element(Element.Type.Section, elementNode.comment, element.name, trimmedElements.toArray());
        } else if (node instanceof SpecMapNode mapNode) {
            if (element.type != Element.Type.Section) {
                return null;
            }
            final var subNode = mapNode.nodeType;
            
            final var trimmedElements = new ArrayList<Element>();
            for (final var subElement : element.asArray()) {
                Element newElement = trimElementForNode(subElement, subNode);
                if (newElement == null) {
                    continue;
                }
                trimmedElements.add(newElement);
            }
            if (trimmedElements.size() == 0) {
                return null;
            }
            return new Element(Element.Type.Section, mapNode.comment, element.name, trimmedElements.toArray());
        } else if (node instanceof SpecListNode listNode) {
            if (element.type != Element.Type.Array) {
                return null;
            }
            final var subNode = listNode.subNodeType;
            
            final var trimmedElements = new ArrayList<Element>();
            for (final var subElement : element.asArray()) {
                final var newElement = trimElementForNode(subElement, subNode);
                if (newElement == null) {
                    continue;
                }
                trimmedElements.add(newElement);
            }
            if (trimmedElements.size() == 0) {
                return null;
            }
            return new Element(Element.Type.Array, listNode.comment, element.name, trimmedElements.toArray());
        } else if (node instanceof SpecStringNode) {
            if (element.type != Element.Type.String && element.type != Element.Type.Number && element.type != Element.Type.Boolean) {
                return null;
            }
            return element;
        } else if (node instanceof SpecEnumNode enumNode) {
            if (element.type != Element.Type.String) {
                return null;
            }
            final var enumVals = (Enum<?>[]) enumNode.enumClass.getEnumConstants();
            final var enumValStrings = new String[enumVals.length];
            for (int i = 0; i < enumVals.length; i++) {
                enumValStrings[i] = enumVals[i].toString().toLowerCase(Locale.ENGLISH);
            }
            final var nameGiven = element.asString().toLowerCase(Locale.ENGLISH);
            Enum<?> givenVal = null;
            for (int i = 0; i < enumValStrings.length; i++) {
                if (nameGiven.equals(enumValStrings[i])) {
                    if (enumNode.allowedValues.isEmpty()) {
                        givenVal = enumVals[i];
                        break;
                    }
                    for (final var allowedValue : enumNode.allowedValues) {
                        if (nameGiven.equals(allowedValue.toLowerCase(Locale.ENGLISH))) {
                            givenVal = enumVals[i];
                            break;
                        }
                    }
                    break;
                }
            }
            if (givenVal == null) {
                return null;
            }
            return new Element(Element.Type.String, enumNode.comment, element.name, givenVal.toString());
        } else if (node instanceof SpecNumberNode numberNode) {
            if (element.type != Element.Type.Number) {
                return null;
            }
            double val = element.asDouble();
            if (isIntegral(numberNode.field.getType())) {
                long realVal = Math.round(val);
                if (realVal < numberNode.lowerBound || realVal > numberNode.upperBound ||
                        (realVal <= numberNode.lowerBound && !(numberNode.lowerInclusive)) ||
                        (realVal >= numberNode.upperBound && !numberNode.upperInclusive)) {
                    if (realVal <= numberNode.lowerBound) {
                        realVal = Math.round(numberNode.lowerBound);
                        if (!numberNode.lowerInclusive) {
                            realVal++;
                        }
                    } else if (realVal >= numberNode.upperBound) {
                        realVal = Math.round(numberNode.upperBound);
                        if (!numberNode.upperInclusive) {
                            realVal--;
                        }
                    }
                }
                val = realVal;
            } else {
                if (val < numberNode.lowerBound || val > numberNode.upperBound ||
                        (val <= numberNode.lowerBound && !(numberNode.lowerInclusive)) ||
                        (val >= numberNode.upperBound && !numberNode.upperInclusive)) {
                    if (val <= numberNode.lowerBound) {
                        val = numberNode.lowerBound;
                        if (!numberNode.lowerInclusive) {
                            val = Math.nextAfter(val, Double.POSITIVE_INFINITY);
                        }
                    } else if (val >= numberNode.upperBound) {
                        val = numberNode.upperBound;
                        if (!numberNode.upperInclusive) {
                            val = Math.nextAfter(val, Double.NEGATIVE_INFINITY);
                        }
                    }
                }
            }
            return new Element(Element.Type.Number, numberNode.comment, element.name, String.valueOf(val));
        } else if (node instanceof SpecBooleanNode booleanNode) {
            if (element.type != Element.Type.String && element.type != Element.Type.Number && element.type != Element.Type.Boolean) {
                return null;
            }
            final boolean newVal;
            if (element.type == Element.Type.String || element.type == Element.Type.Boolean) {
                String str = element.asString();
                newVal = Boolean.parseBoolean(str);
            } else {
                newVal = element.asDouble() != 0;
            }
            
            return new Element(Element.Type.Boolean, booleanNode.comment, element.name, String.valueOf(newVal));
        }
        
        return null;
    }
    
    public Element regenerateElementTree(@Nullable Element tree, boolean enableAdvanced) {
        try {
            return regenerateElementTreeForNode(tree, masterNode, null, null, enableAdvanced);
        } catch (IllegalAccessException e) {
            ConfigManager.LOGGER.error("Unexpected error caught regenerating config");
            ConfigManager.LOGGER.error(e.toString());
            throw new DefinitionError(e.getMessage());
        }
    }
    
    public Element regenerateElementTreeForNode(@Nullable Element tree, SpecNode node, @Nullable Object object, @Nullable String name, boolean enableAdvanced) throws IllegalAccessException {
        if (tree == null) {
            return generateElementForNode(node, object, name, enableAdvanced, false);
        }
        
        if (node instanceof SpecClazzNode clazzNode) {
            if (tree.type != Element.Type.Section) {
                return generateElementForNode(node, object, name, enableAdvanced, false);
            }
            
            final var subElements = new ArrayList<Element>();
            
            final var elements = tree.asArray();
            
            for (final var entry : clazzNode.fieldNodes.entrySet()) {
                nextEntry:
                {
                    for (final var element : elements) {
                        if (entry.getKey().equals(element.name)) {
                            subElements.add(regenerateElementTreeForNode(element, entry.getValue(), null, entry.getKey(), enableAdvanced));
                            break nextEntry;
                        }
                    }
                    if ((enableAdvanced || !entry.getValue().advanced) && !entry.getValue().hidden) {
                        subElements.add(regenerateElementTreeForNode(null, entry.getValue(), null, entry.getKey(), enableAdvanced));
                    }
                }
            }
            
            for (final var entry : clazzNode.clazzNodes.entrySet()) {
                nextEntry:
                {
                    for (final var element : elements) {
                        if (entry.getKey().equals(element.name)) {
                            if ((enableAdvanced || !entry.getValue().advanced) && !entry.getValue().hidden) {
                                Element subElement = regenerateElementTreeForNode(element, entry.getValue(), null, entry.getKey(), enableAdvanced);
                                if (subElement.asArray().length != 0) {
                                    subElements.add(subElement);
                                }
                            }
                            break nextEntry;
                        }
                    }
                    if ((enableAdvanced || !entry.getValue().advanced) && !entry.getValue().hidden) {
                        final var subElement = regenerateElementTreeForNode(null, entry.getValue(), null, entry.getKey(), enableAdvanced);
                        if (subElement.asArray().length != 0) {
                            subElements.add(subElement);
                        }
                    }
                }
            }
            
            return new Element(Element.Type.Section, node.comment, name, subElements.toArray());
        } else if (node instanceof SpecObjectNode objectNode) {
            if (tree.type != Element.Type.Section) {
                return generateElementForNode(objectNode, object, name, enableAdvanced, false);
            }
            
            var nodeObject = objectNode.field.get(object);
            
            if (nodeObject == null) {
                nodeObject = createClassInstance(objectNode.clazz);
                objectNode.field.set(object, nodeObject);
            }
            
            final var subElements = new ArrayList<Element>();
            
            final var elements = tree.asArray();
            
            for (final var entry : objectNode.subNodes.entrySet()) {
                nextEntry:
                {
                    for (Element element : elements) {
                        if (entry.getKey().equals(element.name)) {
                            final var subElement = regenerateElementTreeForNode(element, entry.getValue(), nodeObject, entry.getKey(), enableAdvanced);
                            if (subElement.subElemenets() != 0) {
                                subElements.add(subElement);
                            }
                            break nextEntry;
                        }
                    }
                    if ((enableAdvanced || !entry.getValue().advanced) && !entry.getValue().hidden) {
                        final var subElement = regenerateElementTreeForNode(null, entry.getValue(), nodeObject, entry.getKey(), enableAdvanced);
                        if (subElement.subElemenets() != 0) {
                            subElements.add(subElement);
                        }
                    }
                }
            }
            
            return new Element(Element.Type.Section, objectNode.comment, name, subElements.toArray());
        } else if (node instanceof SpecElementNode elementNode) {
            if (tree.type != Element.Type.Section) {
                return generateElementForNode(elementNode, object, name, enableAdvanced, false);
            }
            
            if (object == null) {
                throw new IllegalArgumentException("Cannot write element node into null object");
            }
            
            final var subElements = new ArrayList<>();
            
            final var elements = tree.asArray();
            
            for (final var entry : elementNode.subNodes.entrySet()) {
                nextEntry:
                {
                    for (final var element : elements) {
                        if (entry.getKey().equals(element.name)) {
                            final var subElement = regenerateElementTreeForNode(element, entry.getValue(), object, entry.getKey(), enableAdvanced);
                            if (subElement.subElemenets() != 0) {
                                subElements.add(subElement);
                            }
                            break nextEntry;
                        }
                    }
                    if ((enableAdvanced || !entry.getValue().advanced) && !entry.getValue().hidden) {
                        final var subElement = regenerateElementTreeForNode(null, entry.getValue(), object, entry.getKey(), enableAdvanced);
                        if (subElement.subElemenets() != 0) {
                            subElements.add(subElement);
                        }
                    }
                }
            }
            
            return new Element(Element.Type.Section, elementNode.comment, name, subElements.toArray());
        } else if (node instanceof SpecMapNode mapNode) {
            if (tree.type != Element.Type.Section) {
                return generateElementForNode(mapNode, object, name, enableAdvanced, false);
            }
            
            final var subElements = new ArrayList<Element>();
            
            final var elements = tree.asArray();
            
            final var nodeObject = mapNode.field.get(object);
            @SuppressWarnings("unchecked") final var map = (Map<String, Object>) nodeObject;
            
            for (final var element : elements) {
                Object elementObject = map.get(element.name);
                if (elementObject == null) {
                    elementObject = createClassInstance(mapNode.elementClass);
                    map.put(element.name, elementObject);
                }
                subElements.add(regenerateElementTreeForNode(element, mapNode.nodeType, elementObject, element.name, enableAdvanced));
            }
            
            return new Element(Element.Type.Section, mapNode.comment, name, subElements.toArray());
        } else if (node instanceof SpecListNode listNode) {
            if (tree.type != Element.Type.Array) {
                return generateElementForNode(listNode, object, name, enableAdvanced, false);
            }
            
            final var subElements = new ArrayList<Element>();
            
            final var elements = tree.asArray();
            
            final var nodeObject = listNode.field.get(object);
            //noinspection unchecked
            final var list = (List<Object>) nodeObject;
            
            for (int i = 0; i < elements.length; i++) {
                final Object elementObject;
                if (i < list.size()) {
                    elementObject = list.get(i);
                } else {
                    elementObject = createClassInstance((listNode).elementClass);
                    list.add(elementObject);
                }
                subElements.add(regenerateElementTreeForNode(elements[i], listNode.subNodeType, elementObject, null, enableAdvanced));
            }
            
            return new Element(Element.Type.Array, listNode.comment, name, subElements.toArray());
        } else if (node instanceof SpecEnumNode enumNode) {
            if (tree.type != Element.Type.String) {
                return generateElementForNode(enumNode, object, name, enableAdvanced, false);
            }
            
            final var enumVals = (Enum<?>[]) enumNode.enumClass.getEnumConstants();
            final var enumValStrings = new String[enumVals.length];
            for (int i = 0; i < enumVals.length; i++) {
                enumValStrings[i] = enumVals[i].toString().toLowerCase(Locale.ENGLISH);
            }
            final var nameGiven = tree.asString().toLowerCase(Locale.ENGLISH);
            Enum<?> givenVal = null;
            for (int i = 0; i < enumValStrings.length; i++) {
                if (nameGiven.equals(enumValStrings[i])) {
                    if (enumNode.allowedValues.isEmpty()) {
                        givenVal = enumVals[i];
                        break;
                    }
                    for (String allowedValue : enumNode.allowedValues) {
                        if (nameGiven.equals(allowedValue.toLowerCase(Locale.ENGLISH))) {
                            givenVal = enumVals[i];
                            break;
                        }
                    }
                    break;
                }
            }
            if (givenVal == null) {
                return generateElementForNode(enumNode, object, name, enableAdvanced, false);
            }
            return tree;
            
        } else if (node instanceof SpecStringNode) {
            if (tree.type != Element.Type.String && tree.type != Element.Type.Number && tree.type != Element.Type.Boolean) {
                return generateElementForNode(node, object, name, enableAdvanced, false);
            }
            return tree;
        } else if (node instanceof SpecNumberNode numberNode) {
            if (tree.type != Element.Type.Number) {
                return generateElementForNode(numberNode, object, name, enableAdvanced, false);
            }
            double val = tree.asDouble();
            if (numberNode.integral) {
                long realVal = Math.round(val);
                if (realVal < numberNode.lowerBound || realVal > numberNode.upperBound ||
                        (realVal <= numberNode.lowerBound && !(numberNode.lowerInclusive)) ||
                        (realVal >= numberNode.upperBound && !numberNode.upperInclusive)) {
                    if (realVal <= numberNode.lowerBound) {
                        realVal = Math.round(numberNode.lowerBound);
                        if (!numberNode.lowerInclusive) {
                            realVal++;
                        }
                    } else if (realVal >= numberNode.upperBound) {
                        realVal = Math.round(numberNode.upperBound);
                        if (!numberNode.upperInclusive) {
                            realVal--;
                        }
                    }
                }
                val = realVal;
            } else {
                if (val < numberNode.lowerBound || val > numberNode.upperBound ||
                        (val <= numberNode.lowerBound && !(numberNode.lowerInclusive)) ||
                        (val >= numberNode.upperBound && !numberNode.upperInclusive)) {
                    if (val <= numberNode.lowerBound) {
                        val = numberNode.lowerBound;
                        if (!numberNode.lowerInclusive) {
                            val = Math.nextAfter(val, Double.POSITIVE_INFINITY);
                        }
                    } else if (val >= numberNode.upperBound) {
                        val = numberNode.upperBound;
                        if (!numberNode.upperInclusive) {
                            val = Math.nextAfter(val, Double.NEGATIVE_INFINITY);
                        }
                    }
                }
            }
            return new Element(Element.Type.Number, numberNode.comment, tree.name, String.valueOf(val));
        } else if (node instanceof SpecBooleanNode boolNode) {
            if (tree.type != Element.Type.String && tree.type != Element.Type.Number && tree.type != Element.Type.Boolean) {
                return generateElementForNode(boolNode, object, name, enableAdvanced, false);
            }
            final boolean newVal;
            if (tree.type == Element.Type.String || tree.type == Element.Type.Boolean) {
                String str = tree.asString();
                newVal = Boolean.parseBoolean(str);
            } else {
                newVal = tree.asDouble() != 0;
            }
            
            return new Element(Element.Type.Boolean, boolNode.comment, tree.name, String.valueOf(newVal));
        }
        
        throw new DefinitionError("Attempting to regenerate element for unknown node type");
    }
    
    public Element generateElementTree(boolean enableAdvanced) {
        return generateElementTree(enableAdvanced, false);
    }
    
    public Element generateElementTree(boolean enableAdvanced, boolean fullTree) {
        try {
            return generateElementForNode(masterNode, null, null, enableAdvanced | fullTree, fullTree);
        } catch (IllegalAccessException e) {
            ConfigManager.LOGGER.error("Unexpected error caught reading from config");
            ConfigManager.LOGGER.error(e.toString());
            throw new DefinitionError(e.getMessage());
        }
    }
    
    public Element generateElementForNode(SpecNode node, @Nullable Object object, @Nullable String name, boolean enableAdvanced, boolean fullTree) throws IllegalAccessException {
        if (node instanceof SpecClazzNode clazzNode) {
            final var subElements = new ArrayList<Element>();
            
            for (final var entry : clazzNode.fieldNodes.entrySet()) {
                if (fullTree || (enableAdvanced || !entry.getValue().advanced) && !entry.getValue().hidden) {
                    subElements.add(generateElementForNode(entry.getValue(), null, entry.getKey(), enableAdvanced, fullTree));
                }
            }
            
            for (final var entry : clazzNode.clazzNodes.entrySet()) {
                if (fullTree || (enableAdvanced || !entry.getValue().advanced) && !entry.getValue().hidden) {
                    subElements.add(generateElementForNode(entry.getValue(), null, entry.getKey(), enableAdvanced, fullTree));
                }
            }
            
            return new Element(Element.Type.Section, clazzNode.comment, name, subElements.toArray());
            
        } else if (node instanceof SpecObjectNode objectNode) {
            final Object nodeObject = objectNode.field.get(object);
            
            final var subElements = new ArrayList<Element>();
            
            for (final var entry : objectNode.subNodes.entrySet()) {
                if (fullTree || (enableAdvanced || !entry.getValue().advanced) && !entry.getValue().hidden) {
                    subElements.add(generateElementForNode(entry.getValue(), nodeObject, entry.getKey(), enableAdvanced, fullTree));
                }
            }
            
            return new Element(Element.Type.Section, objectNode.comment, name, subElements.toArray());
        } else if (node instanceof SpecElementNode elementNode) {
            final var subElements = new ArrayList<Element>();
            
            for (Map.Entry<String, SpecFieldNode> entry : elementNode.subNodes.entrySet()) {
                if (fullTree || (enableAdvanced || !entry.getValue().advanced) && !entry.getValue().hidden) {
                    subElements.add(generateElementForNode(entry.getValue(), object, entry.getKey(), enableAdvanced, fullTree));
                }
            }
            
            return new Element(Element.Type.Section, elementNode.comment, name, subElements.toArray());
        } else if (node instanceof SpecMapNode mapNode) {
            final var nodeObject = mapNode.field.get(object);
            assert nodeObject instanceof Map;
            @SuppressWarnings("unchecked") final var map = (Map<String, ?>) nodeObject;
            final var subElements = new ArrayList<Element>();
            
            for (Map.Entry<String, ?> entry : map.entrySet()) {
                subElements.add(generateElementForNode(mapNode.nodeType, entry.getValue(), entry.getKey(), enableAdvanced, fullTree));
            }
            
            return new Element(Element.Type.Section, mapNode.comment, name, subElements.toArray());
        } else if (node instanceof SpecListNode listNode) {
            final var nodeObject = listNode.field.get(object);
            final var subNodeType = listNode.subNodeType;
            final var subElements = new ArrayList<Element>();
            
            if (nodeObject instanceof List<?> list) {
                for (Object o : list) {
                    subElements.add(generateElementForNode(subNodeType, o, null, enableAdvanced, fullTree));
                }
            } else {
                assert nodeObject.getClass().isArray();
                final int length = Array.getLength(nodeObject);
                for (int i = 0; i < length; i++) {
                    subElements.add(generateElementForNode(subNodeType, Array.get(nodeObject, i), null, enableAdvanced, fullTree));
                }
            }
            
            return new Element(Element.Type.Array, listNode.comment, name, subElements.toArray());
        } else if (node instanceof SpecStringNode stringNode) {
            final var val = stringNode.field.get(object).toString();
            return new Element(Element.Type.String, stringNode.comment, name, val);
        } else if (node instanceof SpecEnumNode enumNode) {
            final var val = enumNode.field.get(object).toString();
            return new Element(Element.Type.String, enumNode.comment, name, val);
        } else if (node instanceof SpecNumberNode numberNode) {
            final var num = (Number) numberNode.field.get(object);
            return new Element(Element.Type.Number, numberNode.comment, name, String.valueOf(num.doubleValue()));
        } else if (node instanceof SpecBooleanNode boolNode) {
            final var bool = (Boolean) (boolNode).field.get(object);
            return new Element(Element.Type.Boolean, boolNode.comment, name, bool.toString());
        }
        
        throw new DefinitionError("Attempting to generate element for unknown node type");
    }
    
    public void writeElementTree(Element tree, boolean isReload) {
        try {
            writeElementNode(tree, masterNode, null, isReload);
        } catch (IllegalAccessException e) {
            ConfigManager.LOGGER.error("Unexpected error caught reading from config");
            e.printStackTrace();
            throw new DefinitionError(e.getMessage());
        }
    }
    
    public static void writeElementNode(Element element, SpecNode node, @Nullable Object object, boolean isReload) throws IllegalAccessException {
        if(isReload && !node.reloadable){
            return;
        }
        if (node instanceof SpecClazzNode clazzNode) {
            if (element.type != Element.Type.Section) {
                ConfigManager.LOGGER.info("Invalid config structure given");
                ConfigManager.LOGGER.info("Attempting to write " + element.type + " to a Class");
                return;
            }
            
            final var subElements = element.asArray();
            
            for (final var subElement : subElements) {
                final var subClazzNode = clazzNode.clazzNodes.get(subElement.name);
                final var fieldNode = clazzNode.fieldNodes.get(subElement.name);
                if (subClazzNode != null) {
                    writeElementNode(subElement, subClazzNode, null, isReload);
                } else if (fieldNode != null) {
                    writeElementNode(subElement, fieldNode, null, isReload);
                } else {
                    Phosphophyllite.LOGGER.info("Unknown config option given: " + subElement.name);
                }
            }
            return;
        } else if (node instanceof SpecObjectNode objectNode) {
            if (element.type != Element.Type.Section) {
                ConfigManager.LOGGER.info("Invalid config structure given");
                ConfigManager.LOGGER.info("Attempting to write " + element.type + " to an Object");
                return;
            }
            
            var nodeObject = objectNode.field.get(object);
            boolean existingObject = true;
            if (nodeObject == null) {
                nodeObject = createClassInstance(objectNode.clazz);
                existingObject = false;
            }
            
            final var subElements = element.asArray();
            
            for (final var subElement : subElements) {
                final var subNode = objectNode.subNodes.get(subElement.name);
                if (subNode != null) {
                    writeElementNode(subElement, subNode, nodeObject, isReload);
                } else {
                    Phosphophyllite.LOGGER.info("Unknown config option given: " + element.name);
                }
            }
            
            if (!existingObject) {
                objectNode.field.set(object, nodeObject);
            }
            return;
        } else if (node instanceof SpecElementNode elementNode) {
            if (element.type != Element.Type.Section) {
                ConfigManager.LOGGER.info("Invalid config structure given");
                ConfigManager.LOGGER.info("Attempting to write " + element.type + " to an Object");
                return;
            }
            
            final var subElements = element.asArray();
            
            for (final var subElement : subElements) {
                final var subNode = elementNode.subNodes.get(subElement.name);
                if (subNode != null) {
                    writeElementNode(subElement, subNode, object, isReload);
                } else {
                    Phosphophyllite.LOGGER.info("Unknown config option given: " + element.name);
                }
            }
            
            return;
        } else if (node instanceof SpecMapNode mapNode) {
            if (element.type != Element.Type.Section) {
                ConfigManager.LOGGER.info("Invalid config structure given");
                ConfigManager.LOGGER.info("Attempting to write " + element.type + " to a Map");
                return;
            }
            
            @SuppressWarnings("rawtypes") final var map = (Map) createClassInstance(mapNode.field.getType());
            
            final var subElements = element.asArray();
            
            for (final var subElement : subElements) {
                final Object newElementObject = createClassInstance(mapNode.elementClass);
                //noinspection unchecked
                map.put(subElement.name, newElementObject);
                writeElementNode(subElement, mapNode.nodeType, newElementObject, isReload);
            }
            
            mapNode.field.set(object, map);
            return;
        } else if (node instanceof SpecListNode listNode) {
            if (element.type != Element.Type.Array) {
                ConfigManager.LOGGER.info("Invalid config structure given");
                ConfigManager.LOGGER.info("Attempting to write " + element.type + " to an Array");
                return;
            }
            
            final var subElements = element.asArray();
            
            if (listNode.field.getType().isArray()) {
                final var array = Array.newInstance(listNode.elementClass, subElements.length);
                for (int i = 0; i < subElements.length; i++) {
                    Object newElementObject = createClassInstance(listNode.elementClass);
                    
                    final var subElement = subElements[i];
                    Array.set(array, i, newElementObject);
                    
                    writeElementNode(subElement, listNode.subNodeType, newElementObject, isReload);
                }
                
                listNode.field.set(object, array);
            } else {
                
                @SuppressWarnings("rawtypes") final var list = (List) createClassInstance(listNode.field.getType());
                
                for (Element subElement : subElements) {
                    final var newElementObject = createClassInstance(listNode.elementClass);
                    writeElementNode(subElement, listNode.subNodeType, newElementObject, isReload);
                    //noinspection unchecked
                    list.add(newElementObject);
                }
                
                listNode.field.set(object, list);
            }
            return;
        } else if (node instanceof SpecStringNode stringNode) {
            if (element.type != Element.Type.String && element.type != Element.Type.Number && element.type != Element.Type.Boolean) {
                ConfigManager.LOGGER.info("Invalid config structure given");
                ConfigManager.LOGGER.info("Attempting to write " + element.type + " to a String");
                return;
            }
            stringNode.field.set(object, element.asString());
            return;
        } else if (node instanceof SpecEnumNode enumNode) {
            if (element.type != Element.Type.String) {
                ConfigManager.LOGGER.info("Invalid config structure given");
                ConfigManager.LOGGER.info("Attempting to write " + element.type + " to a Enum");
                return;
            }
            final var enumVals = (Enum<?>[]) enumNode.enumClass.getEnumConstants();
            final var enumValStrings = new String[enumVals.length];
            for (int i = 0; i < enumVals.length; i++) {
                enumValStrings[i] = enumVals[i].toString().toLowerCase(Locale.ENGLISH);
            }
            final var nameGiven = element.asString().toLowerCase(Locale.ENGLISH);
            Enum<?> givenVal = null;
            for (int i = 0; i < enumValStrings.length; i++) {
                if (nameGiven.equals(enumValStrings[i])) {
                    if (enumNode.allowedValues.isEmpty()) {
                        givenVal = enumVals[i];
                        break;
                    }
                    for (String allowedValue : enumNode.allowedValues) {
                        if (nameGiven.equals(allowedValue.toLowerCase(Locale.ENGLISH))) {
                            givenVal = enumVals[i];
                            break;
                        }
                    }
                    break;
                }
            }
            
            if (givenVal != null) {
                enumNode.field.set(object, givenVal);
            }
            
            return;
        } else if (node instanceof SpecNumberNode numberNode) {
            if (element.type != Element.Type.Number) {
                ConfigManager.LOGGER.info("Invalid config structure given");
                ConfigManager.LOGGER.info("Attempting to write " + element.type + " to a Number");
                return;
            }
            double val = element.asDouble();
            if (numberNode.integral) {
                long realVal = Math.round(val);
                if (realVal < numberNode.lowerBound || realVal > numberNode.upperBound ||
                        (realVal <= numberNode.lowerBound && !(numberNode.lowerInclusive)) ||
                        (realVal >= numberNode.upperBound && !numberNode.upperInclusive)) {
                    ConfigManager.LOGGER.warn("Number value " + element.name + " given out of range value " + realVal + ". Valid range is " +
                            ((numberNode.lowerInclusive ? "[" : "(" + ((numberNode.lowerBound == Double.MIN_VALUE) ? "" : numberNode.lowerBound))) +
                            "," +
                            (((numberNode.upperBound == Double.MAX_VALUE) ? "" : numberNode.upperBound) + (numberNode.upperInclusive ? "]" : ")")) +
                            ". Clamping to range");
                    if (realVal <= numberNode.lowerBound) {
                        realVal = Math.round(numberNode.lowerBound);
                        if (!numberNode.lowerInclusive) {
                            realVal++;
                        }
                    } else if (realVal >= numberNode.upperBound) {
                        realVal = Math.round(numberNode.upperBound);
                        if (!numberNode.upperInclusive) {
                            realVal--;
                        }
                    }
                }
                val = realVal;
            } else {
                if (val < numberNode.lowerBound || val > numberNode.upperBound ||
                        (val <= numberNode.lowerBound && !(numberNode.lowerInclusive)) ||
                        (val >= numberNode.upperBound && !numberNode.upperInclusive)) {
                    ConfigManager.LOGGER.warn("Number value " + element.name + " given out of range value " + val + ". Valid range is " +
                            ((numberNode.lowerInclusive ? "[" : "(" + ((numberNode.lowerBound == Double.MIN_VALUE) ? "" : numberNode.lowerBound))) +
                            "," +
                            (((numberNode.upperBound == Double.MAX_VALUE) ? "" : numberNode.upperBound) + (numberNode.upperInclusive ? "]" : ")")) +
                            ". Clamping to range");
                    if (val <= numberNode.lowerBound) {
                        val = numberNode.lowerBound;
                        if (!numberNode.lowerInclusive) {
                            val = Math.nextAfter(val, Double.POSITIVE_INFINITY);
                        }
                    } else if (val >= numberNode.upperBound) {
                        val = numberNode.upperBound;
                        if (!numberNode.upperInclusive) {
                            val = Math.nextAfter(val, Double.NEGATIVE_INFINITY);
                        }
                    }
                }
            }
            setNumberField(numberNode.field, object, val);
            return;
        } else if (node instanceof SpecBooleanNode booleanNode) {
            if (element.type != Element.Type.String && element.type != Element.Type.Number && element.type != Element.Type.Boolean) {
                ConfigManager.LOGGER.info("Invalid config structure given");
                ConfigManager.LOGGER.info("Attempting to write " + element.type + " to a Boolean");
                return;
            }
            final boolean newVal;
            if (element.type == Element.Type.String || element.type == Element.Type.Boolean) {
                String str = element.asString();
                newVal = Boolean.parseBoolean(str);
            } else {
                newVal = element.asDouble() != 0;
            }
            booleanNode.field.setBoolean(object, newVal);
            return;
        }
        
        ConfigManager.LOGGER.warn("Invalid config structure given");
        ConfigManager.LOGGER.warn("Attempting to write " + element.type + " to an unknown node type");
    }
    
    public void writeDefaults() {
        try {
            defaultNode(masterNode, null);
        } catch (IllegalAccessException e) {
            Phosphophyllite.LOGGER.error("Error caught writing defaults to config");
            Phosphophyllite.LOGGER.error(e.toString());
        }
    }
    
    public static void defaultNode(SpecNode node, @Nullable Object object) throws IllegalAccessException {
        if (node instanceof SpecClazzNode clazzNode) {
            for (final var entry : clazzNode.clazzNodes.entrySet()) {
                defaultNode(entry.getValue(), null);
            }
            
            for (final var entry : clazzNode.fieldNodes.entrySet()) {
                defaultNode(entry.getValue(), null);
            }
        } else if (node instanceof SpecObjectNode objectNode) {
            final var newObject = createClassInstance(objectNode.clazz);
            
            for (final var entry : objectNode.subNodes.entrySet()) {
                defaultNode(entry.getValue(), newObject);
            }
            objectNode.field.set(object, newObject);
        } else if (node instanceof SpecElementNode elementNode) {
            if (object == null) {
                return;
            }
            for (final var entry : elementNode.subNodes.entrySet()) {
                defaultNode(entry.getValue(), object);
            }
        } else if (node instanceof SpecMapNode mapNode) {
            
            final var newMap = new LinkedHashMap<String, Object>();
            
            for (final var entry : mapNode.defaultSubNodes.entrySet()) {
                Object obj = createClassInstance(mapNode.elementClass);
                defaultNode(entry.getValue(), obj);
                newMap.put(entry.getKey(), obj);
            }
            
            mapNode.field.set(object, newMap);
        } else if (node instanceof SpecListNode listNode) {
            
            final var newList = new ArrayList<>();
            
            for (final var defaultSubNode : listNode.defaultSubNodes) {
                Object obj = createClassInstance(listNode.elementClass);
                defaultNode(defaultSubNode, obj);
                newList.add(obj);
            }
            
            listNode.field.set(object, newList);
        } else if (node instanceof SpecStringNode stringNode) {
            stringNode.field.set(object, stringNode.defaultString);
        } else if (node instanceof SpecEnumNode enumNode) {
            @SuppressWarnings({"unchecked", "rawtypes"}) final var enumVal = Enum.valueOf((Class<? extends Enum>) enumNode.enumClass, (enumNode).defaultValue);
            enumNode.field.set(object, enumVal);
        } else if (node instanceof SpecNumberNode numberNode) {
            setNumberField(numberNode.field, object, numberNode.defaultValue);
        } else if (node instanceof SpecBooleanNode boolNode) {
            boolNode.field.setBoolean(object, boolNode.defaultValue);
        }
    }
    
    public static boolean isIntegral(Class<?> numberType) {
        return
                numberType == Byte.class || numberType == byte.class ||
                        numberType == Short.class || numberType == short.class ||
                        numberType == Integer.class || numberType == int.class ||
                        numberType == Long.class || numberType == long.class;
    }
    
    public static void setNumberField(Field field, @Nullable Object object, double value) throws IllegalAccessException {
        final Object newVal;
        final var numberType = field.getType();
        
        if (numberType == Byte.TYPE || numberType == byte.class) {
            newVal = (byte) value;
        } else if (numberType == Short.TYPE || numberType == short.class) {
            newVal = (short) value;
        } else if (numberType == Integer.TYPE || numberType == int.class) {
            newVal = (int) value;
        } else if (numberType == Long.TYPE || numberType == long.class) {
            newVal = (long) value;
        } else if (numberType == Float.TYPE || numberType == float.class) {
            newVal = (float) value;
        } else if (numberType == Double.TYPE || numberType == double.class) {
            newVal = value;
        } else {
            newVal = null;
        }
        
        field.set(object, newVal);
    }
    
    @Nullable
    public static SpecClazzNode buildNodeForClazz(Class<?> clazz, @Nullable String comment, boolean advanced, boolean hidden) {
        
        final var clazzNodes = new LinkedHashMap<String, SpecClazzNode>();
        final var fieldNodes = new LinkedHashMap<String, SpecFieldNode>();
        
        for (final var subclass : clazz.getDeclaredClasses()) {
            final var subNode = buildNodeForClazz(subclass, null, advanced, hidden);
            if (subNode == null) {
                continue;
            }
            final var name = subclass.getSimpleName();
            if (clazzNodes.containsKey(name)) {
                throw new DefinitionError("Duplicate config name: " + name);
            }
            clazzNodes.put(name, subNode);
        }
        
        for (final var field : clazz.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            final var fieldNode = buildNodeForField(field, null);
            if (fieldNode == null) {
                continue;
            }
            final var name = field.getName();
            if (clazzNodes.containsKey(name)) {
                throw new DefinitionError("Duplicate config name: " + name);
            }
            if (fieldNodes.containsKey(name)) {
                throw new DefinitionError("Duplicate config name: " + name);
            }
            fieldNodes.put(name, fieldNode);
        }
        
        if (clazzNodes.isEmpty() && fieldNodes.isEmpty()) {
            return null;
        }
        
        return new SpecClazzNode(clazz, clazzNodes, fieldNodes, comment, advanced, hidden, true);
    }
    
    @Nullable
    public static SpecObjectNode buildNodeForObject(Class<?> clazz, Object object, Field field, @Nullable String comment, boolean advanced, boolean hidden, boolean reloadable) {
        
        final var subNodes = new LinkedHashMap<String, SpecFieldNode>();
        
        for (final var clazzField : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(clazzField.getModifiers())) {
                continue;
            }
            final var fieldNode = buildNodeForField(clazzField, object);
            if (fieldNode == null) {
                continue;
            }
            final var name = clazzField.getName();
            if (subNodes.containsKey(name)) {
                throw new DefinitionError("Duplicate config name: " + name);
            }
            subNodes.put(name, fieldNode);
        }
        
        if (subNodes.isEmpty()) {
            return null;
        }
        
        return new SpecObjectNode(clazz, subNodes, field, comment, advanced, hidden, reloadable);
    }
    
    @Nullable
    private static SpecElementNode buildNodeForElement(Class<?> clazz, Object object, boolean advanced, boolean hidden, boolean reloadable) {
        
        final var subNodes = new LinkedHashMap<String, SpecFieldNode>();
        
        for (final var clazzField : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(clazzField.getModifiers())) {
                continue;
            }
            final var fieldNode = buildNodeForField(clazzField, object);
            if (fieldNode == null) {
                continue;
            }
            final var name = clazzField.getName();
            if (subNodes.containsKey(name)) {
                throw new DefinitionError("Duplicate config name: " + name);
            }
            subNodes.put(name, fieldNode);
        }
        
        if (subNodes.isEmpty()) {
            return null;
        }
        
        return new SpecElementNode(clazz, subNodes, advanced, hidden, reloadable);
    }
    
    @Nullable
    public static SpecFieldNode buildNodeForField(final Field field, @Nullable final Object object) {
        if (!field.isAnnotationPresent(ConfigValue.class)) {
            return null;
        }
        field.setAccessible(true);
        
        final Object fieldObject;
        final Class<?> fieldClass = field.getType();
        if (Modifier.isStatic(field.getModifiers()) == (object == null)) {
            try {
                fieldObject = field.get(object);
            } catch (IllegalAccessException e) {
                Phosphophyllite.LOGGER.warn("Illegal Access attempting to get field");
                Phosphophyllite.LOGGER.warn(e.getMessage());
                return null;
            }
        } else {
            try {
                Constructor<?> constructor = fieldClass.getConstructor();
                constructor.setAccessible(true);
                fieldObject = constructor.newInstance();
            } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                Phosphophyllite.LOGGER.warn(e.getMessage());
                throw new DefinitionError("Unable to create default instance of field object");
            }
        }
        
        
        final ConfigValue fieldAnnotation = field.getAnnotation(ConfigValue.class);
        final var reloadable = fieldAnnotation.reloadable();
        
        final String comment;
        {
            final var commentBuilder = new StringBuilder(fieldAnnotation.comment());
            if (!fieldAnnotation.range().equals("(,)")) {
                if (commentBuilder.length() != 0) {
                    commentBuilder.append('\n');
                }
                commentBuilder.append("Valid range: ").append(fieldAnnotation.range());
            }
            
            if (fieldAnnotation.commentDefaultValue() &&
                    (fieldClass.isPrimitive()
                            || Number.class.isAssignableFrom(fieldClass)
                            || Boolean.class == fieldClass
                            || String.class == fieldClass
                            || fieldClass.isEnum()
                    )
            ) {
                if (commentBuilder.length() != 0) {
                    commentBuilder.append('\n');
                }
                commentBuilder.append("Default: ");
                commentBuilder.append(fieldObject.toString());
            }
            
            if (fieldClass.isEnum()) {
                var allowedValues = fieldAnnotation.allowedValues();
                if (allowedValues.length == 0) {
                    Field[] enumFields = fieldClass.getFields();
                    allowedValues = new String[enumFields.length];
                    for (int i = 0; i < enumFields.length; i++) {
                        allowedValues[i] = enumFields[i].getName();
                    }
                }
                if (commentBuilder.length() != 0) {
                    commentBuilder.append('\n');
                }
                commentBuilder.append("Allowed Values: ");
                for (final var allowedValue : allowedValues) {
                    commentBuilder.append(allowedValue);
                    commentBuilder.append(", ");
                }
            }
            comment = commentBuilder.toString();
        }
        
        final SpecFieldNode node;
        final boolean advanced = fieldAnnotation.advanced();
        final boolean hidden = fieldAnnotation.hidden();
        
        if (fieldClass.isArray() || List.class == fieldClass || ArrayList.class == fieldClass) {
            final var defaultSubNodes = new ArrayList<SpecElementNode>();
            final Class<?> elementClass;
            if (fieldClass.isArray()) {
                elementClass = fieldClass.getComponentType();
                for (int i = 0; i < Array.getLength(fieldObject); i++) {
                    final var element = Array.get(fieldObject, 0);
                    if (element != null) {
                        var subNode = buildNodeForElement(elementClass, element, advanced, hidden, reloadable);
                        if (subNode != null) {
                            defaultSubNodes.add(subNode);
                        }
                    }
                }
            } else {
                final var type = (ParameterizedType) field.getGenericType();
                final var generics = type.getActualTypeArguments();
                elementClass = (Class<?>) generics[0];
                
                final var list = (List<?>) fieldObject;
                list.forEach(element -> {
                    var subNode = buildNodeForElement(elementClass, element, advanced, hidden, reloadable);
                    if (subNode != null) {
                        defaultSubNodes.add(subNode);
                    }
                });
            }
            
            final var defaultObject = createClassInstance(elementClass);
            
            final var defaultNode = buildNodeForElement(elementClass, defaultObject, advanced, hidden, reloadable);
            if (defaultNode == null) {
                return null;
            }
            
            node = new SpecListNode(elementClass, defaultNode, defaultSubNodes, field, comment, advanced, hidden, reloadable);
        } else if (Map.class == fieldClass || HashMap.class == fieldClass) {
            final var type = (ParameterizedType) field.getGenericType();
            final var generics = type.getActualTypeArguments();
            
            if (generics[0] != String.class) {
                throw new RuntimeException("map keys must be strings");
            }
            
            final var elementClass = (Class<?>) generics[1];
            final var defaultSubNodes = new LinkedHashMap<String, SpecElementNode>();
            
            final Object defaultObject = createClassInstance(elementClass);
            
            final var defaultNode = buildNodeForElement(elementClass, defaultObject, advanced, hidden, reloadable);
            if (defaultNode == null) {
                return null;
            }
            
            // its checked, see me check that generic type like 10 lines ago
            @SuppressWarnings("unchecked") final var map = (Map<String, ?>) fieldObject;
            map.forEach((string, element) -> {
                var newNode = buildNodeForElement(elementClass, element, advanced, hidden, reloadable);
                if (newNode != null) {
                    defaultSubNodes.put(string, newNode);
                }
            });
            
            node = new SpecMapNode(elementClass, defaultNode, defaultSubNodes, field, comment, advanced, hidden, reloadable);
        } else if (String.class == fieldClass) {
            node = new SpecStringNode((String) fieldObject, field, comment, hidden, advanced, reloadable);
        } else if (fieldClass.isEnum()) {
            node = new SpecEnumNode(fieldClass, fieldObject.toString(), Arrays.asList(fieldAnnotation.allowedValues()), field, comment, advanced, hidden, reloadable);
        } else if (fieldClass.isPrimitive() || Number.class.isAssignableFrom(fieldClass) || Boolean.class == fieldClass) {
            if (fieldClass == boolean.class || fieldClass == Boolean.class) {
                node = new SpecBooleanNode((Boolean) fieldObject, field, comment, advanced, hidden, reloadable);
            } else {
                // if it isn't a boolean, and it is a primitive, then its a number or its void
                // cant declare a void variable, so its a number
                
                var range = fieldAnnotation.range();
                
                range = range.trim();
                if (range.length() < 3) {
                    throw new DefinitionError("Incomplete range given");
                }
                
                final char lowerInclusiveChar = range.charAt(0);
                final char higherInclusiveChar = range.charAt(range.length() - 1);
                
                final boolean lowerInclusive = switch (lowerInclusiveChar) {
                    case '(' -> false;
                    case '[' -> true;
                    default -> throw new DefinitionError("Unknown lower bound inclusivity");
                };
                final boolean higherInclusive = switch (higherInclusiveChar) {
                    case ')' -> false;
                    case ']' -> true;
                    default -> throw new DefinitionError("Unknown higher bound inclusivity");
                };
                
                range = range.substring(1, range.length() - 1).trim();
                final var bounds = range.split(",");
                if (bounds.length > 2) {
                    throw new DefinitionError("Range cannot have more than two bounds");
                }
                var lowerBoundStr = "";
                var higherBoundStr = "";
                if (bounds.length == 2) {
                    lowerBoundStr = bounds[0].trim();
                    higherBoundStr = bounds[1].trim();
                } else {
                    if (range.length() == 0) {
                        throw new DefinitionError("Incomplete range given");
                    }
                    if (range.length() != 1) {
                        if (bounds.length != 1) {
                            throw new DefinitionError("Incomplete range given");
                        }
                        if (range.charAt(0) == ',') {
                            higherBoundStr = bounds[0];
                        } else if (range.charAt(range.length() - 1) == ',') {
                            lowerBoundStr = bounds[0];
                        } else {
                            throw new DefinitionError("Incomplete range given");
                        }
                    } else if (range.charAt(0) != ',') {
                        throw new DefinitionError("Incomplete range given");
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
                    throw new DefinitionError("Higher bound must be greater or equal to lower bound");
                }
                
                node = new SpecNumberNode(isIntegral(fieldClass), lowerInclusive, lowerBound, higherInclusive, higherBound, ((Number) fieldObject).doubleValue(), field, comment, advanced, hidden, reloadable);
            }
        } else {
            node = buildNodeForObject(fieldClass, fieldObject, field, comment, advanced, hidden, reloadable);
        }
        
        return node;
    }
    
    public static Object createClassInstance(Class<?> elementClass) {
        try {
            final var constructor = elementClass.getConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            Phosphophyllite.LOGGER.warn(e.getMessage());
            throw new DefinitionError("Unable to create default instance of object");
        }
    }
}
