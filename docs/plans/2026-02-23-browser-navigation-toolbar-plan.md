# Browser Navigation Toolbar Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add enhanced browser navigation (split-button Back/Forward with history dropdown, Reload, Home) to both toolwindow panel and editor tabs, with mouse button 4/5 support.

**Architecture:** Enhance `NavigationHistory` with titled entries and multi-step jump. Create a shared `DocsetNavigationToolbar` component with split-button action groups. Inject JavaScript via `JBCefJSQuery` for mouse button 4/5 interception.

**Tech Stack:** Kotlin, IntelliJ Platform SDK (ActionToolbar, DefaultActionGroup, JBCefJSQuery), JCEF/CEF

---

### Task 1: Enhance NavigationHistory with HistoryEntry

**Files:**
- Modify: `src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/NavigationHistory.kt`
- Modify: `src/test/kotlin/io/github/vanillafairy/docsetviewer/editor/NavigationHistoryTest.kt`

**Step 1: Write failing tests for HistoryEntry-based API**

Add these tests to `NavigationHistoryTest.kt`. The existing tests use `push("url")` with a single String argument — all of them must be updated to use `push("url", "title")` with two arguments. Update the class like so:

```kotlin
package io.github.vanillafairy.docsetviewer.editor

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class NavigationHistoryTest {

    private lateinit var history: NavigationHistory

    @Before
    fun setUp() {
        history = NavigationHistory()
    }

    // === Existing tests updated for HistoryEntry ===

    @Test
    fun `initial state has no current entry`() {
        assertThat(history.currentEntry()).isNull()
        assertThat(history.canGoBack()).isFalse()
        assertThat(history.canGoForward()).isFalse()
    }

    @Test
    fun `push sets current entry`() {
        history.push("http://example.com/page1", "Page 1")

        assertThat(history.currentEntry()?.url).isEqualTo("http://example.com/page1")
        assertThat(history.currentEntry()?.title).isEqualTo("Page 1")
    }

    @Test
    fun `canGoBack returns true after multiple pushes`() {
        history.push("http://example.com/page1", "Page 1")
        assertThat(history.canGoBack()).isFalse()

        history.push("http://example.com/page2", "Page 2")
        assertThat(history.canGoBack()).isTrue()
    }

    @Test
    fun `canGoForward returns true only after going back`() {
        history.push("http://example.com/page1", "Page 1")
        history.push("http://example.com/page2", "Page 2")
        assertThat(history.canGoForward()).isFalse()

        history.goBack()
        assertThat(history.canGoForward()).isTrue()
    }

    @Test
    fun `goBack returns previous entry`() {
        history.push("http://example.com/page1", "Page 1")
        history.push("http://example.com/page2", "Page 2")
        history.push("http://example.com/page3", "Page 3")

        val previous = history.goBack()

        assertThat(previous?.url).isEqualTo("http://example.com/page2")
        assertThat(previous?.title).isEqualTo("Page 2")
        assertThat(history.currentEntry()?.url).isEqualTo("http://example.com/page2")
    }

    @Test
    fun `goForward returns next entry after going back`() {
        history.push("http://example.com/page1", "Page 1")
        history.push("http://example.com/page2", "Page 2")
        history.goBack()

        val next = history.goForward()

        assertThat(next?.url).isEqualTo("http://example.com/page2")
        assertThat(next?.title).isEqualTo("Page 2")
        assertThat(history.currentEntry()?.url).isEqualTo("http://example.com/page2")
    }

    @Test
    fun `push clears forward history`() {
        history.push("http://example.com/page1", "Page 1")
        history.push("http://example.com/page2", "Page 2")
        history.push("http://example.com/page3", "Page 3")
        history.goBack()
        history.goBack()

        assertThat(history.canGoForward()).isTrue()
        assertThat(history.forwardCount()).isEqualTo(2)

        history.push("http://example.com/page-new", "New Page")

        assertThat(history.canGoForward()).isFalse()
        assertThat(history.forwardCount()).isEqualTo(0)
    }

    @Test
    fun `push same URL does not add to history`() {
        history.push("http://example.com/page1", "Page 1")
        history.push("http://example.com/page1", "Page 1")
        history.push("http://example.com/page1", "Page 1")

        assertThat(history.canGoBack()).isFalse()
        assertThat(history.backCount()).isEqualTo(0)
    }

    @Test
    fun `goBack returns null when no back history`() {
        history.push("http://example.com/page1", "Page 1")

        val result = history.goBack()

        assertThat(result).isNull()
        assertThat(history.currentEntry()?.url).isEqualTo("http://example.com/page1")
    }

    @Test
    fun `goForward returns null when no forward history`() {
        history.push("http://example.com/page1", "Page 1")
        history.push("http://example.com/page2", "Page 2")

        val result = history.goForward()

        assertThat(result).isNull()
        assertThat(history.currentEntry()?.url).isEqualTo("http://example.com/page2")
    }

    @Test
    fun `clear resets all history`() {
        history.push("http://example.com/page1", "Page 1")
        history.push("http://example.com/page2", "Page 2")
        history.push("http://example.com/page3", "Page 3")
        history.goBack()

        history.clear()

        assertThat(history.currentEntry()).isNull()
        assertThat(history.canGoBack()).isFalse()
        assertThat(history.canGoForward()).isFalse()
        assertThat(history.backCount()).isEqualTo(0)
        assertThat(history.forwardCount()).isEqualTo(0)
    }

    @Test
    fun `backCount returns correct value`() {
        history.push("http://example.com/page1", "Page 1")
        assertThat(history.backCount()).isEqualTo(0)

        history.push("http://example.com/page2", "Page 2")
        assertThat(history.backCount()).isEqualTo(1)

        history.push("http://example.com/page3", "Page 3")
        assertThat(history.backCount()).isEqualTo(2)

        history.goBack()
        assertThat(history.backCount()).isEqualTo(1)
    }

    @Test
    fun `forwardCount returns correct value`() {
        history.push("http://example.com/page1", "Page 1")
        history.push("http://example.com/page2", "Page 2")
        history.push("http://example.com/page3", "Page 3")

        assertThat(history.forwardCount()).isEqualTo(0)

        history.goBack()
        assertThat(history.forwardCount()).isEqualTo(1)

        history.goBack()
        assertThat(history.forwardCount()).isEqualTo(2)

        history.goForward()
        assertThat(history.forwardCount()).isEqualTo(1)
    }

    // === New tests for enhanced features ===

    @Test
    fun `getBackEntries returns entries in most-recent-first order`() {
        history.push("http://example.com/page1", "Page 1")
        history.push("http://example.com/page2", "Page 2")
        history.push("http://example.com/page3", "Page 3")

        val entries = history.getBackEntries()

        assertThat(entries).hasSize(2)
        assertThat(entries[0].title).isEqualTo("Page 2")
        assertThat(entries[1].title).isEqualTo("Page 1")
    }

    @Test
    fun `getForwardEntries returns entries in order`() {
        history.push("http://example.com/page1", "Page 1")
        history.push("http://example.com/page2", "Page 2")
        history.push("http://example.com/page3", "Page 3")
        history.goBack()
        history.goBack()

        val entries = history.getForwardEntries()

        assertThat(entries).hasSize(2)
        assertThat(entries[0].title).isEqualTo("Page 2")
        assertThat(entries[1].title).isEqualTo("Page 3")
    }

    @Test
    fun `goBackTo jumps multiple steps and moves intermediates to forward`() {
        history.push("http://example.com/page1", "Page 1")
        history.push("http://example.com/page2", "Page 2")
        history.push("http://example.com/page3", "Page 3")
        history.push("http://example.com/page4", "Page 4")

        // index 0 = most recent back entry (Page 3), index 2 = oldest (Page 1)
        val result = history.goBackTo(2)

        assertThat(result?.url).isEqualTo("http://example.com/page1")
        assertThat(history.currentEntry()?.url).isEqualTo("http://example.com/page1")
        // Forward stack should have: Page 2, Page 3, Page 4
        assertThat(history.forwardCount()).isEqualTo(3)
        assertThat(history.getForwardEntries().map { it.title })
            .containsExactly("Page 2", "Page 3", "Page 4")
    }

    @Test
    fun `goForwardTo jumps multiple steps and moves intermediates to back`() {
        history.push("http://example.com/page1", "Page 1")
        history.push("http://example.com/page2", "Page 2")
        history.push("http://example.com/page3", "Page 3")
        history.push("http://example.com/page4", "Page 4")
        history.goBack()
        history.goBack()
        history.goBack()
        // Now at Page 1, forward has: Page 2, Page 3, Page 4

        val result = history.goForwardTo(2)

        assertThat(result?.url).isEqualTo("http://example.com/page4")
        assertThat(history.currentEntry()?.url).isEqualTo("http://example.com/page4")
        // Back stack should have: Page 1, Page 2, Page 3
        assertThat(history.backCount()).isEqualTo(3)
        assertThat(history.getBackEntries().map { it.title })
            .containsExactly("Page 3", "Page 2", "Page 1")
    }

    @Test
    fun `goBackTo with invalid index returns null`() {
        history.push("http://example.com/page1", "Page 1")
        history.push("http://example.com/page2", "Page 2")

        assertThat(history.goBackTo(5)).isNull()
        assertThat(history.currentEntry()?.url).isEqualTo("http://example.com/page2")
    }

    @Test
    fun `goForwardTo with invalid index returns null`() {
        history.push("http://example.com/page1", "Page 1")
        history.push("http://example.com/page2", "Page 2")
        history.goBack()

        assertThat(history.goForwardTo(5)).isNull()
        assertThat(history.currentEntry()?.url).isEqualTo("http://example.com/page1")
    }

    @Test
    fun `push updates title for same URL with different title`() {
        history.push("http://example.com/page1", "Loading...")
        history.push("http://example.com/page1", "Actual Title")

        assertThat(history.currentEntry()?.title).isEqualTo("Actual Title")
        assertThat(history.backCount()).isEqualTo(0)
    }

    @Test
    fun `getBackEntries returns empty list when no back history`() {
        history.push("http://example.com/page1", "Page 1")
        assertThat(history.getBackEntries()).isEmpty()
    }

    @Test
    fun `getForwardEntries returns empty list when no forward history`() {
        history.push("http://example.com/page1", "Page 1")
        assertThat(history.getForwardEntries()).isEmpty()
    }

    @Test
    fun `history respects maxSize limit`() {
        val smallHistory = NavigationHistory(maxSize = 3)
        smallHistory.push("http://example.com/page1", "Page 1")
        smallHistory.push("http://example.com/page2", "Page 2")
        smallHistory.push("http://example.com/page3", "Page 3")
        smallHistory.push("http://example.com/page4", "Page 4")

        assertThat(smallHistory.backCount()).isEqualTo(3)
        // Oldest entry (Page 1) should have been evicted
        assertThat(smallHistory.getBackEntries().map { it.title })
            .containsExactly("Page 3", "Page 2", "Page 1")
    }
}
```

