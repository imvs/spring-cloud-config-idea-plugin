package me.imvs.springcloudconfighelper.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class FileUtils {
    private final static String PROPERTIES_FILE_NAME = "spring-cloud-config-helper.properties";
    private final String basePath;

    public FileUtils(String basePath) {
        this.basePath = basePath;
    }

    public void writeYaml(Map<String, Object> map, String outFile) throws IOException {
        ObjectMapper theMapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        Files.writeString(getFilePath(outFile), theMapper.writeValueAsString(map));
    }

    private Path getFilePath(String relativePath) {
        File file = new File(basePath, relativePath);
        File parentDir = file.getParentFile();
        maikeDirIfNotExists(parentDir);
        return file.toPath();
    }

    private void maikeDirIfNotExists(File dir) {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Fail to make dir: " + dir.getPath());
            }
        }
    }

    private File getTempDir() {
        File build = getBuildDir();
        File tmp = new File(build, "tmp");
        maikeDirIfNotExists(tmp);
        return tmp;
    }

    private File getBuildDir() {
        File build = new File(basePath, "build");
        maikeDirIfNotExists(build);
        return build;
    }

    public File getPropertiesFile() {
        File tmp = getTempDir();
        return new File(tmp, PROPERTIES_FILE_NAME);
    }
}
