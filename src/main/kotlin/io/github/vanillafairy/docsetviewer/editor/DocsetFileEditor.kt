package io.github.vanillafairy.docsetviewer.editor

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.icons.AllIcons
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
    private var toolbar: ActionToolbar? = null

    init {
        mainPanel = JPanel(BorderLayout())
        browserPanel = DocsetBrowserPanel(this)

        setupToolbar()
        mainPanel.add(browserPanel.component, BorderLayout.CENTER)

        // Load the initial URL
        browserPanel.loadUrl(file.getDocumentUrl())
    }

    private fun setupToolbar() {
        val actionGroup = DefaultActionGroup().apply {
            add(BackAction())
            add(ForwardAction())
            add(ReloadAction())
        }

        toolbar = ActionManager.getInstance().createActionToolbar(
            "DocsetViewer",
            actionGroup,
            true
        )
        toolbar?.targetComponent = mainPanel
        mainPanel.add(toolbar!!.component, BorderLayout.NORTH)
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

    private inner class BackAction : AnAction("Back", "Go back", AllIcons.Actions.Back) {
        override fun actionPerformed(e: AnActionEvent) {
            browserPanel.goBack()
        }

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = browserPanel.canGoBack()
        }

        override fun getActionUpdateThread() = com.intellij.openapi.actionSystem.ActionUpdateThread.EDT
    }

    private inner class ForwardAction : AnAction("Forward", "Go forward", AllIcons.Actions.Forward) {
        override fun actionPerformed(e: AnActionEvent) {
            browserPanel.goForward()
        }

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = browserPanel.canGoForward()
        }

        override fun getActionUpdateThread() = com.intellij.openapi.actionSystem.ActionUpdateThread.EDT
    }

    private inner class ReloadAction : AnAction("Reload", "Reload page", AllIcons.Actions.Refresh) {
        override fun actionPerformed(e: AnActionEvent) {
            browserPanel.reload()
        }

        override fun getActionUpdateThread() = com.intellij.openapi.actionSystem.ActionUpdateThread.EDT
    }
}