**Step 2: Run tests to verify they fail**

Run: `JAVA_HOME="/c/Program Files/JetBrains/CLion 2022.3.1/jbr" ./gradlew test --tests "io.github.vanillafairy.docsetviewer.editor.NavigationHistoryTest" -q`
Expected: Compilation errors — `push()` signature changed, `current()` renamed, `HistoryEntry` doesn't exist.

**Step 3: Implement NavigationHistory with HistoryEntry**

Replace the full content of `src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/NavigationHistory.kt`:

```kotlin
package io.github.vanillafairy.docsetviewer.editor

/**
 * An entry in the navigation history with a URL and page title.
 */
data class HistoryEntry(val url: String, val title: String)

/**
 * Manages navigation history for the docset browser.
 * Supports back/forward navigation through visited URLs with titles.
 * History is bounded to prevent unbounded memory growth.
 */
class NavigationHistory(
    private val maxSize: Int = DEFAULT_MAX_SIZE
) {
    companion object {
        const val DEFAULT_MAX_SIZE = 100
    }

    private val backStack = ArrayDeque<HistoryEntry>()
    private val forwardStack = ArrayDeque<HistoryEntry>()
    private var current: HistoryEntry? = null

    /**
     * Pushes a new entry to the history.
     * If the URL matches the current entry, updates the title only.
     * Clears forward history when navigating to a new page.
     * Removes oldest entries if history exceeds max size.
     */
    fun push(url: String, title: String) {
        if (url == current?.url) {
            // Update title for the same URL (e.g., title loaded after initial push)
            current = current?.copy(title = title)
            return
        }

        current?.let { backStack.addLast(it) }
        current = HistoryEntry(url, title)
        forwardStack.clear()

        while (backStack.size > maxSize) {
            backStack.removeFirst()
        }
    }

    /**
     * Gets the current history entry.
     */
    fun currentEntry(): HistoryEntry? = current

    /**
     * Returns true if back navigation is available.
     */
    fun canGoBack(): Boolean = backStack.isNotEmpty()

    /**
     * Returns true if forward navigation is available.
     */
    fun canGoForward(): Boolean = forwardStack.isNotEmpty()

    /**
     * Navigates back one step and returns the previous entry.
     * Returns null if no back history available.
     */
    fun goBack(): HistoryEntry? {
        if (!canGoBack()) return null

        current?.let { forwardStack.addFirst(it) }
        current = backStack.removeLast()
        return current
    }

    /**
     * Navigates forward one step and returns the next entry.
     * Returns null if no forward history available.
     */
    fun goForward(): HistoryEntry? {
        if (!canGoForward()) return null

        current?.let { backStack.addLast(it) }
        current = forwardStack.removeFirst()
        return current
    }

    /**
     * Returns back history entries in most-recent-first order.
     */
    fun getBackEntries(): List<HistoryEntry> = backStack.reversed()

    /**
     * Returns forward history entries in order.
     */
    fun getForwardEntries(): List<HistoryEntry> = forwardStack.toList()

    /**
     * Jumps back to the entry at the given index (0 = most recent back entry).
     * Intermediate entries and current are moved to the forward stack.
     * Returns null if index is out of range.
     */
    fun goBackTo(index: Int): HistoryEntry? {
        if (index < 0 || index >= backStack.size) return null

        // Move current to forward stack
        current?.let { forwardStack.addFirst(it) }

        // The index is in "most recent first" order, so we need to pop (index + 1) entries
        // from the back stack, pushing all but the last to the forward stack
        val targetBackStackIndex = backStack.size - 1 - index
        for (i in backStack.size - 1 downTo targetBackStackIndex + 1) {
            forwardStack.addFirst(backStack.removeLast())
        }
        current = backStack.removeLast()
        return current
    }

    /**
     * Jumps forward to the entry at the given index (0 = next forward entry).
     * Intermediate entries and current are moved to the back stack.
     * Returns null if index is out of range.
     */
    fun goForwardTo(index: Int): HistoryEntry? {
        if (index < 0 || index >= forwardStack.size) return null

        // Move current to back stack
        current?.let { backStack.addLast(it) }

        // Pop (index + 1) entries from the forward stack, pushing all but the last to back stack
        for (i in 0 until index) {
            backStack.addLast(forwardStack.removeFirst())
        }
        current = forwardStack.removeFirst()
        return current
    }

    /**
     * Clears all history.
     */
    fun clear() {
        backStack.clear()
        forwardStack.clear()
        current = null
    }

    /**
     * Gets the number of items in back history.
     */
    fun backCount(): Int = backStack.size

    /**
     * Gets the number of items in forward history.
     */
    fun forwardCount(): Int = forwardStack.size
}
```

