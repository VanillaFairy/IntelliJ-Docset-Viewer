package io.github.vanillafairy.docsetviewer.editor.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import io.github.vanillafairy.docsetviewer.editor.DocsetBrowserPanel

/**
 * Split-button action group for back navigation.
 * Clicking the button goes back one step.
 * The dropdown arrow shows back history entries for direct jumping.
 */
class BackActionGroup(
    private val browserPanel: DocsetBrowserPanel
) : DefaultActionGroup("Back", true) {

    init {
        templatePresentation.icon = AllIcons.Actions.Back
        templatePresentation.description = "Go back"
        templatePresentation.putClientProperty("actionGroup.perform.firstAction", true)
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val actions = mutableListOf<AnAction>()

        // First action: go back one step (primary action for split button click)
        actions.add(object : AnAction("Back", "Go back one page", AllIcons.Actions.Back) {
            override fun actionPerformed(e: AnActionEvent) {
                browserPanel.goBack()
            }

            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled = browserPanel.canGoBack()
            }

            override fun getActionUpdateThread() = ActionUpdateThread.EDT
        })

        // Separator before history entries
        val backEntries = browserPanel.getHistory().getBackEntries()
        if (backEntries.isNotEmpty()) {
            actions.add(Separator.getInstance())

            // History entries
            backEntries.forEachIndexed { index, entry ->
                actions.add(object : AnAction(entry.title) {
                    override fun actionPerformed(e: AnActionEvent) {
                        browserPanel.goBackTo(index)
                    }

                    override fun getActionUpdateThread() = ActionUpdateThread.EDT
                })
            }
        }

        return actions.toTypedArray()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = browserPanel.canGoBack()
    }

    override fun getActionUpdateThread() = ActionUpdateThread.EDT
}
