package me.imvs.springcloudconfighelper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import me.imvs.springcloudconfighelper.plugin.PluginBundle;
import me.imvs.springcloudconfighelper.plugin.PluginException;
import me.imvs.springcloudconfighelper.plugin.PluginHelper;
import me.imvs.springcloudconfighelper.plugin.model.PluginModel;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.StringJoiner;

public class FilesHelper {
    private final static YAMLFactory yamlFactory = new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);

    public static String getBaseDir(@NotNull Project project) {
        String basePath = project.getBasePath();
        if (basePath == null) {
            basePath = "";
        }
        return Path.of(basePath).toString();
    }

    public static VirtualFile getBaseDirVirtualFile(@NotNull Project project) {
        Path basePath = project.getBasePath() == null
                ? getFilePath(project, "")
                : getFilePath(project, project.getBasePath());
        return LocalFileSystem.getInstance().findFileByNioFile(basePath);
    }

    public static Path writeYaml(Object map, Path path, OpenOption... options) throws IOException {
        ObjectMapper theMapper = new ObjectMapper(yamlFactory);
        return Files.writeString(path, theMapper.writeValueAsString(map), options);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static Path reWriteYaml(Object object, Path path) throws IOException {
        return writeYaml(object, path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static File getDir(File path, boolean create, String child) {
        File result = child == null ? path : new File(path, child);
        return create ? maikeDirIfNotExists(result) : result;
    }

    public static Path getFilePath(Project project, String path) {
        if (new File(path).isAbsolute()) {
            return Path.of(path);
        } else {
            return Path.of(getBaseDir(project), path);
        }
    }

    public static String validateOutFile(Project project, PluginModel model) {
        File file = new File(getFilePath(project, model.getOutFile()).toUri());
        if (file.exists() && file.isDirectory()) {
            return Path.of(model.getOutFile(), model.getAppName() + ".yml").toString();
        } else if (model.getOutFile().endsWith(File.separator) || model.getOutFile().endsWith("/")) {
            return getDir(file, false, model.getAppName() + ".yml").toString();
        }
        return model.getOutFile();
    }

    public static String[] validateLocations(Project project, String locations) {
        if (locations.isBlank()) {
            return new String[]{getBaseDir(project)};
        }
        StringJoiner joiner = new StringJoiner(",\n");
        String[] result = Arrays
                .stream(PluginHelper.splitCommaSeparated(locations))
                .filter(location -> !location.isBlank())
                .map(location -> getFilePath(project, location).toUri())
                .peek(uri -> {
                    if (!new File(uri).exists()) {
                        joiner.add(uri.getPath());
                    }
                })
                .map(URI::getPath)
                .toArray(String[]::new);
        if (joiner.length() > 0) {
            throw new PluginException(PluginBundle.message("message.error.expected.locations.not.found"),
                    new FileNotFoundException(joiner.toString())
            );
        }
        return result;
    }

    private static File maikeDirIfNotExists(File dir) {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Failed to make dir: " + dir.getPath());
            }
        }
        return dir;
    }
}
