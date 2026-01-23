package io.github.vanillafairy.docsetviewer.actions

import io.github.vanillafairy.docsetviewer.core.index.DocsetIndexService
import io.github.vanillafairy.docsetviewer.core.parser.DocsetParser
import io.github.vanillafairy.docsetviewer.settings.DocsetSettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.Messages
import java.nio.file.Paths

/**
 * Action to open and add a docset via file chooser.
 */
class OpenDocsetAction : AnAction("Add Docset...", "Add a docset folder", null) {

    override fun actionPerformed(e: AnActionEvent) {
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            .withTitle("Select Docset")
            .withDescription("Choose a .docset folder to add")

        val file = FileChooser.chooseFile(descriptor, e.project, null) ?: return

        val path = file.path
        if (!path.endsWith(".docset")) {
            Messages.showWarningDialog(
                e.project,
                "Please select a folder ending with .docset",
                "Invalid Docset"
            )
            return
        }

        val parser = DocsetParser()
        if (!parser.isValidDocset(Paths.get(path))) {
            Messages.showWarningDialog(
                e.project,
                "The selected folder does not appear to be a valid docset.\n" +
                        "It should contain Contents/Info.plist and Contents/Resources/docSet.dsidx",
                "Invalid Docset"
            )
            return
        }

        // Add to settings
        val settings = DocsetSettings.getInstance()
        val normalizedPath = path.replace('\\', '/')
        if (settings.addDocsetPath(normalizedPath)) {
            // Load the docset
            val indexService = DocsetIndexService.getInstance()
            val docset = indexService.loadDocset(normalizedPath)

            if (docset != null) {
                Messages.showInfoMessage(
                    e.project,
                    "Loaded docset: ${docset.name} (${docset.entryCount} entries)",
                    "Docset Added"
                )
            } else {
                Messages.showErrorDialog(
                    e.project,
                    "Failed to load docset from: $path",
                    "Error Loading Docset"
                )
                settings.removeDocsetPath(normalizedPath)
            }
        } else {
            Messages.showInfoMessage(
                e.project,
                "This docset is already configured.",
                "Docset Exists"
            )
        }
    }
}
