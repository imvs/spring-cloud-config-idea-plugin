package me.imvs.springcloudconfighelper.plugin.model;

import com.intellij.ui.RecentsManager;

import java.util.Arrays;
import java.util.Collection;

@SuppressWarnings({"LombokGetterMayBeUsed", "LombokSetterMayBeUsed"})
public final class PluginModel {

    private static final String APP_NAME_KEY = "appName";
    private static final String PROFILES_KEY = "profiles";
    private static final String SEARCH_LOCATIONS_KEY = "searchLocations";
    private static final String OUT_FILE_KEY = "outFile";
    private String appName = "application";
    private String profiles = "default";
    private String searchLocations = "";
    private String outFile = "";

    public PluginModel(RecentsManager manager) {
        appName = getRecent(manager, APP_NAME_KEY, appName);
        profiles = getRecent(manager, PROFILES_KEY, profiles);
        searchLocations = getRecent(manager, SEARCH_LOCATIONS_KEY, searchLocations);
        outFile = getRecent(manager, OUT_FILE_KEY, outFile);
    }

    private String getRecent(RecentsManager manager, String key, String defaultValue) {
        var appNameResent = manager.getRecentEntries(key);
        if (appNameResent != null && !appNameResent.isEmpty()) {
            return appNameResent.get(0);
        } else {
            return defaultValue;
        }
    }

    public void saveRecent(RecentsManager manager) {
        manager.registerRecentEntry(APP_NAME_KEY, appName);
        manager.registerRecentEntry(PROFILES_KEY, profiles);
        manager.registerRecentEntry(SEARCH_LOCATIONS_KEY, searchLocations);
        manager.registerRecentEntry(OUT_FILE_KEY, outFile);
    }

    public String getAppName() {
        return this.appName;
    }

    public String getProfiles() {
        return this.profiles;
    }

    public String getSearchLocations() {
        return this.searchLocations;
    }

    public String getOutFile() {
        return this.outFile;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setProfiles(String profiles) {
        this.profiles = profiles;
    }

    public void setSearchLocations(String searchLocations) {
        this.searchLocations = searchLocations;
    }

    public void setOutFile(String outFile) {
        this.outFile = outFile;
    }

    public String toString() {
        return "PluginModel(appName=" + this.getAppName() + ", profiles=" + this.getProfiles() + ", searchLocations=" + this.getSearchLocations() + ", outFile=" + this.getOutFile() + ")";
    }

    public void removeProfiles(Collection<String> notExistingProfiles) {
        String profiles = Arrays.stream(getProfiles().split("\\s*,\\s*"))
                .filter(p -> !notExistingProfiles.contains(p))
                .reduce((p, p2) -> p + ", " + p2)
                .orElse("");
        setProfiles(profiles);
    }
}
