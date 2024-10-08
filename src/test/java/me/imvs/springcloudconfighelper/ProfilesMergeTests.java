package me.imvs.springcloudconfighelper;

import lombok.extern.slf4j.Slf4j;
import me.imvs.springcloudconfighelper.core.ProfilesMerger;
import me.imvs.springcloudconfighelper.core.PropertiesParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.server.environment.NativeEnvironmentProperties;
import org.springframework.cloud.config.server.environment.NativeEnvironmentRepository;
import org.springframework.core.Ordered;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProfilesMergeTests {

    String application = "test";
    String[] profiles = new String[]{"default", "special"};
    String[] locations = new String[]{
            Objects.requireNonNull(this.getClass().getClassLoader().getResource("profiles")).toURI().getPath(),
            Objects.requireNonNull(this.getClass().getClassLoader().getResource("specials")).toURI().getPath()
    };

    public ProfilesMergeTests() throws URISyntaxException {
    }

    @Test
    public void readDefaultPropertiesSuccess() {
        Map<?, ?> source = map(profiles[0], Ordered.LOWEST_PRECEDENCE);
        log.info("Application: {}, profile: {}\n{}", application, profiles[0], source);
        Assertions.assertEquals("first", source.get("test.list[0]"));
        Assertions.assertEquals("second", source.get("test.list[1]"));
        Assertions.assertEquals("bar", source.get("test.dict.foo"));
        Assertions.assertEquals(0, source.get("test.dict.default"));
    }

    @Test
    public void readSpecialPropertiesSuccess() {
        Map<?, ?> source = map(profiles[1], Ordered.LOWEST_PRECEDENCE);
        log.info("Application: {}, profile: {}\n{}", application, profiles[1], source);
        Assertions.assertEquals("string", source.get("test.list"));
        Assertions.assertEquals("overridden", source.get("test.dict.foo"));
        Assertions.assertEquals("more", source.get("test.dict.add"));
    }

    @Test
    public void readBothPropertiesSuccess() {
        List<Map<String, Object>> source = map(profiles, Ordered.LOWEST_PRECEDENCE);
        Assertions.assertEquals(2, source.size());
        Assertions.assertEquals("{test.list[0]=first, test.list[1]=second, test.list[2]=third, test.dict.default=0, test.dict.foo=bar}", source.get(0).toString());
        Assertions.assertEquals("{test.list=string, test.dict.foo=overridden, test.dict.add=more}", source.get(1).toString());
        source = map(profiles, Ordered.HIGHEST_PRECEDENCE);
        Assertions.assertEquals(2, source.size());
        Assertions.assertEquals("{test.list[0]=first, test.list[1]=second, test.list[2]=third, test.dict.default=0, test.dict.foo=bar}", source.get(0).toString());
        Assertions.assertEquals("{test.list=string, test.dict.foo=overridden, test.dict.add=more}", source.get(1).toString());
    }

    @Test
    public void unFlatDefaultPropertiesSuccess() {
        Map<String, Object> map = PropertiesParser.toMap(map(profiles[0], Ordered.LOWEST_PRECEDENCE));
        log.info("Application: {}, profile: {}\n{}", application, profiles[0], map);
        //noinspection unchecked
        Map<String, Object> dict = (Map<String, Object>) ((Map<String, Object>) map.get("test")).get("dict");
        Assertions.assertEquals("bar", dict.get("foo"));
        Assertions.assertEquals(0, dict.get("default"));
        //noinspection unchecked
        List<String> list = (List<String>) ((Map<String, Object>) map.get("test")).get("list");
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("first", list.get(0));
        Assertions.assertEquals("second", list.get(1));
        Assertions.assertEquals("third", list.get(2));
    }

    @Test
    public void unFlatSpecialPropertiesSuccess() {
        Map<String, Object> map = PropertiesParser.toMap(map(profiles[1], Ordered.LOWEST_PRECEDENCE));
        log.info("Application: {}, profile: {}\n{}", application, profiles[1], map);
        //noinspection unchecked
        Map<String, Object> dict = (Map<String, Object>) ((Map<String, Object>) map.get("test")).get("dict");
        Assertions.assertEquals("overridden", dict.get("foo"));
        Assertions.assertEquals("more", dict.get("add"));
        //noinspection unchecked
        String list = (String) ((Map<String, Object>) map.get("test")).get("list");
        Assertions.assertEquals("string", list);
    }

    @Test
    public void mergeSpecialThanDefaultSuccess() throws IOException {
        ProfilesMerger profilesMerger = new ProfilesMerger();
        Map<String, Object> map = profilesMerger.merge(locations, application, "special,default");
        Path path = FilesHelper.writeYaml(map, Path.of("build/tmp/temp-single.yml"));
        String ymlString = Files.readString(path);
        log.info("Application: {}, profile: {}\n{}", application, "special,default", map);
        Assertions.assertEquals("""
                test:
                  list:
                  - "first"
                  - "second"
                  - "third"
                  dict:
                    foo: "bar"
                    add: "more"
                    default: 0
                """, ymlString);
    }

    @Test
    public void mergeDefaultThanSpecialSuccess() throws IOException {
        ProfilesMerger profilesMerger = new ProfilesMerger();
        Map<String, Object> map = profilesMerger.merge(locations, application, "Default,Special");
        Path path = FilesHelper.writeYaml(map, Path.of("build/tmp/temp-single.yml"));
        String ymlString = Files.readString(path);
        log.info("Application: {}, profile: {}\n{}", application, "special,default", map);
        Assertions.assertEquals("""
                test:
                  list: "string"
                  dict:
                    default: 0
                    foo: "overridden"
                    add: "more"
                 """, ymlString);
    }

    private List<Map<String, Object>> map(String[] profiles, int order) {
        return Arrays.stream(profiles)
                .map(p -> map(p, order))
                .collect(Collectors.toList());
    }

    private Map<String, Object> map(String profile, int order) {
        ProfilesMerger profilesMerger = new ProfilesMerger();
        NativeEnvironmentProperties properties = profilesMerger.getProperties(locations, order);
        NativeEnvironmentRepository repository = profilesMerger.getRepository(properties);
        Environment one = repository.findOne(application, profile, null);
        Assertions.assertEquals(1, one.getPropertySources().size());
        //noinspection unchecked
        return (Map<String, Object>) one.getPropertySources().get(0).getSource();
    }
}
