package me.imvs.springcloudconfighelper;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.server.environment.NativeEnvironmentProperties;
import org.springframework.cloud.config.server.environment.NativeEnvironmentRepository;
import org.springframework.core.Ordered;

import java.io.File;
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
public class UnitTests {

    String application = "test";
    String[] profiles = new String[]{"default", "special"};
    String[] locations = new String[]{
            Objects.requireNonNull(this.getClass().getClassLoader().getResource("profiles")).toURI().getPath(),
            Objects.requireNonNull(this.getClass().getClassLoader().getResource("specials")).toURI().getPath()
    };

    public UnitTests() throws URISyntaxException {
    }

    @Test
    public void loadPropertyFileSuccess() throws IOException {
        FileSupport fileSupport = new FileSupport();
        File propertiesFile = fileSupport.getPropertiesFile("build");
        boolean ignored = propertiesFile.createNewFile();
        Assertions.assertTrue(propertiesFile.exists());
        Assertions.assertTrue(propertiesFile.isFile());
        Assertions.assertEquals("spring-cloud-config-helper.properties", propertiesFile.getName());
        Assertions.assertTrue(propertiesFile.getParentFile().toURI().normalize().toString().endsWith("build/tmp/"));
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
        Assertions.assertEquals("{test.list[0]=first, test.list[1]=second, test.dict.default=0, test.dict.foo=bar}",source.get(0).toString());
        Assertions.assertEquals("{test.list=string, test.dict.foo=overridden, test.dict.add=more}",source.get(1).toString());
        source = map(profiles, Ordered.HIGHEST_PRECEDENCE);
        Assertions.assertEquals(2, source.size());
        Assertions.assertEquals("{test.list[0]=first, test.list[1]=second, test.dict.default=0, test.dict.foo=bar}",source.get(0).toString());
        Assertions.assertEquals("{test.list=string, test.dict.foo=overridden, test.dict.add=more}",source.get(1).toString());
    }

    @Test
    public void unFlatDefaultPropertiesSuccess() {
        CollectionsMerger collectionsMerger = new CollectionsMerger();
        Map<String, Object> map = collectionsMerger.unFlat(map(profiles[0], Ordered.LOWEST_PRECEDENCE));
        log.info("Application: {}, profile: {}\n{}", application, profiles[0], map);
        //noinspection unchecked
        Map<String, Object> dict = (Map<String, Object>) ((Map<String, Object>) map.get("test")).get("dict");
        Assertions.assertEquals("bar", dict.get("foo"));
        Assertions.assertEquals(0, dict.get("default"));
        //noinspection unchecked
        List<String> list = (List<String>) ((Map<String, Object>) map.get("test")).get("list");
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals("first", list.get(0));
        Assertions.assertEquals("second", list.get(1));
    }

    @Test
    public void unFlatSpecialPropertiesSuccess() {
        CollectionsMerger collectionsMerger = new CollectionsMerger();
        Map<String, Object> map = collectionsMerger.unFlat(map(profiles[1], Ordered.LOWEST_PRECEDENCE));
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
    public void mergeSpecialThanDefaultSuccess() throws IOException, URISyntaxException {
        CollectionsMerger collectionsMerger = new CollectionsMerger();
        String[] locations = new String[]{
                Objects.requireNonNull(this.getClass().getClassLoader().getResource("profiles")).toURI().getPath(),
                Objects.requireNonNull(this.getClass().getClassLoader().getResource("specials")).toURI().getPath()
        };
        Map<String, Object> map = collectionsMerger.merge(locations, application, "special,default");
        FileSupport fileSupport = new FileSupport();
        Path path = fileSupport.writeYaml(map, "build/tmp/temp-single.yml");
        String ymlString = Files.readString(path);
        log.info("Application: {}, profile: {}\n{}", application, "special,default", map);
        Assertions.assertEquals("""
                test:
                  list:
                  - "first"
                  - "second"
                  dict:
                    foo: "bar"
                    add: "more"
                    default: 0
                """, ymlString);
    }
    @Test
    public void mergeDefaultThanSpecialSuccess() throws IOException, URISyntaxException {
        CollectionsMerger collectionsMerger = new CollectionsMerger();
        String[] locations = new String[]{
                Objects.requireNonNull(this.getClass().getClassLoader().getResource("profiles")).toURI().getPath(),
                Objects.requireNonNull(this.getClass().getClassLoader().getResource("specials")).toURI().getPath()
        };
        Map<String, Object> map = collectionsMerger.merge(locations, application, "Default,Special");
        FileSupport fileSupport = new FileSupport();
        Path path = fileSupport.writeYaml(map, "build/tmp/temp-single.yml");
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
        CollectionsMerger collectionsMerger = new CollectionsMerger();
        NativeEnvironmentProperties properties = collectionsMerger.getProperties(locations, order);
        NativeEnvironmentRepository repository = collectionsMerger.getRepository(properties);
        Environment one = repository.findOne(application, profile, null);
        Assertions.assertEquals(1, one.getPropertySources().size());
        //noinspection unchecked
        return (Map<String, Object>) one.getPropertySources().get(0).getSource();
    }
}
