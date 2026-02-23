# Back/Forward Right-Click History Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace split-button `BackActionGroup`/`ForwardActionGroup` with simple icon buttons where left-click navigates and right-click shows a history `JPopupMenu`.

**Architecture:** Each button is an `AnAction` that also implements `CustomComponentAction` (standard IntelliJ interface for toolbar buttons with custom Swing rendering). The custom component is a plain `JButton` styled to look like an IntelliJ toolbar button. `update()` syncs enabled state to the component via `CustomComponentAction.COMPONENT_KEY`. The popup is a plain `JPopupMenu` populated from `NavigationHistory`.

**Tech Stack:** Kotlin, IntelliJ Platform 2024.1+ (`CustomComponentAction`, `JBUI`, `ActionToolbar`), Swing (`JButton`, `JPopupMenu`, `MouseAdapter`)

---

### Task 1: Delete the old action group files

No tests for this task — just deletion.

**Files:**
- Delete: `src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/actions/BackActionGroup.kt`
- Delete: `src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/actions/ForwardActionGroup.kt`

**Step 1: Delete both files**

```bash
rm "src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/actions/BackActionGroup.kt"
rm "src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/actions/ForwardActionGroup.kt"
```

**Step 2: Commit**

```bash
git add -A
git commit -m "refactor: remove BackActionGroup and ForwardActionGroup"
```

---

### Task 2: Create `BackAction`

**Files:**
- Create: `src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/actions/BackAction.kt`

**Step 1: Create the file with this exact content**

```kotlin
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
 * Toolbar button for back navigation.
 * Left-click goes back one step. Right-click shows back history popup.
 */
class BackAction(
    private val browserPanel: DocsetBrowserPanel
) : AnAction("Back", "Go back (right-click for history)", AllIcons.Actions.Back), CustomComponentAction {

    override fun actionPerformed(e: AnActionEvent) {
        browserPanel.goBack()
    }

    override fun update(e: AnActionEvent) {
        val enabled = browserPanel.canGoBack()
        e.presentation.isEnabled = enabled
        val component = e.presentation.getClientProperty(CustomComponentAction.COMPONENT_KEY) as? JComponent
        component?.isEnabled = enabled
    }

    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        val button = JButton(AllIcons.Actions.Back).apply {
            isContentAreaFilled = false
            isBorderPainted = false
            isOpaque = false
            border = JBUI.Borders.empty(4)
            preferredSize = ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
            toolTipText = "Back (right-click for history)"
            isEnabled = presentation.isEnabled
        }

        button.addActionListener {
            browserPanel.goBack()
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
        val entries = browserPanel.getHistory().getBackEntries()
        if (entries.isEmpty()) return
        val menu = JPopupMenu()
        entries.forEachIndexed { index, entry ->
            val item = JMenuItem(entry.title)
            item.addActionListener { browserPanel.goBackTo(index) }
            menu.add(item)
        }
        menu.show(component, 0, component.height)
    }
}
```

**Step 2: Verify the project compiles**

```bash
JAVA_HOME="/c/Program Files/JetBrains/CLion 2022.3.1/jbr" ./gradlew compileKotlin
```

Expected: `BUILD SUCCESSFUL`

**Step 3: Commit**

```bash
git add src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/actions/BackAction.kt
git commit -m "feat: add BackAction with left-click navigate, right-click history popup"
```

---

### Task 3: Create `ForwardAction`

**Files:**
- Create: `src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/actions/ForwardAction.kt`

**Step 1: Create the file with this exact content**

```kotlin
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

        button.addActionListener {
            browserPanel.goForward()
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
```

**Step 2: Verify the project compiles**

```bash
JAVA_HOME="/c/Program Files/JetBrains/CLion 2022.3.1/jbr" ./gradlew compileKotlin
```

Expected: `BUILD SUCCESSFUL`

**Step 3: Commit**

```bash
git add src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/actions/ForwardAction.kt
git commit -m "feat: add ForwardAction with left-click navigate, right-click history popup"
```

---

### Task 4: Wire up `DocsetNavigationToolbar`

**Files:**
- Modify: `src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/DocsetNavigationToolbar.kt`

**Step 1: Update the imports and usages**

In [DocsetNavigationToolbar.kt](src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/DocsetNavigationToolbar.kt), replace:

```kotlin
import io.github.vanillafairy.docsetviewer.editor.actions.BackActionGroup
import io.github.vanillafairy.docsetviewer.editor.actions.ForwardActionGroup
```

with:

```kotlin
import io.github.vanillafairy.docsetviewer.editor.actions.BackAction
import io.github.vanillafairy.docsetviewer.editor.actions.ForwardAction
```

Also replace the usages inside the `apply` block:

```kotlin
// old
add(BackActionGroup(browserPanel))
add(ForwardActionGroup(browserPanel))

// new
add(BackAction(browserPanel))
add(ForwardAction(browserPanel))
```

**Step 2: Build and run tests**

```bash
JAVA_HOME="/c/Program Files/JetBrains/CLion 2022.3.1/jbr" ./gradlew build
```

Expected: `BUILD SUCCESSFUL` with all tests passing.

**Step 3: Commit**

```bash
git add src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/DocsetNavigationToolbar.kt
git commit -m "refactor: use BackAction and ForwardAction in DocsetNavigationToolbar"
```

---

### Task 5: Manual smoke test

**Step 1: Run the IDE**

```bash
JAVA_HOME="/c/Program Files/JetBrains/CLion 2022.3.1/jbr" ./gradlew runIde
```

**Step 2: Verify these behaviors**

1. Open a docset entry — toolbar shows Back and Forward icon buttons
2. Navigate to a few pages — Back button becomes enabled
3. Left-click Back — navigates back one page
4. Right-click Back — popup appears listing previous pages; clicking an entry jumps to it
5. After jumping back several pages, right-click Forward — popup shows forward history
6. When no back history exists, Back button is greyed out and right-click shows no popup
