package io.github.vanillafairy.docsetviewer.editor.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import io.github.vanillafairy.docsetviewer.editor.DocsetBrowserPanel

/**
 * Split-button action group for forward navigation.
 * Clicking the button goes forward one step.
 * The dropdown arrow shows forward history entries for direct jumping.
 */
class ForwardActionGroup(
    private val browserPanel: DocsetBrowserPanel
) : DefaultActionGroup("Forward", true) {

    init {
        templatePresentation.icon = AllIcons.Actions.Forward
        templatePresentation.description = "Go forward"
        templatePresentation.putClientProperty("actionGroup.perform.firstAction", true)
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val actions = mutableListOf<AnAction>()

        // First action: go forward one step
        actions.add(object : AnAction("Forward", "Go forward one page", AllIcons.Actions.Forward) {
            override fun actionPerformed(e: AnActionEvent) {
                browserPanel.goForward()
            }

            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled = browserPanel.canGoForward()
            }

            override fun getActionUpdateThread() = ActionUpdateThread.EDT
        })

        // Separator before history entries
        val forwardEntries = browserPanel.getHistory().getForwardEntries()
        if (forwardEntries.isNotEmpty()) {
            actions.add(Separator.getInstance())

            // History entries
            forwardEntries.forEachIndexed { index, entry ->
                actions.add(object : AnAction(entry.title) {
                    override fun actionPerformed(e: AnActionEvent) {
                        browserPanel.goForwardTo(index)
                    }

                    override fun getActionUpdateThread() = ActionUpdateThread.EDT
                })
            }
        }

        return actions.toTypedArray()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = browserPanel.canGoForward()
    }

    override fun getActionUpdateThread() = ActionUpdateThread.EDT
}
