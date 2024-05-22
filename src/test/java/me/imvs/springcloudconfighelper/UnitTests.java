package me.imvs.springcloudconfighelper;

import me.imvs.springcloudconfighelper.plugin.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UnitTests {

    String basePath;

    @BeforeAll
    public void init() throws URISyntaxException {
        URI uri = Objects.requireNonNull(this.getClass().getClassLoader().getResource("file")).toURI();
        File file = new File(uri);
        Assertions.assertTrue(file.exists());
        Assertions.assertTrue(file.isFile());
        basePath = file.getParentFile().toURI().getPath();
    }

    @Test
    public void loadPropertyFileTest() throws IOException {
        FileUtils fileUtils = new FileUtils(basePath);
        File propertiesFile = fileUtils.getPropertiesFile();
        Assertions.assertTrue(propertiesFile.exists());
        Assertions.assertTrue(propertiesFile.isFile());
        Properties properties = new Properties();
        properties.load(new FileInputStream(propertiesFile));

        Assertions.assertEquals(4, properties.size());
        Assertions.assertEquals("test", properties.getProperty("app-name"));
        Assertions.assertEquals("default , special", properties.getProperty("profiles"));
        Assertions.assertEquals("profiles,specials", properties.getProperty("locations"));
        Assertions.assertEquals("temp-single.yml", properties.getProperty("out-yaml"));
    }

    @Test
    public void mergeTest() throws IOException, URISyntaxException {
        MergeProfiles mergeProfiles = new MergeProfiles();
        String[] locations = new String[]{
                new File(basePath, "default").toURI().getPath(),
                new File(basePath, "specials").toURI().getPath()
        };
        Map<String, Object> map = mergeProfiles.merge(locations, "test", "default , special");
        FileUtils fileUtils = new FileUtils(basePath);
        fileUtils.writeYaml(map, "temp-single.yml");
        Path path = Path.of(Objects.requireNonNull(this.getClass().getClassLoader().getResource("temp-single.yml")).toURI());
        String ymlString = Files.readString(path);
        Assertions.assertEquals("""
                test:
                  list: "singleton"
                  dict:
                    foo: "bar"
                    add: "special"
                """, ymlString);
    }
}