**Step 4: Run tests to verify they pass**

Run: `JAVA_HOME="/c/Program Files/JetBrains/CLion 2022.3.1/jbr" ./gradlew test --tests "io.github.vanillafairy.docsetviewer.editor.NavigationHistoryTest" -q`
Expected: All tests PASS.

**Step 5: Commit**

```bash
git add src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/NavigationHistory.kt src/test/kotlin/io/github/vanillafairy/docsetviewer/editor/NavigationHistoryTest.kt
git commit -m "feat: enhance NavigationHistory with HistoryEntry, multi-step jump, and entry listing"
```

---

### Task 2: Update DocsetBrowserPanel for HistoryEntry and title capture

**Files:**
- Modify: `src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/DocsetBrowserPanel.kt`

**Step 1: Update DocsetBrowserPanel to use HistoryEntry API**

The changes to `DocsetBrowserPanel.kt` are:

1. **Add import** for `JBCefJSQuery` at the top:
   ```kotlin
   import com.intellij.ui.jcef.JBCefJSQuery
   ```

2. **Add `homeUrl` property** and expose `history` for the toolbar:
   ```kotlin
   var homeUrl: String? = null
       private set
   ```
   Add a method to get history (needed by toolbar action groups):
   ```kotlin
   fun getHistory(): NavigationHistory = history
   ```

