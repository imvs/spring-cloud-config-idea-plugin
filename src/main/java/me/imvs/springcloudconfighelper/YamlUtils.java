package me.imvs.springcloudconfighelper;

import java.util.*;

public class YamlUtils {

    public static Map<String, Object> unFlat(Map<String, Object> flatMap) {
        return flatMap.keySet()
                .stream()
                .map(s -> {
                    LinkedList<String> list = new LinkedList<>(Arrays.asList(s.split("\\.")));
                    list.add(s);
                    return list;
                })
                .reduce(new LinkedHashMap<>(), (map, keys) -> accumulator(map, keys, flatMap), YamlUtils::mergeMaps);
    }

    private static LinkedHashMap<String, Object> accumulator(LinkedHashMap<String, Object> map, LinkedList<String> keys, Map<String, Object> flatMap) {
        final Object[] keyAndIndex = getKeyAndIndex(keys.remove());
        LinkedHashMap<String, Object> value = valueMapper(keyAndIndex, keys, flatMap);
        return mergeMaps(map, value);
    }

    private static Object merge(Object o, Object o2) {
        if (bothMaps(o, o2)) {
            //noinspection unchecked
            return mergeMaps((LinkedHashMap<String, Object>) o, (LinkedHashMap<String, Object>) o2);
        }
        if (bothEntries(o, o2)) {
            //noinspection unchecked
            return mergeEntriesToList((Map.Entry<Integer, Object>) o, (Map.Entry<Integer, Object>) o2);
        }
        if (anyEntries(o, o2)) {
            return addEntryToList(o, o2);
        }
        return o2;
    }

    private static Object addEntryToList(Object o1, Object o2) {
        if (!(o2 instanceof Map.Entry)) {
            return o2;
        }
        if (o1 instanceof ArrayList) {
            //noinspection unchecked
            Map.Entry<Integer, Object> entry = (Map.Entry<Integer, Object>) o2;
            int index = entry.getKey();
            //noinspection unchecked
            ArrayList<Object> list = (ArrayList<Object>) o1;
            list.ensureCapacity(index + 1);
            for (int i = 0; i < index + 1; i++) {
                if (i == index) {
                    if (list.size() < i + 1) {
                        list.add(i, entry.getValue());
                        continue;
                    }
                    merge(list.get(i), entry.getValue());
                }
                if (list.size() < i + 1) {
                    list.add(i, null);
                }
            }
            return list;
        }
        //noinspection unchecked
        Map.Entry<Integer, Object> entry = (Map.Entry<Integer, Object>) o2;
        int index = entry.getKey();
        ArrayList<Object> list = new ArrayList<>(index + 1);
        for (int i = 0; i < index + 1; i++) {
            if (i == index) {
                list.add(i, entry.getValue());
                continue;
            }
            list.add(i, null);
        }
        return list;
    }

    private static Object mergeEntriesToList(Map.Entry<Integer, Object> o1, Map.Entry<Integer, Object> o2) {
        int index1 = o1.getKey();
        int index2 = o2.getKey();
        int maxSize = Math.max(index1, index2) + 1;
        ArrayList<Object> result = new ArrayList<>(maxSize);
        for (int i = 0; i < maxSize; i++) {
            if (index1 == i && index2 == i) {
                result.add(i, merge(o1.getValue(), o2.getValue()));
            } else {
                if (i == index1) {
                    result.add(i, o1.getValue());
                }
                if (i == index2) {
                    result.add(i, o2.getValue());
                }
                result.add(i, null);
            }
        }
        return result;
    }

    private static boolean bothEntries(Object o, Object o2) {
        return o instanceof Map.Entry && o2 instanceof Map.Entry;
    }

    private static boolean anyEntries(Object o, Object o2) {
        return o instanceof Map.Entry || o2 instanceof Map.Entry;
    }

    private static boolean bothMaps(Object o, Object o2) {
        return o instanceof Map && o2 instanceof Map;
    }

    private static LinkedHashMap<String, Object> mergeMaps(LinkedHashMap<String, Object> o, LinkedHashMap<String, Object> o2) {
        o2.forEach((key, value) -> o.put(key, merge(o.get(key), value)));
        return o;
    }

    private static Object[] getKeyAndIndex(String originKey) {
        boolean isList = originKey.endsWith("]");
        if (isList) {
            int index = originKey.indexOf("[");
            String key = originKey.substring(0, index);
            int listIndex = Integer.parseInt(originKey.substring(index + 1, originKey.indexOf("]")));
            return new Object[]{key, listIndex};
        }
        return new Object[]{originKey};
    }

    private static LinkedHashMap<String, Object> valueMapper(Object[] keyAndIndex, LinkedList<String> strings, Map<String, Object> flatMap) {
        String key = (String) keyAndIndex[0];
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        if (strings.size() == 1) {
            Object value = wrapValue(keyAndIndex, flatMap.get(strings.remove()));
            result.put(key, value);
        } else {
            Object value = wrapValue(keyAndIndex, valueMapper(getKeyAndIndex(strings.remove()), strings, flatMap));
            result.put(key, value);
        }
        return result;
    }

    private static Object wrapValue(Object[] keyAndIndex, Object value) {
        if (keyAndIndex.length == 1) {
            return value;
        }
        int listIndex = (int) keyAndIndex[1];
        return Map.entry(listIndex, value);
    }
}
