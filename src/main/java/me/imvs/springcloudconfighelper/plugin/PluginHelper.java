package me.imvs.springcloudconfighelper.plugin;

public class PluginHelper {

    public static String[] splitCommaSeparated(String string) {
        return string.split("\\s*,\\s*");
    }
}
