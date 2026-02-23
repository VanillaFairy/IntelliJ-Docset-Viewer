package io.github.vanillafairy.docsetviewer.editor.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import io.github.vanillafairy.docsetviewer.editor.DocsetBrowserPanel

/**
 * Action to navigate to the docset's home/index page.
 */
class HomeAction(
    private val browserPanel: DocsetBrowserPanel
) : AnAction("Home", "Go to docset index page", AllIcons.Actions.ShowAsTree) {

    override fun actionPerformed(e: AnActionEvent) {
        browserPanel.goHome()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = browserPanel.homeUrl != null
    }

    override fun getActionUpdateThread() = ActionUpdateThread.EDT
}
