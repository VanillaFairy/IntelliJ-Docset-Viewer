package io.github.vanillafairy.docsetviewer.ui.toolwindow

import io.github.vanillafairy.docsetviewer.core.index.DocsetIndexService
import io.github.vanillafairy.docsetviewer.core.model.Docset
import io.github.vanillafairy.docsetviewer.core.model.DocsetEntry
import io.github.vanillafairy.docsetviewer.core.model.DocsetType
import io.github.vanillafairy.docsetviewer.editor.DocsetBrowserPanel
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBSplitter
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.icons.AllIcons
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.tree.*
import javax.swing.Timer

/**
 * Main panel for the Docset tool window.
 * Contains a tree of docsets/entries on the left and a browser panel on the right.
 */
class DocsetToolWindowPanel(
    private val project: Project
) : JPanel(BorderLayout()), Disposable {

    private val rootNode = DefaultMutableTreeNode("Docsets")
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree = Tree(treeModel)

    private val searchField = SearchTextField()
    private var searchDebounceTimer: Timer? = null

    private val indexService = DocsetIndexService.getInstance()

    private val browserPanel = DocsetBrowserPanel(this)

    // Track if we're programmatically selecting to avoid recursive updates
    private var isProgrammaticSelection = false

    init {
        setupUI()
        setupListeners()
        refreshDocsets()
    }

    private fun setupUI() {
        // Tree setup
        tree.cellRenderer = DocsetTreeCellRenderer()
        tree.isRootVisible = false
        tree.showsRootHandles = true
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION

        val treePanel = JPanel(BorderLayout())
        treePanel.add(searchField, BorderLayout.NORTH)
        treePanel.add(JBScrollPane(tree), BorderLayout.CENTER)

        // Main split: tree panel on the left and browser on the right
        val mainSplitter = JBSplitter(false, 0.25f)
        mainSplitter.firstComponent = treePanel
        mainSplitter.secondComponent = browserPanel.component

        add(mainSplitter, BorderLayout.CENTER)
    }

    private fun setupListeners() {
        // Tree selection listener
        tree.addTreeSelectionListener {
            if (!isProgrammaticSelection) {
                onTreeSelectionChanged()
            }
        }

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

    private fun onTreeSelectionChanged() {
        val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return
        val userObject = node.userObject

        if (userObject is DocsetEntry) {
            openEntryInBrowser(userObject)
        }
    }

    private fun debounceSearch() {
        searchDebounceTimer?.stop()
        searchDebounceTimer = Timer(300) {
            performSearch()
        }
        searchDebounceTimer?.isRepeats = false
        searchDebounceTimer?.start()
    }

    private fun performSearch() {
        val query = searchField.text.trim()

        ApplicationManager.getApplication().executeOnPooledThread {
            val docsets = indexService.getAllDocsets()

            val filteredDocsets = if (query.isEmpty()) {
                // No search query - show all docsets with their entries
                docsets.map { docset ->
                    DocsetWithEntries(docset, docset.entries)
                }
            } else {
                // Search query - show only matching entries under each docset
                docsets.mapNotNull { docset ->
                    val matchingEntries = indexService.searchInDocset(query, docset.identifier, 100)
                    if (matchingEntries.isNotEmpty()) {
                        DocsetWithEntries(docset, matchingEntries)
                    } else {
                        null
                    }
                }
            }

            SwingUtilities.invokeLater {
                rebuildTree(filteredDocsets, expandAll = query.isNotEmpty())
            }
        }
    }

    private data class DocsetWithEntries(val docset: Docset, val entries: List<DocsetEntry>)

    private fun rebuildTree(docsetsWithEntries: List<DocsetWithEntries>, expandAll: Boolean = false) {
        // Remember current selection
        val selectedEntry = (tree.lastSelectedPathComponent as? DefaultMutableTreeNode)
            ?.userObject as? DocsetEntry

        rootNode.removeAllChildren()

        for (dwe in docsetsWithEntries) {
            val docsetNode = DefaultMutableTreeNode(dwe.docset)
            for (entry in dwe.entries) {
                docsetNode.add(DefaultMutableTreeNode(entry))
            }
            rootNode.add(docsetNode)
        }

        treeModel.reload()

        if (expandAll) {
            // Expand all docsets when searching
            for (i in 0 until rootNode.childCount) {
                val docsetNode = rootNode.getChildAt(i) as DefaultMutableTreeNode
                tree.expandPath(TreePath(docsetNode.path))
            }
        }

        // Try to restore selection
        if (selectedEntry != null) {
            selectEntry(selectedEntry, collapseOthers = false)
        }
    }

    private fun openEntryInBrowser(entry: DocsetEntry) {
        val docset = indexService.getDocset(entry.docsetIdentifier) ?: return

        val fullPath = docset.resolveDocumentPath(entry.path).toFile()

        if (fullPath.exists()) {
            browserPanel.loadUrl(fullPath.toURI().toString())
        }
    }

    /**
     * Opens an entry in the browser panel and selects it in the tree.
     * Called from external actions (e.g., FindInDocsetsAction).
     */
    fun openEntry(entry: DocsetEntry) {
        // First, open in browser
        openEntryInBrowser(entry)

        // Then select in tree (this will collapse other docsets)
        selectEntry(entry, collapseOthers = true)
    }

    /**
     * Selects an entry in the tree, optionally collapsing other docset nodes.
     */
    private fun selectEntry(entry: DocsetEntry, collapseOthers: Boolean) {
        isProgrammaticSelection = true
        try {
            // Find the entry node in the tree
            for (i in 0 until rootNode.childCount) {
                val docsetNode = rootNode.getChildAt(i) as DefaultMutableTreeNode
                val docset = docsetNode.userObject as? Docset ?: continue

                if (docset.identifier == entry.docsetIdentifier) {
                    // Found the docset - now find the entry
                    for (j in 0 until docsetNode.childCount) {
                        val entryNode = docsetNode.getChildAt(j) as DefaultMutableTreeNode
                        val nodeEntry = entryNode.userObject as? DocsetEntry ?: continue

                        if (nodeEntry.name == entry.name && nodeEntry.path == entry.path) {
                            // Found the entry - expand docset and select entry
                            if (collapseOthers) {
                                collapseAllDocsets()
                            }
                            val path = TreePath(entryNode.path)
                            tree.expandPath(path.parentPath)
                            tree.selectionPath = path
                            tree.scrollPathToVisible(path)
                            return
                        }
                    }

                    // Entry not found in current tree (might be filtered out)
                    // Just expand the docset
                    if (collapseOthers) {
                        collapseAllDocsets()
                    }
                    tree.expandPath(TreePath(docsetNode.path))
                    return
                }
            }
        } finally {
            isProgrammaticSelection = false
        }
    }

    private fun collapseAllDocsets() {
        for (i in 0 until rootNode.childCount) {
            val docsetNode = rootNode.getChildAt(i) as DefaultMutableTreeNode
            tree.collapsePath(TreePath(docsetNode.path))
        }
    }

    fun refreshDocsets() {
        performSearch()
    }

    override fun dispose() {
        searchDebounceTimer?.stop()
        Disposer.dispose(browserPanel)
    }

    /**
     * Tree cell renderer for docsets and entries with appropriate icons.
     */
    private class DocsetTreeCellRenderer : DefaultTreeCellRenderer() {
        override fun getTreeCellRendererComponent(
            tree: JTree?,
            value: Any?,
            sel: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ): Component {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)

            val node = value as? DefaultMutableTreeNode ?: return this
            val userObject = node.userObject

            when (userObject) {
                is Docset -> {
                    text = "${userObject.name} (${userObject.entryCount})"
                    icon = AllIcons.Nodes.PpLib
                }
                is DocsetEntry -> {
                    text = "${userObject.name} (${userObject.type.displayName})"
                    icon = getIconForType(userObject.type)
                }
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

    companion object {
        /**
         * Gets the DocsetToolWindowPanel instance for a project.
         */
        fun getInstance(project: Project): DocsetToolWindowPanel? {
            val toolWindow = com.intellij.openapi.wm.ToolWindowManager
                .getInstance(project)
                .getToolWindow("Docsets") ?: return null

            val content = toolWindow.contentManager.getContent(0) ?: return null
            return content.component as? DocsetToolWindowPanel
        }
    }
}
