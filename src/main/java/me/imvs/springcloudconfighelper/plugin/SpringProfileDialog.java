package me.imvs.springcloudconfighelper.plugin;

import com.intellij.openapi.project.Project;
import me.imvs.springcloudconfighelper.MergeProfiles;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

public class SpringProfileDialog extends JDialog {

    private static final String APP_NAME_PROPERTY = "app-name";
    private static final String PROFILES_PROPERTY = "profiles";
    private static final String LOCATIONS_PROPERTY = "locations";
    private static final String OUT_YAML_PROPERTY = "out-yaml";
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField appNameField;
    private JTextField profilesField;
    private JTextField locationsFiled;
    private JTextField outYamlField;
    private final Project project;
    private final FileUtils fileUtils;
    private Properties properties;

    public SpringProfileDialog(Project project) throws IOException {
        this.project = project;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(e -> {
            try {
                onOK();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        buttonCancel.addActionListener(e -> onCancel());

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(e -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        fileUtils = new FileUtils(getBaseDir());
        loadProperties();
    }

    private void onOK() throws IOException {
        String[] locations = getLocations();
        MergeProfiles mergeProfiles = new MergeProfiles();
        Map<String, Object> mergeResult = mergeProfiles.merge(locations, appNameField.getText(), profilesField.getText());
        fileUtils.writeYaml(mergeResult, outYamlField.getText());
        updateProperties();
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    private void updateProperties() throws IOException {
        properties.setProperty(APP_NAME_PROPERTY, appNameField.getText());
        properties.setProperty(PROFILES_PROPERTY, profilesField.getText());
        properties.setProperty(LOCATIONS_PROPERTY, locationsFiled.getText());
        properties.setProperty(OUT_YAML_PROPERTY, outYamlField.getText());
        try (FileOutputStream fos = new FileOutputStream(fileUtils.getPropertiesFile())) {
            properties.store(fos, null);
        }
    }

    private void loadProperties() throws IOException {
        properties = new Properties();
        File propertiesFile = fileUtils.getPropertiesFile();
        if (propertiesFile.exists()) {
            try (FileInputStream fis = new FileInputStream(propertiesFile)) {
                properties.load(fis);
            }
            if (properties.containsKey(APP_NAME_PROPERTY)) {
                appNameField.setText(properties.getProperty(APP_NAME_PROPERTY));
            }
            if (properties.containsKey(PROFILES_PROPERTY)) {
                profilesField.setText(properties.getProperty(PROFILES_PROPERTY));
            }
            if (properties.containsKey(LOCATIONS_PROPERTY)) {
                locationsFiled.setText(properties.getProperty(LOCATIONS_PROPERTY));
            }
            if (properties.containsKey(OUT_YAML_PROPERTY)) {
                outYamlField.setText(properties.getProperty(OUT_YAML_PROPERTY));
            }
        } else {
            if (!propertiesFile.createNewFile()) {
                throw new RuntimeException("Fail to make property file: " + propertiesFile.getPath());
            }
        }
    }

    private String getBaseDir() {
        String baseDire = project.getBasePath();
        if (baseDire == null) {
            throw new RuntimeException("Project base path is null.");
        }
        return baseDire;
    }

    private String[] getLocations() {
        String baseDir = getBaseDir();
        String locationsText = locationsFiled.getText();
        if (locationsText.isBlank()) {
            return new String[]{baseDir};
        }
        return Arrays
                .stream(locationsText.split("\\s*,\\s*"))
                .map(s -> new File(baseDir, s).toURI().getPath())
                .toArray(String[]::new);
    }
}
