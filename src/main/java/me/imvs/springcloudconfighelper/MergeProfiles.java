package me.imvs.springcloudconfighelper;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.NativeEnvironmentProperties;
import org.springframework.cloud.config.server.environment.NativeEnvironmentRepository;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

import java.util.LinkedHashMap;
import java.util.Map;

public class MergeProfiles {

    public Map<String, Object> merge(String[] locations, String application, String profile) {
        return merge(locations, Ordered.LOWEST_PRECEDENCE, application, profile);
    }

    public Map<String, Object> merge(String[] locations, int order, String application, String profile) {
        Map<String, Object> result = new LinkedHashMap<>();
        NativeEnvironmentRepository repository = getRepository(getProperties(locations, order));
        for (PropertySource propertySource : repository.findOne(application, profile, null).getPropertySources()) {
            //noinspection unchecked
            Map<String, Object> unFlatten = YamlUtils.unFlat((Map<String, Object>) propertySource.getSource());
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

    private Map<String, Object> combine(Object oldValue, Map<String, Object> map) {
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
}
