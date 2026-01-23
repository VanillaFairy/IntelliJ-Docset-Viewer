package io.github.vanillafairy.docsetviewer.ui.toolwindow

import io.github.vanillafairy.docsetviewer.core.index.DocsetIndexService
import io.github.vanillafairy.docsetviewer.core.model.Docset
import io.github.vanillafairy.docsetviewer.core.model.DocsetEntry
import io.github.vanillafairy.docsetviewer.core.model.DocsetType
import io.github.vanillafairy.docsetviewer.editor.DocsetVirtualFile
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBSplitter
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.icons.AllIcons
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.Timer

/**
 * Main panel for the Docset tool window.
 * Contains a docset list and an entry search/list view.
 */
class DocsetToolWindowPanel(
    private val project: Project
) : JPanel(BorderLayout()), Disposable {

    private val docsetListModel = DefaultListModel<Docset>()
    private val docsetList = JBList(docsetListModel)

    private val entryListModel = DefaultListModel<DocsetEntry>()
    private val entryList = JBList(entryListModel)

    private val searchField = SearchTextField()
    private var searchDebounceTimer: Timer? = null

    private val indexService = DocsetIndexService.getInstance()

    init {
        setupUI()
        setupListeners()
        refreshDocsets()
    }

    private fun setupUI() {
        // Docset list on the left
        docsetList.cellRenderer = DocsetCellRenderer()
        docsetList.selectionMode = ListSelectionModel.SINGLE_SELECTION

        val docsetPanel = JPanel(BorderLayout())
        docsetPanel.add(JLabel("Docsets"), BorderLayout.NORTH)
        docsetPanel.add(JBScrollPane(docsetList), BorderLayout.CENTER)

        // Entry list on the right
        entryList.cellRenderer = EntryCellRenderer()
        entryList.selectionMode = ListSelectionModel.SINGLE_SELECTION

        val entryPanel = JPanel(BorderLayout())
        entryPanel.add(searchField, BorderLayout.NORTH)
        entryPanel.add(JBScrollPane(entryList), BorderLayout.CENTER)

        // Split view
        val splitter = JBSplitter(false, 0.3f)
        splitter.firstComponent = docsetPanel
        splitter.secondComponent = entryPanel

        add(splitter, BorderLayout.CENTER)
    }

    private fun setupListeners() {
        // Docset selection listener
        docsetList.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                onDocsetSelected()
            }
        }

        // Entry double-click listener
        entryList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    openSelectedEntry()
                }
            }
        })

        // Search field listener with debouncing
        searchField.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                debounceSearch()
            }
        })

        // Index service listener
        indexService.addChangeListener(object : DocsetIndexService.DocsetChangeListener {
            override fun onDocsetLoaded(docset: Docset) {
                SwingUtilities.invokeLater { refreshDocsets() }
            }

            override fun onDocsetUnloaded(identifier: String) {
                SwingUtilities.invokeLater { refreshDocsets() }
            }
        })
    }

    private fun debounceSearch() {
        searchDebounceTimer?.stop()
        searchDebounceTimer = Timer(300) {
            performSearch()
        }
        searchDebounceTimer?.isRepeats = false
        searchDebounceTimer?.start()
    }

    private fun onDocsetSelected() {
        performSearch()
    }

    private fun performSearch() {
        val query = searchField.text.trim()
        val selectedDocset = docsetList.selectedValue

        ApplicationManager.getApplication().executeOnPooledThread {
            val results = if (selectedDocset != null) {
                indexService.searchInDocset(query, selectedDocset.identifier, 200)
            } else {
                indexService.searchAll(query, 200)
            }

            SwingUtilities.invokeLater {
                entryListModel.clear()
                results.forEach { entryListModel.addElement(it) }
            }
        }
    }

    private fun openSelectedEntry() {
        val entry = entryList.selectedValue ?: return
        val docset = indexService.getDocset(entry.docsetIdentifier) ?: return

        val virtualFile = DocsetVirtualFile(entry, docset)
        FileEditorManager.getInstance(project).openFile(virtualFile, true)
    }

    fun refreshDocsets() {
        docsetListModel.clear()
        indexService.getAllDocsets().forEach { docsetListModel.addElement(it) }

        // Update entry list
        if (docsetListModel.size() > 0 && docsetList.selectedIndex < 0) {
            docsetList.selectedIndex = 0
        }
        performSearch()
    }

    override fun dispose() {
        searchDebounceTimer?.stop()
    }

    /**
     * Cell renderer for docset list items.
     */
    private class DocsetCellRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)

            if (value is Docset) {
                text = "${value.name} (${value.entryCount})"
                icon = AllIcons.Nodes.PpLib
            }

            return this
        }
    }

    /**
     * Cell renderer for entry list items with type icons.
     */
    private class EntryCellRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)

            if (value is DocsetEntry) {
                text = "${value.name} (${value.type.displayName})"
                icon = getIconForType(value.type)
            }

            return this
        }

        private fun getIconForType(type: DocsetType): Icon {
            return when (type) {
                DocsetType.CLASS -> AllIcons.Nodes.Class
                DocsetType.METHOD -> AllIcons.Nodes.Method
                DocsetType.FUNCTION -> AllIcons.Nodes.Function
                DocsetType.PROPERTY -> AllIcons.Nodes.Property
                DocsetType.CONSTANT -> AllIcons.Nodes.Constant
                DocsetType.VARIABLE -> AllIcons.Nodes.Variable
                DocsetType.TYPE -> AllIcons.Nodes.Type
                DocsetType.STRUCT -> AllIcons.Nodes.Static
                DocsetType.ENUM -> AllIcons.Nodes.Enum
                DocsetType.INTERFACE -> AllIcons.Nodes.Interface
                DocsetType.FIELD -> AllIcons.Nodes.Field
                DocsetType.NAMESPACE -> AllIcons.Nodes.Package
                DocsetType.MODULE -> AllIcons.Nodes.Module
                DocsetType.PACKAGE -> AllIcons.Nodes.Package
                DocsetType.GUIDE -> AllIcons.FileTypes.Text
                DocsetType.EXCEPTION -> AllIcons.Nodes.ExceptionClass
                DocsetType.ANNOTATION -> AllIcons.Nodes.Annotationtype
                DocsetType.BUILTIN -> AllIcons.Nodes.AbstractMethod
                else -> AllIcons.Nodes.Unknown
            }
        }
    }
}
