package me.imvs.springcloudconfighelper.plugin.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.browsers.BrowserLauncher;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.fields.ExpandableTextField;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.Consumer;
import me.imvs.springcloudconfighelper.ProfileNotFoundException;
import me.imvs.springcloudconfighelper.plugin.PluginBundle;
import me.imvs.springcloudconfighelper.plugin.PluginException;
import me.imvs.springcloudconfighelper.plugin.PluginIcons;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import static me.imvs.springcloudconfighelper.FilesHelper.getBaseDirVirtualFile;

public class UiHelper {

    private final static UiHelper INSTANCE = new UiHelper();

    public UiHelper() {
    }

    public static UiHelper getInstance() {
        return INSTANCE;
    }

    @NotNull
    @SuppressWarnings("SameParameterValue")
    private FileChooserDescriptor fileChooserDescriptor(String title, boolean isFile, boolean isMultiple) {
        FileChooserDescriptor descriptor;
        if (isMultiple) {
            descriptor = isFile
                    ? FileChooserDescriptorFactory.createMultipleFilesNoJarsDescriptor()
                    : FileChooserDescriptorFactory.createMultipleFoldersDescriptor();
        } else {
            descriptor = isFile
                    ? FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
                    : FileChooserDescriptorFactory.createSingleFolderDescriptor();
        }
        descriptor.setTitle(title);
        return descriptor;
    }

    public ExtendableTextComponent.Extension fileChooserExtension(
            @NotNull Project project,
            String title,
            String tooltip,
            boolean isFile, boolean isMultiple,
            @NotNull Icon defaultIcon, @NotNull Icon hoveredIcon,
            @NotNull Consumer<? super List<VirtualFile>> fileChooserCallback
    ) {
        FileChooserDescriptor descriptor =
                fileChooserDescriptor(title, isFile, isMultiple);
        return ExtendableTextComponent.Extension.create(
                defaultIcon, hoveredIcon,
                tooltip,
                () -> FileChooser.chooseFiles(
                        descriptor,
                        project,
                        getBaseDirVirtualFile(project),
                        fileChooserCallback
                )
        );
    }

    public JTextField profilesTextField() {
        ExpandableTextField expandableTextField = new ExpandableTextField();
        expandableTextField.setToolTipText(PluginBundle.property("active.profiles.help"));
        return expandableTextField;
    }

    public JTextField appNameField() {
        JBTextField field = new JBTextField();
        field.setToolTipText(PluginBundle.property("application.name.help"));
        return field;
    }

    public JTextField searchLocationsTextField(@NotNull Project project) {
        String title = PluginBundle.property("search.locations.label");
        ExpandableTextField field = new ExpandableTextField();
        ExtendableTextComponent.Extension extension =
                fileChooserExtension(
                        project,
                        title, null,
                        false, true,
                        AllIcons.General.OpenDiskHover, AllIcons.General.OpenDiskHover,
                        virtualFiles -> virtualFiles
                                .stream()
                                .map(VirtualFile::getPath)
                                .reduce((s, s2) -> s + ", " + s2)
                                .ifPresent(field::setText));
        field.addExtension(extension);
        field.setToolTipText(PluginBundle.property("search.locations.help"));
        return field;
    }

    public JTextField outFileTextField(@NotNull Project project, String appName) {
        String title = PluginBundle.property("output.file.label");
        ExtendableTextField field = new ExpandableTextField();
        ExtendableTextComponent.Extension extension =
                fileChooserExtension(
                        project,
                        title, null,
                        false, false,
                        AllIcons.General.OpenDiskHover, AllIcons.General.OpenDiskHover,
                        virtualFiles -> {
                            if (virtualFiles.isEmpty()) {
                                return;
                            }
                            String path;
                            if (appName == null || appName.isBlank()) {
                                path = virtualFiles.get(0).getPath();
                            } else {
                                path = Path.of(virtualFiles.get(0).getPath(), appName + ".yml").toString();
                            }
                            field.setText(path);
                        });
        field.addExtension(extension);
        field.setToolTipText(PluginBundle.property("output.file.help"));
        return field;
    }

    public void showSuccessNotification(@NotNull Project project, VirtualFile outFile) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("Spring Profiles Merger Group")
                .createNotification(
                        PluginBundle.property("message.success"),
                        PluginBundle.message("message.success.description", outFile),
                        NotificationType.INFORMATION
                )
                .addAction(NotificationAction.create(
                        PluginBundle.property("message.open.file"),
                        anActionEvent -> FileEditorManager.getInstance(project)
                                .openTextEditor(new OpenFileDescriptor(project, outFile), true)
                ))
                .setIcon(PluginIcons.notification)
                .notify(project);
    }

    public void showPropertySourceEmtyNotification(@Nullable Project project) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("Spring Profiles Merger Group")
                .createNotification(
                        PluginBundle.property("message.failed"),
                        PluginBundle.property("message.no.property.sources.title"),
                        NotificationType.WARNING
                )
                .notify(project);

    }

    public void showProfileNotFoundNotification(@Nullable Project project, ProfileNotFoundException ex) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("Spring Profiles Merger Group")
                .createNotification(
                        PluginBundle.property("message.failed"),
                        PluginBundle.message("message.no.profile.title", ex.getProfile()),
                        NotificationType.WARNING
                )
                .notify(project);
    }

    public void showExpectedErrorNotification(@Nullable Project project, PluginException t) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("Spring Profiles Merger Group")
                .createNotification(
                        PluginBundle.property("message.failed"),
                        t.getCause() == null
                                ? t.getMessage()
                                : PluginBundle.message("message.error.details", t.getMessage(), t.getCause()),
                        NotificationType.ERROR)
                .notify(project);
    }

    public void showUnexpectedErrorNotification(@NotNull Project project, Throwable t) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("Spring Profiles Merger Group")
                .createNotification(
                        PluginBundle.property("message.error.unexpected"),
                        ExceptionUtils.getRootCauseMessage(t) + "\n" + ExceptionUtils.getStackTrace(t),
                        NotificationType.ERROR
                )
                .addAction(NotificationAction.create(
                        PluginBundle.property("message.error.unexpected.issue"),
                        anActionEvent -> BrowserLauncher.getInstance().browse(URI.create(PluginBundle.property("links.plugin.source")))
                ))
                .notify(project);
    }
}
