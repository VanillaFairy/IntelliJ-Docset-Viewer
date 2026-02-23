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
