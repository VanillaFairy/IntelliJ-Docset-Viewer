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

    @Test
    fun `initial state has no current URL`() {
        assertThat(history.current()).isNull()
        assertThat(history.canGoBack()).isFalse()
        assertThat(history.canGoForward()).isFalse()
    }

    @Test
    fun `push sets current URL`() {
        history.push("http://example.com/page1")

        assertThat(history.current()).isEqualTo("http://example.com/page1")
    }

    @Test
    fun `canGoBack returns true after multiple pushes`() {
        history.push("http://example.com/page1")
        assertThat(history.canGoBack()).isFalse()

        history.push("http://example.com/page2")
        assertThat(history.canGoBack()).isTrue()
    }

    @Test
    fun `canGoForward returns true only after going back`() {
        history.push("http://example.com/page1")
        history.push("http://example.com/page2")
        assertThat(history.canGoForward()).isFalse()

        history.goBack()
        assertThat(history.canGoForward()).isTrue()
    }

    @Test
    fun `goBack returns previous URL`() {
        history.push("http://example.com/page1")
        history.push("http://example.com/page2")
        history.push("http://example.com/page3")

        val previous = history.goBack()

        assertThat(previous).isEqualTo("http://example.com/page2")
        assertThat(history.current()).isEqualTo("http://example.com/page2")
    }

    @Test
    fun `goForward returns next URL after going back`() {
        history.push("http://example.com/page1")
        history.push("http://example.com/page2")
        history.goBack()

        val next = history.goForward()

        assertThat(next).isEqualTo("http://example.com/page2")
        assertThat(history.current()).isEqualTo("http://example.com/page2")
    }

    @Test
    fun `push clears forward history`() {
        history.push("http://example.com/page1")
        history.push("http://example.com/page2")
        history.push("http://example.com/page3")
        history.goBack()
        history.goBack()

        assertThat(history.canGoForward()).isTrue()
        assertThat(history.forwardCount()).isEqualTo(2)

        history.push("http://example.com/page-new")

        assertThat(history.canGoForward()).isFalse()
        assertThat(history.forwardCount()).isEqualTo(0)
    }

    @Test
    fun `push same URL does not add to history`() {
        history.push("http://example.com/page1")
        history.push("http://example.com/page1")
        history.push("http://example.com/page1")

        assertThat(history.canGoBack()).isFalse()
        assertThat(history.backCount()).isEqualTo(0)
    }

    @Test
    fun `goBack returns null when no back history`() {
        history.push("http://example.com/page1")

        val result = history.goBack()

        assertThat(result).isNull()
        assertThat(history.current()).isEqualTo("http://example.com/page1")
    }

    @Test
    fun `goForward returns null when no forward history`() {
        history.push("http://example.com/page1")
        history.push("http://example.com/page2")

        val result = history.goForward()

        assertThat(result).isNull()
        assertThat(history.current()).isEqualTo("http://example.com/page2")
    }

    @Test
    fun `clear resets all history`() {
        history.push("http://example.com/page1")
        history.push("http://example.com/page2")
        history.push("http://example.com/page3")
        history.goBack()

        history.clear()

        assertThat(history.current()).isNull()
        assertThat(history.canGoBack()).isFalse()
        assertThat(history.canGoForward()).isFalse()
        assertThat(history.backCount()).isEqualTo(0)
        assertThat(history.forwardCount()).isEqualTo(0)
    }

    @Test
    fun `backCount returns correct value`() {
        history.push("http://example.com/page1")
        assertThat(history.backCount()).isEqualTo(0)

        history.push("http://example.com/page2")
        assertThat(history.backCount()).isEqualTo(1)

        history.push("http://example.com/page3")
        assertThat(history.backCount()).isEqualTo(2)

        history.goBack()
        assertThat(history.backCount()).isEqualTo(1)
    }

    @Test
    fun `forwardCount returns correct value`() {
        history.push("http://example.com/page1")
        history.push("http://example.com/page2")
        history.push("http://example.com/page3")

        assertThat(history.forwardCount()).isEqualTo(0)

        history.goBack()
        assertThat(history.forwardCount()).isEqualTo(1)

        history.goBack()
        assertThat(history.forwardCount()).isEqualTo(2)

        history.goForward()
        assertThat(history.forwardCount()).isEqualTo(1)
    }
}
