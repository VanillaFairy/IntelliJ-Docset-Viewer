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
