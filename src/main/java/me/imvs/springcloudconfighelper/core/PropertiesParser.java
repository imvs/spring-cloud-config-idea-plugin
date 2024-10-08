package me.imvs.springcloudconfighelper.core;


import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;

import static java.util.stream.Collectors.*;

public class PropertiesParser {

    static String LIST_PATTERN_END = "]";
    static String LIST_PATTERN_FULL_GROUP = "full";
    static String LIST_PATTERN_INDEX_GROUP = "index";
    static String PROPERTY_DELIMITER = "\\.";
    static Pattern listPattern = Pattern.compile("(?<full>\\[(?<index>[0-9]+)])");

    public static Map<String, Object> toMap(Map<?, ?> properties) {
        List<Element> list = properties.entrySet().stream()
                .map(PropertiesParser::parseProperty).toList();
        //noinspection unchecked
        return (Map<String, Object>) toMap(list);
    }

    public static <T> T toObject(Map<?, ?> properties, Class<T> type) {
        Object map = toMap(properties);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(map, type);
    }

    public static Map<String, Object> toMap(Properties properties) {
        List<Element> list = properties.entrySet().stream()
                .map(PropertiesParser::parseProperty).toList();
        //noinspection unchecked
        return (Map<String, Object>) toMap(list);
    }

    public static <T> T toObject(Properties properties, Class<T> type) {
        Object map = toMap(properties);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(map, type);
    }

    private static Object toMap(List<Element> elements) {
        if (elements == null || elements.isEmpty()) {
            return null;
        }
        if (elements.get(0).type.equals(ElementType.dict)) {
            return elements
                    .stream()
                    .collect(groupByKey());
        }
        if (elements.get(0).type.equals(ElementType.list)) {
            Collection<Object> collected = elements
                    .stream()
                    .sorted(Comparator.comparing((Element o) -> o.key))
                    .collect(groupByKey()).values();
            return new LinkedList<>(collected);
        }
        if (elements.size() == 1) {
            return elements.get(0).value();
        }
        return elements.stream().map(Element::value).toList();
    }

    static Collector<Element, ?, LinkedHashMap<String, Object>> groupByKey() {
        return groupingBy(
                Element::key,
                LinkedHashMap::new,
                collectingAndThen(
                        toList(),
                        list -> {
                            List<Element> mapped = list
                                    .stream()
                                    .map(e -> {
                                        if (e.child() != null) {
                                            return e.child();
                                        }
                                        return e;
                                    }).toList();
                            return toMap(mapped);
                        }
                )
        );
    }

    private static Element parseProperty(Map.Entry<?, ?> property) {
        String[] split = property.getKey().toString().split(PROPERTY_DELIMITER, 2);
        String key = split[0];
        if (split.length == 1) {
            Element child = new Element(property.getValue());
            if (isList(key)) {
                return parseList(key, child);
            } else {
                return new Element(key, child, ElementType.dict);
            }
        } else {
            Element child = parseProperty(Map.entry(split[1], property.getValue()));
            if (isList(key)) {
                return parseList(key, child);
            } else {
                return new Element(key, child, ElementType.dict);
            }
        }
    }

    private static Element parseList(String key, Element child) {
        Matcher matcher = listPattern.matcher(key);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Bad property: " + key);
        }
        String full = matcher.group(LIST_PATTERN_FULL_GROUP);
        String index = matcher.group(LIST_PATTERN_INDEX_GROUP);
        key = key.replace(full, "");
        return new Element(key, new Element(index, child, ElementType.list), ElementType.dict);
    }

    private static boolean isList(String key) {
        return key.endsWith(LIST_PATTERN_END);
    }

    record Element(String key, Object value, Element child,
                   ElementType type) {
        public Element(String key, Element child, ElementType type) {
            this(key, null, child, type);
        }

        public Element(Object value) {
            this(null, value, null, ElementType.object);
        }
    }

    enum ElementType {
        dict,
        list,
        object
    }
}
