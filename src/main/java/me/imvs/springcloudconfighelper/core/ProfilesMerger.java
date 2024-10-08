package me.imvs.springcloudconfighelper.core;

import io.micrometer.observation.ObservationRegistry;
import me.imvs.springcloudconfighelper.DupplicatedPropertySourcesException;
import me.imvs.springcloudconfighelper.ProfileNotFoundException;
import me.imvs.springcloudconfighelper.PropertySourceEmptyException;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.NativeEnvironmentProperties;
import org.springframework.cloud.config.server.environment.NativeEnvironmentRepository;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

import java.net.URI;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ProfilesMerger {
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
                        PropertySource::getSource,
                        (map, map2) -> {
                            throw new DupplicatedPropertySourcesException();
                        },
                        LinkedHashMap::new)
                );
        if (propertySources.isEmpty()) {
            throw new PropertySourceEmptyException();
        }
        for (String p : profile.split("\\s*,\\s*")) {
            Map<?, ?> properties = propertySources.get(p);
            if (properties == null) {
                throw new ProfileNotFoundException(p);
            }
            Map<String, Object> map = PropertiesParser.toMap(properties);
            result = merge(result, map);
        }
        return result;
    }

    public NativeEnvironmentProperties getProperties(String[] locations, int order) {
        NativeEnvironmentProperties properties = new NativeEnvironmentProperties();
        properties.setSearchLocations(locations);
        properties.setOrder(order);
        return properties;
    }

    public NativeEnvironmentRepository getRepository(NativeEnvironmentProperties properties) {
        ConfigurableEnvironment environment = new StandardEnvironment();
        return new NativeEnvironmentRepository(environment, properties, ObservationRegistry.NOOP);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, Object> merge(Object first, Map<String, Object> next) {
        if (first instanceof Map firstMap) {
            for (Map.Entry<String, Object> entry : next.entrySet()) {
                Object value = firstMap.get(entry.getKey());
                Object object = entry.getValue();
                if (value == null) {
                    firstMap.put(entry.getKey(), object);
                    continue;
                }
                if (object instanceof Map map) {
                    firstMap.put(entry.getKey(), merge(value, map));
                } else {
                    firstMap.put(entry.getKey(), object);
                }
            }
            return firstMap;
        } else {
            return new LinkedHashMap<>(next);
        }
    }
}
