package me.imvs.springcloudconfighelper.plugin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.containers.stream
import me.imvs.springcloudconfighelper.FilesHelper
import me.imvs.springcloudconfighelper.plugin.PluginBundle
import me.imvs.springcloudconfighelper.plugin.PluginHelper
import me.imvs.springcloudconfighelper.plugin.model.PluginModel
import java.nio.file.Files


class UiDsl(private val model: PluginModel) {

    private val uiHelper = UiHelper.getInstance()
    fun pluginPanel(project: Project): DialogPanel {
        return panel {
            var appName: String = model.appName
            val fieldCantBeEmptyMsg = PluginBundle.property("message.validation.field.empty")
            row(PluginBundle.property("application.name.label")) {
                label("").gap(RightGap.SMALL)
                cell(uiHelper.appNameField())
                    .align(AlignX.FILL)
                    .bindText(model::getAppName, model::setAppName)
                    .onChanged { field -> appName = field.text }
                    .addValidationRule(fieldCantBeEmptyMsg) { field -> field.getText().isBlank() }
            }
            row(PluginBundle.property("active.profiles.label")) {
                label("").gap(RightGap.SMALL)
                cell(uiHelper.profilesTextField())
                    .align(AlignX.FILL)
                    .bindText(model::getProfiles, model::setProfiles)
                    .addValidationRule(fieldCantBeEmptyMsg) { field -> field.getText().isBlank() }
            }
            row(PluginBundle.property("search.locations.label")) {
                val searchLocationNotFound = PluginBundle.property("message.error.expected.locations.not.found")
                label("").gap(RightGap.SMALL)
                cell(uiHelper.searchLocationsTextField(project))
                    .align(AlignX.FILL)
                    .bindText(model::getSearchLocations, model::setSearchLocations)
                    .addValidationRule(fieldCantBeEmptyMsg) { field -> field.getText().isBlank() }
                    .addValidationRule(searchLocationNotFound) { field ->
                        PluginHelper.splitCommaSeparated(field.getText())
                            .stream()
                            .anyMatch { s -> !Files.exists(FilesHelper.getFilePath(project, s)) }
                    }
            }
            row(PluginBundle.property("output.file.label")) {
                val outFileTextField = uiHelper.outFileTextField(project, appName)
                label("").gap(RightGap.SMALL)
                cell(outFileTextField)
                    .align(AlignX.FILL)
                    .bindText(model::getOutFile, model::setOutFile)
                    .addValidationRule(fieldCantBeEmptyMsg) { field -> field.getText().isBlank() }
            }
        }
    }
}