3. **Update `setupBrowser()`** to capture page title via JavaScript callback. Replace the current `onLoadEnd` implementation. The key change: instead of `history.push(url)`, we now use a `JBCefJSQuery` to get the title from JS, then call `history.push(url, title)`:

   Replace the `setupBrowser()` method entirely:

   ```kotlin
   private var titleQuery: JBCefJSQuery? = null

   private fun setupBrowser() {
       val cefBrowser = browser?.cefBrowser ?: return
       val jbBrowser = browser ?: return

       // Create a JS query handler for receiving page titles
       titleQuery = JBCefJSQuery.create(jbBrowser).also { query ->
           query.addHandler { title ->
               val url = cefBrowser.url ?: return@addHandler null
               history.push(url, title)
               loadListener?.invoke(url)
               null
           }
       }

       jbBrowser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
           override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
               if (frame?.isMain == true) {
                   val url = browser?.url ?: return

                   // Push with URL-derived fallback title immediately
                   val fallbackTitle = extractFilenameFromUrl(url)
                   history.push(url, fallbackTitle)
                   loadListener?.invoke(url)

                   // Inject color scheme preference
                   injectColorSchemePreference(browser)

                   // Then get actual page title via JS and update
                   val titleJs = """
                       (function() {
                           var title = document.title || '';
                           if (title) {
                               ${titleQuery!!.inject("title")}
                           }
                       })();
                   """.trimIndent()
                   browser.executeJavaScript(titleJs, url, 0)
               }
           }
       }, cefBrowser)
   }
   ```

