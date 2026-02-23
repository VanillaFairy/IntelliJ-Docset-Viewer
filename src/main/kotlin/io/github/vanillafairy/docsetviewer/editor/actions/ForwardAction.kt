package io.github.vanillafairy.docsetviewer.editor.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.util.ui.JBUI
import io.github.vanillafairy.docsetviewer.editor.DocsetBrowserPanel
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.SwingUtilities

/**
 * Toolbar button for forward navigation.
 * Left-click goes forward one step. Right-click shows forward history popup.
 */
class ForwardAction(
    private val browserPanel: DocsetBrowserPanel
) : AnAction("Forward", "Go forward (right-click for history)", AllIcons.Actions.Forward), CustomComponentAction {

    override fun actionPerformed(e: AnActionEvent) {
        browserPanel.goForward()
    }

    override fun update(e: AnActionEvent) {
        val enabled = browserPanel.canGoForward()
        e.presentation.isEnabled = enabled
        val component = e.presentation.getClientProperty(CustomComponentAction.COMPONENT_KEY) as? JComponent
        component?.isEnabled = enabled
    }

    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        val button = JButton(AllIcons.Actions.Forward).apply {
            isContentAreaFilled = false
            isBorderPainted = false
            isOpaque = false
            border = JBUI.Borders.empty(4)
            preferredSize = ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
            toolTipText = "Forward (right-click for history)"
            isEnabled = presentation.isEnabled
        }

        button.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    showHistoryPopup(button)
                }
            }
        })

        return button
    }

    private fun showHistoryPopup(component: JComponent) {
        val entries = browserPanel.getHistory().getForwardEntries()
        if (entries.isEmpty()) return
        val menu = JPopupMenu()
        entries.forEachIndexed { index, entry ->
            val item = JMenuItem(entry.title)
            item.addActionListener { browserPanel.goForwardTo(index) }
            menu.add(item)
        }
        menu.show(component, 0, component.height)
    }
}
