package io.github.vanillafairy.docsetviewer.editor

/**
 * Manages navigation history for the docset browser.
 * Supports back/forward navigation through visited URLs.
 * History is bounded to prevent unbounded memory growth.
 */
class NavigationHistory(
    private val maxSize: Int = DEFAULT_MAX_SIZE
) {
    companion object {
        const val DEFAULT_MAX_SIZE = 100
    }

    private val backStack = ArrayDeque<String>()
    private val forwardStack = ArrayDeque<String>()
    private var currentUrl: String? = null

    /**
     * Pushes a new URL to the history.
     * Clears forward history when navigating to a new page.
     * Removes oldest entries if history exceeds max size.
     */
    fun push(url: String) {
        if (url == currentUrl) return

        currentUrl?.let { backStack.addLast(it) }
        currentUrl = url
        forwardStack.clear()

        // Limit history size to prevent unbounded memory growth
        while (backStack.size > maxSize) {
            backStack.removeFirst()
        }
    }

    /**
     * Gets the current URL.
     */
    fun current(): String? = currentUrl

    /**
     * Returns true if back navigation is available.
     */
    fun canGoBack(): Boolean = backStack.isNotEmpty()

    /**
     * Returns true if forward navigation is available.
     */
    fun canGoForward(): Boolean = forwardStack.isNotEmpty()

    /**
     * Navigates back and returns the previous URL.
     * Returns null if no back history available.
     */
    fun goBack(): String? {
        if (!canGoBack()) return null

        currentUrl?.let { forwardStack.addFirst(it) }
        currentUrl = backStack.removeLast()
        return currentUrl
    }

    /**
     * Navigates forward and returns the next URL.
     * Returns null if no forward history available.
     */
    fun goForward(): String? {
        if (!canGoForward()) return null

        currentUrl?.let { backStack.addLast(it) }
        currentUrl = forwardStack.removeFirst()
        return currentUrl
    }

    /**
     * Clears all history.
     */
    fun clear() {
        backStack.clear()
        forwardStack.clear()
        currentUrl = null
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
