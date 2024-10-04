package me.imvs.springcloudconfighelper.plugin;

public class PluginException extends RuntimeException {

    public PluginException(String message, Throwable e) {
        super(message, e);
    }
    public PluginException(String message) {
        super(message);
    }
}