4. **Add helper methods** at the bottom of the class (before `companion object`):

   ```kotlin
   private fun extractFilenameFromUrl(url: String): String {
       return try {
           val path = java.net.URI(url).path ?: url
           val filename = path.substringAfterLast('/')
           filename.substringBeforeLast('.').ifEmpty { url }
       } catch (e: Exception) {
           url
       }
   }

   /**
    * Navigates to the home page (docset index).
    */
   fun goHome() {
       homeUrl?.let { browser?.loadURL(it) }
   }

   /**
    * Sets the home URL for this browser panel.
    * Typically the docset's index page.
    */
   fun setHomeUrl(url: String) {
       this.homeUrl = url
   }
   ```

5. **Update `goBack()` and `goForward()`** to use HistoryEntry:

   ```kotlin
   fun goBack() {
       history.goBack()?.let { browser?.loadURL(it.url) }
   }

   fun goForward() {
       history.goForward()?.let { browser?.loadURL(it.url) }
   }
   ```

6. **Add `goBackTo()` and `goForwardTo()`** for dropdown navigation:

   ```kotlin
   fun goBackTo(index: Int) {
       history.goBackTo(index)?.let { browser?.loadURL(it.url) }
   }

   fun goForwardTo(index: Int) {
       history.goForwardTo(index)?.let { browser?.loadURL(it.url) }
   }
   ```

7. **Update `dispose()`** to clean up the JBCefJSQuery:

   ```kotlin
   override fun dispose() {
       titleQuery?.let { Disposer.dispose(it) }
       titleQuery = null
       browser?.let { Disposer.dispose(it) }
       browser = null
   }
   ```

**Step 2: Build to verify compilation**

