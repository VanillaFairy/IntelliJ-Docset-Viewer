package io.github.vanillafairy.docsetviewer.settings

import io.github.vanillafairy.docsetviewer.core.DocsetValidator
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Settings UI for configuring docset paths.
 */
class DocsetSettingsConfigurable : Configurable {

    private var panel: JPanel? = null
    private var listModel: DefaultListModel<String>? = null
    private var pathList: JBList<String>? = null

    override fun getDisplayName(): String = "Docset Viewer"

    override fun createComponent(): JComponent {
        val model = DefaultListModel<String>()
        listModel = model
        val list = JBList(model)
        pathList = list

        val decorator = ToolbarDecorator.createDecorator(list)
            .setAddAction { addDocset() }
            .setRemoveAction { removeSelectedDocset() }
            .disableUpDownActions()

        val mainPanel = JPanel(BorderLayout())

        // Header with description
        val headerPanel = JPanel(BorderLayout())
        headerPanel.border = JBUI.Borders.emptyBottom(8)
        headerPanel.add(JBLabel("Configure paths to .docset bundles:"), BorderLayout.WEST)
        mainPanel.add(headerPanel, BorderLayout.NORTH)

        // List panel
        mainPanel.add(decorator.createPanel(), BorderLayout.CENTER)

        // Bottom panel with import buttons
        val bottomPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        bottomPanel.border = JBUI.Borders.emptyTop(8)

        val importZealButton = JButton("Import from Zeal")
        importZealButton.addActionListener { importFromZeal() }
        bottomPanel.add(importZealButton)

        mainPanel.add(bottomPanel, BorderLayout.SOUTH)

        panel = mainPanel
        return mainPanel
    }

    private fun addDocset() {
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            .withTitle("Select Docset")
            .withDescription("Choose a .docset folder")

        val file = FileChooser.chooseFile(descriptor, null, null) ?: return

        val path = file.path
        if (!DocsetValidator.hasDocsetExtension(path)) {
            Messages.showWarningDialog(
                "Please select a folder ending with .docset",
                "Invalid Docset"
            )
            return
        }

        if (!isValidDocset(Paths.get(path))) {
            Messages.showWarningDialog(
                "The selected folder does not appear to be a valid docset.\n" +
                        "It should contain Contents/Info.plist and Contents/Resources/docSet.dsidx",
                "Invalid Docset"
            )
            return
        }

        addPathToModel(path)
    }

    private fun importFromZeal() {
        val zealPaths = findZealDocsetPaths()

        if (zealPaths.isEmpty()) {
            Messages.showInfoMessage(
                "Could not find Zeal docsets directory.\n\n" +
                        "Checked locations:\n" +
                        "  - %LOCALAPPDATA%\\Zeal\\Zeal\\docsets\n" +
                        "  - %APPDATA%\\Zeal\\Zeal\\docsets\n" +
                        "  - ~/.local/share/Zeal/Zeal/docsets\n\n" +
                        "Please make sure Zeal is installed and has docsets downloaded.",
                "Zeal Not Found"
            )
            return
        }

        var importedCount = 0
        var skippedCount = 0

        for (docsetPath in zealPaths) {
            if (isValidDocset(docsetPath)) {
                if (addPathToModel(docsetPath.toString())) {
                    importedCount++
                } else {
                    skippedCount++
                }
            }
        }

        val message = buildString {
            append("Imported $importedCount docset(s) from Zeal.")
            if (skippedCount > 0) {
                append("\n$skippedCount docset(s) were already configured.")
            }
        }

        Messages.showInfoMessage(message, "Import Complete")
    }

    private fun findZealDocsetPaths(): List<Path> {
        val docsets = mutableListOf<Path>()

        // Windows paths
        val localAppData = System.getenv("LOCALAPPDATA")
        val appData = System.getenv("APPDATA")
        val userHome = System.getProperty("user.home")

        val possibleZealDirs = listOfNotNull(
            localAppData?.let { Paths.get(it, "Zeal", "Zeal", "docsets") },
            appData?.let { Paths.get(it, "Zeal", "Zeal", "docsets") },
            // Linux/macOS paths
            Paths.get(userHome, ".local", "share", "Zeal", "Zeal", "docsets"),
            Paths.get(userHome, "Library", "Application Support", "Zeal", "docsets")
        )

        for (zealDir in possibleZealDirs) {
            if (Files.exists(zealDir) && Files.isDirectory(zealDir)) {
                try {
                    Files.list(zealDir).use { stream ->
                        stream.filter { path ->
                            Files.isDirectory(path) && path.fileName.toString().endsWith(".docset")
                        }.forEach { docsets.add(it) }
                    }
                } catch (e: Exception) {
                    // Ignore errors reading directory
                }
            }
        }

        return docsets
    }

    private fun isValidDocset(docsetPath: Path): Boolean {
        return DocsetValidator.hasRequiredFiles(docsetPath)
    }

    private fun addPathToModel(path: String): Boolean {
        val normalizedPath = path.replace('\\', '/')
        val model = listModel ?: return false
        if (!model.contains(normalizedPath)) {
            model.addElement(normalizedPath)
            return true
        }
        return false
    }

    private fun removeSelectedDocset() {
        val selectedIndex = pathList?.selectedIndex ?: return
        if (selectedIndex >= 0) {
            listModel?.remove(selectedIndex)
        }
    }

    override fun isModified(): Boolean {
        val settings = DocsetSettings.getInstance()
        val currentPaths = settings.getDocsetPaths()
        val modelPaths = getModelPaths()
        return currentPaths != modelPaths
    }

    override fun apply() {
        val settings = DocsetSettings.getInstance()
        settings.setDocsetPaths(getModelPaths())
    }

    override fun reset() {
        val settings = DocsetSettings.getInstance()
        listModel?.clear()
        settings.getDocsetPaths().forEach { listModel?.addElement(it) }
    }

    override fun disposeUIResources() {
        panel = null
        listModel = null
        pathList = null
    }

    private fun getModelPaths(): List<String> {
        val paths = mutableListOf<String>()
        val model = listModel ?: return paths
        for (i in 0 until model.size()) {
            paths.add(model.getElementAt(i))
        }
        return paths
    }
}
