package io.github.vanillafairy.docsetviewer.editor

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import io.github.vanillafairy.docsetviewer.editor.actions.BackActionGroup
import io.github.vanillafairy.docsetviewer.editor.actions.ForwardActionGroup
import io.github.vanillafairy.docsetviewer.editor.actions.HomeAction
import io.github.vanillafairy.docsetviewer.editor.actions.ReloadAction
import javax.swing.JComponent

/**
 * Shared navigation toolbar for the docset browser.
 * Used by both DocsetFileEditor and DocsetToolWindowPanel.
 */
class DocsetNavigationToolbar(
    browserPanel: DocsetBrowserPanel,
    targetComponent: JComponent
) {
    val component: JComponent

    init {
        val actionGroup = DefaultActionGroup().apply {
            add(BackActionGroup(browserPanel))
            add(ForwardActionGroup(browserPanel))
            add(ReloadAction(browserPanel))
            add(HomeAction(browserPanel))
        }

        val toolbar = ActionManager.getInstance().createActionToolbar(
            "DocsetViewer.Navigation",
            actionGroup,
            true
        )
        toolbar.targetComponent = targetComponent
        component = toolbar.component
    }
}