Run: `JAVA_HOME="/c/Program Files/JetBrains/CLion 2022.3.1/jbr" ./gradlew compileKotlin -q`
Expected: Compilation succeeds. (DocsetFileEditor will also need updating due to the `current()` -> `currentEntry()` rename, but that's handled in Task 4.)

Note: The build may show errors from `DocsetFileEditor.kt` since it still uses the old API. This is expected and fixed in Task 4.

**Step 3: Commit**

```bash
git add src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/DocsetBrowserPanel.kt
git commit -m "feat: add title capture, home URL, and multi-step navigation to DocsetBrowserPanel"
```

---

### Task 3: Create navigation action classes

**Files:**
- Create: `src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/actions/BackActionGroup.kt`
- Create: `src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/actions/ForwardActionGroup.kt`
- Create: `src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/actions/ReloadAction.kt`
- Create: `src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/actions/HomeAction.kt`

**Step 1: Create BackActionGroup**

Create file `src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/actions/BackActionGroup.kt`:

```kotlin
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
```

**Step 2: Create ForwardActionGroup**

Create file `src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/actions/ForwardActionGroup.kt`:

```kotlin
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
```

**Step 3: Create ReloadAction**

Create file `src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/actions/ReloadAction.kt`:

```kotlin
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
```

**Step 4: Create HomeAction**

Create file `src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/actions/HomeAction.kt`:

```kotlin
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
```

**Step 5: Commit**

```bash
git add src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/actions/
git commit -m "feat: add BackActionGroup, ForwardActionGroup, ReloadAction, and HomeAction"
```

---

### Task 4: Create DocsetNavigationToolbar and integrate everywhere

**Files:**
- Create: `src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/DocsetNavigationToolbar.kt`
- Modify: `src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/DocsetFileEditor.kt`
- Modify: `src/main/kotlin/io/github/vanillafairy/docsetviewer/ui/toolwindow/DocsetToolWindowPanel.kt`

**Step 1: Create DocsetNavigationToolbar**

Create file `src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/DocsetNavigationToolbar.kt`:

```kotlin
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
```

**Step 2: Simplify DocsetFileEditor to use DocsetNavigationToolbar**

Replace the entire content of `src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/DocsetFileEditor.kt`:

```kotlin
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
```

**Step 3: Add toolbar to DocsetToolWindowPanel**

In `src/main/kotlin/io/github/vanillafairy/docsetviewer/ui/toolwindow/DocsetToolWindowPanel.kt`, make two changes:

1. Add import at the top:
   ```kotlin
   import io.github.vanillafairy.docsetviewer.editor.DocsetNavigationToolbar
   ```

2. In `setupUI()`, wrap the browser panel in a panel with the toolbar. Replace this section:

   ```kotlin
   // Current code:
   mainSplitter.secondComponent = browserPanel.component
   ```

   With:

   ```kotlin
   // Browser panel with navigation toolbar above it
   val browserWithToolbar = JPanel(BorderLayout())
   val toolbar = DocsetNavigationToolbar(browserPanel, browserWithToolbar)
   browserWithToolbar.add(toolbar.component, BorderLayout.NORTH)
   browserWithToolbar.add(browserPanel.component, BorderLayout.CENTER)
   mainSplitter.secondComponent = browserWithToolbar
   ```

3. In `openEntryInBrowser()`, set the home URL when loading a docset entry. Replace the method:

   ```kotlin
   private fun openEntryInBrowser(entry: DocsetEntry) {
       val docset = indexService.getDocset(entry.docsetIdentifier) ?: return

       val fullPath = docset.resolveDocumentPath(entry.path).toFile()

       if (fullPath.exists()) {
           // Set home URL for this docset
           val indexFile = docset.documentsPath.resolve("index.html").toFile()
           if (indexFile.exists()) {
               browserPanel.setHomeUrl(indexFile.toURI().toString())
           }

           browserPanel.loadUrl(fullPath.toURI().toString())
       }
   }
   ```

**Step 4: Build to verify everything compiles**

Run: `JAVA_HOME="/c/Program Files/JetBrains/CLion 2022.3.1/jbr" ./gradlew compileKotlin -q`
Expected: Compilation succeeds with no errors.

**Step 5: Run all tests**

Run: `JAVA_HOME="/c/Program Files/JetBrains/CLion 2022.3.1/jbr" ./gradlew test -q`
Expected: All tests PASS.

**Step 6: Commit**

```bash
git add src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/DocsetNavigationToolbar.kt src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/DocsetFileEditor.kt src/main/kotlin/io/github/vanillafairy/docsetviewer/ui/toolwindow/DocsetToolWindowPanel.kt
git commit -m "feat: add shared DocsetNavigationToolbar, integrate into editor and toolwindow"
```

---

### Task 5: Add mouse button 4/5 support via JBCefJSQuery

**Files:**
- Modify: `src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/DocsetBrowserPanel.kt`

**Step 1: Add mouse button interception JavaScript**

In `DocsetBrowserPanel.kt`, add a new `JBCefJSQuery` for mouse button events. Add a new field alongside `titleQuery`:

```kotlin
private var mouseBackQuery: JBCefJSQuery? = null
private var mouseForwardQuery: JBCefJSQuery? = null
```

In `setupBrowser()`, after creating the `titleQuery`, create the mouse button queries:

```kotlin
mouseBackQuery = JBCefJSQuery.create(jbBrowser).also { query ->
    query.addHandler {
        goBack()
        null
    }
}

mouseForwardQuery = JBCefJSQuery.create(jbBrowser).also { query ->
    query.addHandler {
        goForward()
        null
    }
}
```

In the `onLoadEnd` handler, after the title JavaScript injection, add mouse button interception:

```kotlin
// Inject mouse button 4/5 (back/forward) handlers
val mouseJs = """
    (function() {
        if (window.__docsetMouseHandlerInstalled) return;
        window.__docsetMouseHandlerInstalled = true;
        document.addEventListener('mouseup', function(e) {
            if (e.button === 3) {
                e.preventDefault();
                e.stopPropagation();
                ${mouseBackQuery!!.inject("'back'")}
            } else if (e.button === 4) {
                e.preventDefault();
                e.stopPropagation();
                ${mouseForwardQuery!!.inject("'forward'")}
            }
        }, true);
    })();
""".trimIndent()
browser.executeJavaScript(mouseJs, url, 0)
```

The `window.__docsetMouseHandlerInstalled` guard prevents duplicate handlers if `onLoadEnd` fires multiple times for the same page.

**Step 2: Update `dispose()` to clean up mouse queries**

```kotlin
override fun dispose() {
    mouseBackQuery?.let { Disposer.dispose(it) }
    mouseBackQuery = null
    mouseForwardQuery?.let { Disposer.dispose(it) }
    mouseForwardQuery = null
    titleQuery?.let { Disposer.dispose(it) }
    titleQuery = null
    browser?.let { Disposer.dispose(it) }
    browser = null
}
```

**Step 3: Build to verify compilation**

Run: `JAVA_HOME="/c/Program Files/JetBrains/CLion 2022.3.1/jbr" ./gradlew compileKotlin -q`
Expected: Compilation succeeds.

**Step 4: Run all tests**

Run: `JAVA_HOME="/c/Program Files/JetBrains/CLion 2022.3.1/jbr" ./gradlew test -q`
Expected: All tests PASS.

**Step 5: Commit**

```bash
git add src/main/kotlin/io/github/vanillafairy/docsetviewer/editor/DocsetBrowserPanel.kt
git commit -m "feat: add mouse button 4/5 (back/forward) support via JBCefJSQuery"
```

---

### Task 6: Manual testing and final verification

**Step 1: Build the plugin**

Run: `JAVA_HOME="/c/Program Files/JetBrains/CLion 2022.3.1/jbr" ./gradlew buildPlugin -q`
Expected: Plugin ZIP built successfully in `build/distributions/`.

**Step 2: Run IDE for manual testing**

Run: `JAVA_HOME="/c/Program Files/JetBrains/CLion 2022.3.1/jbr" ./gradlew runIde`

Manual test checklist:
- [ ] Open a docset and navigate between entries
- [ ] Verify Back button goes back one step
- [ ] Verify Forward button goes forward one step
- [ ] Verify Back dropdown arrow shows history with page titles
- [ ] Verify clicking a dropdown entry jumps directly to that page
- [ ] Verify Forward dropdown shows forward history after going back
- [ ] Verify Reload button refreshes the page
- [ ] Verify Home button navigates to docset index page
- [ ] Verify Home button is disabled when no home URL is set
- [ ] Verify mouse button 4 (back) works
- [ ] Verify mouse button 5 (forward) works
- [ ] Verify toolbar appears in both the toolwindow panel and editor tabs
- [ ] Verify buttons are disabled when navigation is not available

**Step 3: Final commit (if any fixes needed)**

```bash
git add -A
git commit -m "fix: address issues found during manual testing"
```
