package me.imvs.springcloudconfighelper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class FileSupport {
    private final static String PROPERTIES_FILE_NAME = "spring-cloud-config-helper.properties";
    private final static String DEFAULT_TEMP_DIR_NAME = "tmp";
    private final File baseDir;

    public FileSupport(String basePath) {
        baseDir = new File(basePath);
    }

    public FileSupport() {
        this(".");
    }

    public Path writeYaml(Map<String, Object> map, String outFile) throws IOException {
        ObjectMapper theMapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        return Files.writeString(getFilePath(outFile), theMapper.writeValueAsString(map));
    }

    public File getPropertiesFile(String buildDir) {
        File workDir;
        URI uri = URI.create(buildDir);
        if (uri.isAbsolute()) {
            File buildDirFile = new File(uri);
            workDir = getDirs(buildDirFile, true, DEFAULT_TEMP_DIR_NAME);
        } else {
            workDir = getDirs(baseDir, true, buildDir, DEFAULT_TEMP_DIR_NAME);
        }
        return new File(workDir, PROPERTIES_FILE_NAME);
    }

    public File getDir(File path, boolean create, String child) {
        File result = new File(path, child);
        return create ? maikeDirIfNotExists(result) : result;
    }

    public File getDirs(File path, boolean create, String... children) {
        if (children.length == 0) {
            return path;
        }
        File result = path;
        for (String child : children) {
            result = getDir(result, create, child);
        }
        return create ? maikeDirIfNotExists(result) : result;
    }

    private Path getFilePath(String relativePath) {
        URI uri = URI.create(relativePath);
        File file = uri.isAbsolute()
                ? new File(uri)
                : new File(baseDir, uri.getPath());
        File parentDir = file.getParentFile();
        maikeDirIfNotExists(parentDir);
        return file.toPath();
    }

    private File maikeDirIfNotExists(File dir) {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Failed to make dir: " + dir.getPath());
            }
        }
        return dir;
    }
}
