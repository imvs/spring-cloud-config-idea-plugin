package me.imvs.springcloudconfighelper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.NativeEnvironmentProperties;
import org.springframework.cloud.config.server.environment.NativeEnvironmentRepository;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

import java.util.LinkedHashMap;
import java.util.Map;

public class MergeProfiles {

    ObjectMapper theMapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));

    public String getYaml(String[] locations, int order, String application, String profile) throws JsonProcessingException {
        Map<String, Object> result = new LinkedHashMap<>();
        NativeEnvironmentRepository repository = getRepository(getProperties(locations, order));
        for (PropertySource propertySource : repository.findOne(application, profile, null).getPropertySources()) {
            //noinspection unchecked
            Map<String, Object> unFlatten = YamlUtils.unFlat((Map<String, Object>) propertySource.getSource());
            result = combine(result, unFlatten);
        }
        return theMapper.writeValueAsString(result);
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
