package me.imvs.springcloudconfighelper.plugin;

import com.intellij.openapi.project.Project;
import me.imvs.springcloudconfighelper.CollectionsMerger;
import me.imvs.springcloudconfighelper.FileSupport;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

public class PluginDialog extends JDialog {

    private static final String APP_NAME_PROPERTY = "app-name";
    private static final String PROFILES_PROPERTY = "profiles";
    private static final String LOCATIONS_PROPERTY = "locations";
    private static final String OUT_YAML_PROPERTY = "out-yaml";
    private static final String BUILD_DIR = "build";
    private static final String DEFAULT_APP_NAME = "application";
    private static final String DEFAULT_OUT_FILE = "build/output.yml";
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField appNameField;
    private JTextField profilesField;
    private JTextField locationsFiled;
    private JTextField outYamlField;
    private JLabel errorMessage;
    private final Project project;
    private final FileSupport fileSupport;
    private Properties properties;

    public PluginDialog(Project project) throws IOException {
        setResizable(false);
        this.project = project;
        fileSupport = new FileSupport(getBaseDir());
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        buttonOK.addActionListener(e -> {
            try {
                onOK();
                errorMessage.setText("");
            } catch (Throwable ex) {
                errorMessage.setText(ex.getMessage());
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
        loadProperties();
    }

    private void onOK() throws IOException {
        String[] locations = getLocations();
        CollectionsMerger collectionsMerger = new CollectionsMerger();
        Map<String, Object> mergeResult = collectionsMerger.merge(locations, appNameField.getText(), profilesField.getText());
        fileSupport.writeYaml(mergeResult, outYamlField.getText());
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
        try (FileOutputStream fos = new FileOutputStream(fileSupport.getPropertiesFile(BUILD_DIR))) {
            properties.store(fos, null);
        }
    }

    private void loadProperties() throws IOException {
        properties = new Properties();
        File propertiesFile = fileSupport.getPropertiesFile(BUILD_DIR);
        if (propertiesFile.exists()) {
            try (FileInputStream fis = new FileInputStream(propertiesFile)) {
                properties.load(fis);
            }
            if (properties.containsKey(APP_NAME_PROPERTY)) {
                appNameField.setText(properties.getProperty(APP_NAME_PROPERTY));
            } else {
                appNameField.setText(DEFAULT_APP_NAME);
            }
            if (properties.containsKey(PROFILES_PROPERTY)) {
                profilesField.setText(properties.getProperty(PROFILES_PROPERTY));
            }
            if (properties.containsKey(LOCATIONS_PROPERTY)) {
                locationsFiled.setText(properties.getProperty(LOCATIONS_PROPERTY));
            }
            if (properties.containsKey(OUT_YAML_PROPERTY)) {
                outYamlField.setText(properties.getProperty(OUT_YAML_PROPERTY));
            } else {
                outYamlField.setText(DEFAULT_OUT_FILE);
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
