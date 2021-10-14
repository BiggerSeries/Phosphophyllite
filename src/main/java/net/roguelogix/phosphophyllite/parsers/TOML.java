package net.roguelogix.phosphophyllite.parsers;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import net.minecraft.MethodsReturnNonnullByDefault;
import org.apache.commons.lang3.NotImplementedException;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TOML {
    public static Element parseString(String string) {
        return parseObject(new TomlParser().parse(string), null);
    }
    
    private static Element parseObject(Object obj, @Nullable String name) {
        if (obj instanceof Config) {
            return parseObject(((Config) obj).valueMap(), name);
        } else if (obj instanceof Map) {
            final ArrayList<Element> subElements = new ArrayList<>();
            ((Map<?, ?>) obj).forEach((str, obj1) -> subElements.add(parseObject(obj1, (String) str)));
            return new Element(Element.Type.Section, null, name, subElements.toArray());
        } else if (obj instanceof List) {
            final ArrayList<Element> subElements = new ArrayList<>();
            ((List<?>) obj).forEach(e -> subElements.add(parseObject(e, null)));
            return new Element(Element.Type.Array, null, name, subElements.toArray());
        } else if (obj instanceof String) {
            return new Element(Element.Type.String, null, name, obj);
        } else if (obj instanceof Boolean) {
            return new Element(Element.Type.Boolean, null, name, obj.toString());
        } else {
            return new Element(Element.Type.Number, null, name, obj.toString());
        }
    }
    
    private static void newLine(int indentLevel, StringBuilder builder) {
        builder.append("\n");
        for (int i = 0; i < indentLevel; i++) {
            builder.append("    ");
        }
    }
    
    public static String parseElement(Element element) {
        StringBuilder builder = new StringBuilder();
        parseElement(new Element(element.type, element.comment, null, element.value), -1, builder, false, null);
        return builder.substring(2, builder.length() - 2);
    }
    
    private static void parseElement(Element element, int indentLevel, StringBuilder builder, boolean omitComments, @Nullable String currentSectionName) {
        if (!omitComments && element.comment != null) {
            if (!element.comment.isEmpty()) {
                String[] commentLines = element.comment.split("\n");
                newLine(indentLevel, builder);
                builder.append("#");
                for (String commentLine : commentLines) {
                    newLine(indentLevel, builder);
                    builder.append("# ");
                    builder.append(commentLine);
                }
                newLine(indentLevel, builder);
                builder.append("#");
            }
        }
        newLine(indentLevel, builder);
        switch (element.type) {
            case String: {
                builder.append(element.name).append(" = ");
                String value = element.asString();
                value = value.replace("\n", "\\n");
                value = value.replace("\r", "\\r");
                value = value.replace("\"", "\\\"");
                builder.append("\"");
                builder.append(value);
                builder.append("\"");
                break;
            }
            case Boolean:
            case Number: {
                builder.append(element.name).append(" = ");
                builder.append(element.asString());
                break;
            }
            case Array: {
                builder.append("# Arrays are not supported with TOML");
                newLine(indentLevel, builder);
                break;
            }
            case Section: {
                Element[] elements = element.asArray();
                Arrays.sort(elements, (a, b) -> {
                    if (a.type == b.type) {
                        return 0;
                    }
                    if (a.type == Element.Type.Section) {
                        return 1;
                    }
                    if (b.type == Element.Type.Section) {
                        return -1;
                    }
                    return 0;
                });
                String sectionname = null;
                if (element.name != null) {
                    sectionname = (currentSectionName == null ? "" : currentSectionName + ".") + element.name;
                    builder.append('[').append(sectionname).append(']');
                }
                for (Element value : elements) {
                    parseElement(value, indentLevel + 1, builder, omitComments, sectionname);
                    if (value.type == Element.Type.Section) {
                        builder.deleteCharAt(builder.length() - 1);
                    }
                }
                builder.deleteCharAt(builder.length() - 1);
                newLine(indentLevel, builder);
                break;
            }
        }
        builder.append('\n');
    }
}
