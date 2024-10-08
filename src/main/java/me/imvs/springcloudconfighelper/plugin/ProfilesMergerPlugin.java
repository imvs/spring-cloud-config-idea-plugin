package me.imvs.springcloudconfighelper.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.RecentsManager;
import me.imvs.springcloudconfighelper.core.ProfilesMerger;
import me.imvs.springcloudconfighelper.FilesHelper;
import me.imvs.springcloudconfighelper.ProfileNotFoundException;
import me.imvs.springcloudconfighelper.PropertySourceEmptyException;
import me.imvs.springcloudconfighelper.plugin.model.PluginModel;
import me.imvs.springcloudconfighelper.plugin.ui.ProfilesMergerDialogWrapper;
import me.imvs.springcloudconfighelper.plugin.ui.UiHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static me.imvs.springcloudconfighelper.FilesHelper.validateLocations;
import static me.imvs.springcloudconfighelper.FilesHelper.validateOutFile;

public class ProfilesMergerPlugin extends AnAction {

    private static final UiHelper uiHelper = UiHelper.getInstance();

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        Project project = e.getProject();
        if (project == null) {
            throw new PluginException(PluginBundle.property("message.error.expected.project.not.found"));
        }
        try {
            RecentsManager manager = RecentsManager.getInstance(project);
            PluginModel pluginModel = new PluginModel(manager);
            ProfilesMergerDialogWrapper wrapper =
                    new ProfilesMergerDialogWrapper(e.getProject(), true, pluginModel);
            wrapper.show();
            if (wrapper.getExitCode() == 0) {
                pluginModel.saveRecent(manager);
                VirtualFile virtualFile = merge(project, pluginModel);
                if (virtualFile != null) {
                    uiHelper.showSuccessNotification(project, virtualFile);
                }
            }
        } catch (PropertySourceEmptyException ex) {
            uiHelper.showPropertySourceEmtyNotification(e.getProject());
        } catch (ProfileNotFoundException ex) {
            uiHelper.showProfileNotFoundNotification(e.getProject(), ex);
        } catch (PluginException t) {
            uiHelper.showExpectedErrorNotification(e.getProject(), t);
        } catch (Throwable t) {
            uiHelper.showUnexpectedErrorNotification(e.getProject(), t);
        }
    }

    private @Nullable VirtualFile merge(Project project, PluginModel model) {
        String[] locations = validateLocations(project, model.getSearchLocations());
        String outFile = validateOutFile(project, model);
        ProfilesMerger profilesMerger = new ProfilesMerger();
        Map<String, Object> mergeResult = profilesMerger
                .merge(locations, model.getAppName(), model.getProfiles());
        try {
            Path path = FilesHelper.getFilePath(project, outFile);
            FilesHelper.getDir(new File(path.getParent().toUri()), true, null);
            FilesHelper.reWriteYaml(mergeResult, path);
            LocalFileSystem fs = LocalFileSystem.getInstance();
            return fs.refreshAndFindFileByIoFile(new File(path.toUri()));
        } catch (IOException e) {
            throw new PluginException(PluginBundle.property("message.error.expected.outfile"), e);
        }
    }
}
