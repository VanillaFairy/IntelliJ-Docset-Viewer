package io.github.vanillafairy.docsetviewer.editor.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import io.github.vanillafairy.docsetviewer.editor.DocsetBrowserPanel

/**
 * Action to reload the current page in the docset browser.
 */
class ReloadAction(
    private val browserPanel: DocsetBrowserPanel
) : AnAction("Reload", "Reload page", AllIcons.Actions.Refresh) {

    override fun actionPerformed(e: AnActionEvent) {
        browserPanel.reload()
    }

    override fun getActionUpdateThread() = ActionUpdateThread.EDT
}
