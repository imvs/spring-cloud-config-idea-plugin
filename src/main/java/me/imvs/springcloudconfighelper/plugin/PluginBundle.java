package me.imvs.springcloudconfighelper.plugin;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public final class PluginBundle {
    @NonNls
    private static final String BUNDLE = "messages.PluginUiBundle";
    private static final DynamicBundle INSTANCE =
            new DynamicBundle(PluginBundle.class, BUNDLE);

    public PluginBundle() {
    }

    @Nls
    @NotNull
    public static String property(
            @NotNull @PropertyKey(resourceBundle = BUNDLE) String key
    ) {
        return INSTANCE.getResourceBundle().getString(key);
    }

    @Nls
    @NotNull
    public static String message(
            @NotNull @PropertyKey(resourceBundle = BUNDLE) String key, @NotNull Object... params
    ) {
        return INSTANCE.getMessage(key, params);
    }
}
