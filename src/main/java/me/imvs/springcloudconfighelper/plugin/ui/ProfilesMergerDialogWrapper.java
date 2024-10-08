package me.imvs.springcloudconfighelper.plugin.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogPanel;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.IconLoader;
import me.imvs.springcloudconfighelper.plugin.PluginBundle;
import me.imvs.springcloudconfighelper.plugin.PluginIcons;
import me.imvs.springcloudconfighelper.plugin.model.PluginModel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ProfilesMergerDialogWrapper extends DialogWrapper {

    private final Project project;
    private final PluginModel pluginModel;

    public ProfilesMergerDialogWrapper(@Nullable Project project, boolean canBeParent, PluginModel pluginModel) {
        super(project, canBeParent);
        this.project = project;
        this.pluginModel = pluginModel;
        setTitle(PluginBundle.property("dialog.title"));
        setResizable(false);
        init();
        getWindow().setIconImage(IconLoader.toImage(PluginIcons.notification));
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        DialogPanel dialogPanel = new UiDsl(pluginModel).pluginPanel(project);
        dialogPanel.setPreferredSize(new Dimension(500, 0));
        return dialogPanel;
    }
}
