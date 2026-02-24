package io.github.vanillafairy.docsetviewer.editor

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import io.github.vanillafairy.docsetviewer.editor.actions.BackAction
import io.github.vanillafairy.docsetviewer.editor.actions.ForwardAction
import io.github.vanillafairy.docsetviewer.editor.actions.HomeAction
import io.github.vanillafairy.docsetviewer.editor.actions.ReloadAction
import javax.swing.JComponent
import javax.swing.SwingUtilities

/**
 * Shared navigation toolbar for the docset browser.
 * Used by both DocsetFileEditor and DocsetToolWindowPanel.
 * Automatically refreshes button state when the browser navigates.
 */
class DocsetNavigationToolbar(
    browserPanel: DocsetBrowserPanel,
    targetComponent: JComponent
) {
    val component: JComponent
    private val actionToolbar: ActionToolbar

    init {
        val actionGroup = DefaultActionGroup().apply {
            add(BackAction(browserPanel))
            add(ForwardAction(browserPanel))
            add(ReloadAction(browserPanel))
            add(HomeAction(browserPanel))
        }

        actionToolbar = ActionManager.getInstance().createActionToolbar(
            "DocsetViewer.Navigation",
            actionGroup,
            true
        )
        actionToolbar.targetComponent = targetComponent
        component = actionToolbar.component

        // Force-refresh toolbar button state whenever a page loads
        browserPanel.addNavigationListener {
            SwingUtilities.invokeLater {
                actionToolbar.updateActionsImmediately()
            }
        }
    }
}
