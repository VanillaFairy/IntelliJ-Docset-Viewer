package io.github.vanillafairy.docsetviewer.editor

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * FileEditor implementation for displaying docset documentation using JCEF.
 */
class DocsetFileEditor(
    private val file: DocsetVirtualFile
) : UserDataHolderBase(), FileEditor, Disposable {

    private val mainPanel: JPanel
    private val browserPanel: DocsetBrowserPanel

    init {
        mainPanel = JPanel(BorderLayout())
        browserPanel = DocsetBrowserPanel(this)

        // Set home URL from the docset
        val documentsPath = file.docset.documentsPath
        val indexFile = documentsPath.resolve("index.html").toFile()
        if (indexFile.exists()) {
            browserPanel.setHomeUrl(indexFile.toURI().toString())
        }

        // Add shared navigation toolbar
        val toolbar = DocsetNavigationToolbar(browserPanel, mainPanel)
        mainPanel.add(toolbar.component, BorderLayout.NORTH)
        mainPanel.add(browserPanel.component, BorderLayout.CENTER)

        // Load the initial URL
        browserPanel.loadUrl(file.getDocumentUrl())
    }

    override fun getComponent(): JComponent = mainPanel

    override fun getPreferredFocusedComponent(): JComponent = browserPanel.component

    override fun getName(): String = "Docset Viewer"

    override fun setState(state: FileEditorState) {}

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = file.isValid

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}

    override fun getCurrentLocation(): FileEditorLocation? = null

    override fun dispose() {
        Disposer.dispose(browserPanel)
    }

    override fun getFile(): VirtualFile = file
}
