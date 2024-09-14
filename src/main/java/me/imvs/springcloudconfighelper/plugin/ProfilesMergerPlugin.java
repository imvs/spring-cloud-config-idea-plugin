package me.imvs.springcloudconfighelper.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class ProfilesMergerPlugin extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        PluginDialog dialog;
        try {
            dialog = new PluginDialog(e.getProject());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        dialog.setSize(500, 350);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    @Override
    public boolean isDumbAware() {
        return super.isDumbAware();
    }
}
