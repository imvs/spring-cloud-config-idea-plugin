package me.imvs.springcloudconfighelper;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.NativeEnvironmentProperties;
import org.springframework.cloud.config.server.environment.NativeEnvironmentRepository;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class CollectionsMerger {
    static final String PROFILE_SPLIT_REGEX = "\\s*,\\s*";
    static final String PROFILE_TOKEN_REGEX = "-(.*)\\.\\w+";

    public Map<String, Object> merge(String[] locations, String application, String profile) {
        return merge(locations, Ordered.LOWEST_PRECEDENCE, application, profile);
    }

    public Map<String, Object> merge(String[] locations, int order, String application, String profile) {
        Map<String, Object> result = new LinkedHashMap<>();
        NativeEnvironmentRepository repository = getRepository(getProperties(locations, order));
        Map<String, ? extends Map<?, ?>> propertySources = repository
                .findOne(application, profile, null)
                .getPropertySources()
                .stream().collect(Collectors.toMap(
                        propertySource -> Path.of(URI.create(propertySource.getName()))
                                .getFileName()
                                .toString()
                                .replaceAll(application + PROFILE_TOKEN_REGEX, "$1"),
                        PropertySource::getSource)
                );
        if (propertySources.isEmpty()) {
            throw new IllegalStateException("property sources are empty");
        }
        for (String p : profile.split(PROFILE_SPLIT_REGEX)) {
            Map<?, ?> map = propertySources.get(p);
            if (map == null) {
                throw new IllegalStateException("No property source for profile: " + p);
            }
            Map<String, Object> unFlatten = unFlat(map);
            result = combine(result, unFlatten);
        }
        return result;
    }

    NativeEnvironmentProperties getProperties(String[] locations, int order) {
        NativeEnvironmentProperties properties = new NativeEnvironmentProperties();
        properties.setSearchLocations(locations);
        properties.setOrder(order);
        return properties;
    }

    NativeEnvironmentRepository getRepository(NativeEnvironmentProperties properties) {
        ConfigurableEnvironment environment = new StandardEnvironment();
        return new NativeEnvironmentRepository(environment, properties, ObservationRegistry.NOOP);
    }

    Map<String, Object> combine(Object oldValue, Map<String, Object> map) {
        if (oldValue instanceof Map) {
            //noinspection unchecked
            Map<String, Object> oldMap = (Map<String, Object>) oldValue;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Object value = oldMap.get(entry.getKey());
                if (value == null) {
                    oldMap.put(entry.getKey(), entry.getValue());
                    continue;
                }
                if (entry.getValue() instanceof Map) {
                    //noinspection unchecked
                    oldMap.put(entry.getKey(), combine(value, (Map<String, Object>) entry.getValue()));
                } else {
                    oldMap.put(entry.getKey(), entry.getValue());
                }
            }
            return oldMap;
        } else {
            return new LinkedHashMap<>(map);
        }
    }

    Map<String, Object> unFlat(Map<?, ?> flatMap) {
        return flatMap.keySet()
                .stream()
                .map(s -> {
                    if (s instanceof String str) {
                        LinkedList<String> list = new LinkedList<>(Arrays.asList(str.split("\\.")));
                        list.add(str);
                        return list;
                    }
                    if (s == null) {
                        return null;
                    }
                    throw new IllegalArgumentException("Bad key type: " + s.getClass().getName());
                })
                .filter(Objects::nonNull)
                .reduce(new LinkedHashMap<>(), (map, keys) -> accumulator(map, keys, flatMap), this::mergeMaps);
    }

    LinkedHashMap<String, Object> accumulator(LinkedHashMap<String, Object> map, LinkedList<String> keys, Map<?, ?> flatMap) {
        final Object[] keyAndIndex = getKeyAndIndex(keys.remove());
        LinkedHashMap<String, Object> value = valueMapper(keyAndIndex, keys, flatMap);
        return mergeMaps(map, value);
    }

    Object merge(Object o, Object o2) {
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

    boolean bothEntries(Object o, Object o2) {
        return o instanceof Map.Entry && o2 instanceof Map.Entry;
    }

    boolean anyEntries(Object o, Object o2) {
        return o instanceof Map.Entry || o2 instanceof Map.Entry;
    }

    boolean bothMaps(Object o, Object o2) {
        return o instanceof Map && o2 instanceof Map;
    }

    @SuppressWarnings("unchecked")
    Object addEntryToList(Object o1, Object o2) {
        if (!(o2 instanceof Map.Entry)) {
            return o2;
        }
        //noinspection rawtypes
        if (o1 instanceof ArrayList list) {
            //noinspection unchecked
            Map.Entry<Integer, ?> entry = (Map.Entry<Integer, ?>) o2;
            int index = entry.getKey();
            Object value = entry.getValue();
            list.ensureCapacity(index + 1);
            for (int i = 0; i < index + 1; i++) {
                if (i == index) {
                    if (list.size() < i + 1) {
                        list.set(i, value);
                        continue;
                    }
                    merge(list.get(i), value);
                }
                if (list.size() < i + 1) {
                    list.set(i, null);
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

    Object mergeEntriesToList(Map.Entry<Integer, Object> o1, Map.Entry<Integer, Object> o2) {
        int index1 = o1.getKey();
        int index2 = o2.getKey();
        int maxSize = Math.max(index1, index2) + 1;
        Object[] result = new Object[maxSize];
        for (int i = 0; i < maxSize; i++) {
            if (index1 == i && index2 == i) {
                result[i] = merge(o1.getValue(), o2.getValue());
            } else {
                if (i == index1) {
                    result[i] = o1.getValue();
                } else if (i == index2) {
                    result[i] = o2.getValue();
                } else {
                    result[i] = null;
                }
            }
        }
        return new ArrayList<>(Arrays.asList(result));
    }

    LinkedHashMap<String, Object> mergeMaps(LinkedHashMap<String, Object> o, LinkedHashMap<String, Object> o2) {
        o2.forEach((key, value) -> o.put(key, merge(o.get(key), value)));
        return o;
    }

    Object[] getKeyAndIndex(String originKey) {
        boolean isList = originKey.endsWith("]");
        if (isList) {
            int index = originKey.indexOf("[");
            String key = originKey.substring(0, index);
            int listIndex = Integer.parseInt(originKey.substring(index + 1, originKey.indexOf("]")));
            return new Object[]{key, listIndex};
        }
        return new Object[]{originKey};
    }

    LinkedHashMap<String, Object> valueMapper(Object[] keyAndIndex, LinkedList<String> strings, Map<?, ?> flatMap) {
        String key = (String) keyAndIndex[0];
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        Object value;
        if (strings.size() == 1) {
            value = wrapValue(keyAndIndex, flatMap.get(strings.remove()));
        } else {
            value = wrapValue(keyAndIndex, valueMapper(getKeyAndIndex(strings.remove()), strings, flatMap));
        }
        result.put(key, value);
        return result;
    }

    Object wrapValue(Object[] keyAndIndex, Object value) {
        if (keyAndIndex.length == 1) {
            return value;
        }
        int listIndex = (int) keyAndIndex[1];
        return Map.entry(listIndex, value);
    }
}
