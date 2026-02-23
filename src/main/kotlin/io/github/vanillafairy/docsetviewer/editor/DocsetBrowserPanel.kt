package io.github.vanillafairy.docsetviewer.editor

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefJSQuery
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * Panel containing JCEF browser for rendering docset HTML content.
 */
class DocsetBrowserPanel(
    private val parentDisposable: Disposable
) : JPanel(BorderLayout()), Disposable {

    private var browser: JBCefBrowser? = null
    private val history = NavigationHistory()
    private var loadListener: ((String) -> Unit)? = null
    private val navigationListeners = mutableListOf<() -> Unit>()
    private var titleQuery: JBCefJSQuery? = null

    var homeUrl: String? = null
        private set

    val component: JComponent
        get() = this

    init {
        Disposer.register(parentDisposable, this)

        if (JBCefApp.isSupported()) {
            browser = JBCefBrowser()
            setupBrowser()
            setupMouseNavigation()
            add(browser!!.component, BorderLayout.CENTER)
        } else {
            add(createFallbackPanel(), BorderLayout.CENTER)
        }
    }

    private fun setupBrowser() {
        val cefBrowser = browser?.cefBrowser ?: return
        val jbBrowser = browser ?: return

        // Create a JS query handler for receiving page titles
        titleQuery = JBCefJSQuery.create(jbBrowser).also { query ->
            query.addHandler { title ->
                val url = cefBrowser.url ?: return@addHandler null
                history.push(url, title)
                notifyNavigationListeners()
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
                    notifyNavigationListeners()
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

    /**
     * Intercepts mouse button 4/5 (back/forward) at the AWT level
     * to prevent IntelliJ from handling them as editor navigation.
     */
    private fun setupMouseNavigation() {
        val browserComponent = browser?.component ?: return
        browserComponent.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                when (e.button) {
                    4 -> {
                        e.consume()
                        goBack()
                    }
                    5 -> {
                        e.consume()
                        goForward()
                    }
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                if (e.button == 4 || e.button == 5) {
                    e.consume()
                }
            }
        })
    }

    private fun notifyNavigationListeners() {
        navigationListeners.forEach { it() }
    }

    /**
     * Injects JavaScript to apply the correct color scheme based on IDE theme.
     * Since we can't use DevTools Protocol to emulate prefers-color-scheme,
     * we extract dark mode CSS rules and apply them directly.
     */
    private fun injectColorSchemePreference(browser: CefBrowser) {
        val isDark = !JBColor.isBright()

        if (!isDark) {
            // Light mode - nothing to do, that's usually the default
            return
        }

        val js = """
            (function() {
                // Override matchMedia for any JS that checks it
                const originalMatchMedia = window.matchMedia;
                window.matchMedia = function(query) {
                    if (query === '(prefers-color-scheme: dark)') {
                        return {
                            matches: true,
                            media: query,
                            onchange: null,
                            addListener: function(cb) { },
                            removeListener: function() {},
                            addEventListener: function(type, cb) { },
                            removeEventListener: function() {},
                            dispatchEvent: function() { return true; }
                        };
                    }
                    if (query === '(prefers-color-scheme: light)') {
                        return {
                            matches: false,
                            media: query,
                            onchange: null,
                            addListener: function() {},
                            removeListener: function() {},
                            addEventListener: function() {},
                            removeEventListener: function() {},
                            dispatchEvent: function() { return true; }
                        };
                    }
                    return originalMatchMedia.call(window, query);
                };

                // Extract and apply dark mode CSS rules
                const darkRules = [];

                for (const sheet of document.styleSheets) {
                    try {
                        if (!sheet.cssRules) continue;

                        for (const rule of sheet.cssRules) {
                            // Check for @media (prefers-color-scheme: dark) rules
                            if (rule instanceof CSSMediaRule) {
                                const mediaText = rule.conditionText || rule.media.mediaText;
                                if (mediaText && mediaText.includes('prefers-color-scheme') &&
                                    mediaText.includes('dark')) {
                                    // Extract the rules inside the media query
                                    for (const innerRule of rule.cssRules) {
                                        darkRules.push(innerRule.cssText);
                                    }
                                }
                            }
                        }
                    } catch (e) {
                        // Cross-origin stylesheets will throw, ignore them
                    }
                }

                // Apply dark mode rules if any were found
                if (darkRules.length > 0) {
                    const style = document.createElement('style');
                    style.id = 'intellij-dark-mode';
                    style.textContent = darkRules.join('\n');
                    document.head.appendChild(style);
                }

                // Also set color-scheme for native elements (scrollbars, form controls)
                document.documentElement.style.colorScheme = 'dark';
            })();
        """.trimIndent()

        browser.executeJavaScript(js, browser.url, 0)
    }

    private fun createFallbackPanel(): JComponent {
        return JPanel(BorderLayout()).apply {
            add(JLabel(
                "<html><center>JCEF is not supported on this platform.<br>" +
                        "Please use an IDE with JCEF support to view docsets.</center></html>",
                SwingConstants.CENTER
            ), BorderLayout.CENTER)
        }
    }

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
     * Loads a URL in the browser.
     */
    fun loadUrl(url: String) {
        browser?.loadURL(url)
    }

    /**
     * Gets the current URL.
     */
    fun getCurrentUrl(): String? = browser?.cefBrowser?.url

    /**
     * Returns true if back navigation is available.
     */
    fun canGoBack(): Boolean = history.canGoBack()

    /**
     * Returns true if forward navigation is available.
     */
    fun canGoForward(): Boolean = history.canGoForward()

    /**
     * Navigates back in history.
     */
    fun goBack() {
        history.goBack()?.let {
            browser?.loadURL(it.url)
            notifyNavigationListeners()
        }
    }

    /**
     * Navigates forward in history.
     */
    fun goForward() {
        history.goForward()?.let {
            browser?.loadURL(it.url)
            notifyNavigationListeners()
        }
    }

    /**
     * Jumps back to the entry at the given index in back history.
     */
    fun goBackTo(index: Int) {
        history.goBackTo(index)?.let {
            browser?.loadURL(it.url)
            notifyNavigationListeners()
        }
    }

    /**
     * Jumps forward to the entry at the given index in forward history.
     */
    fun goForwardTo(index: Int) {
        history.goForwardTo(index)?.let {
            browser?.loadURL(it.url)
            notifyNavigationListeners()
        }
    }

    /**
     * Reloads the current page.
     */
    fun reload() {
        browser?.cefBrowser?.reload()
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

    /**
     * Gets the navigation history.
     */
    fun getHistory(): NavigationHistory = history

    /**
     * Sets a listener for URL load events.
     */
    fun setLoadListener(listener: (String) -> Unit) {
        this.loadListener = listener
    }

    /**
     * Adds a listener that is notified when navigation state changes
     * (page loaded, back/forward performed). Used by toolbar to refresh button state.
     */
    fun addNavigationListener(listener: () -> Unit) {
        navigationListeners.add(listener)
    }

    /**
     * Returns true if JCEF is supported.
     */
    fun isBrowserSupported(): Boolean = JBCefApp.isSupported()

    /**
     * Gets the underlying browser component.
     */
    fun getBrowser(): JBCefBrowser? = browser

    override fun dispose() {
        titleQuery?.let { Disposer.dispose(it) }
        titleQuery = null
        browser?.let { Disposer.dispose(it) }
        browser = null
    }

    companion object {
        /**
         * Checks if JCEF is available.
         */
        @JvmStatic
        fun isJcefSupported(): Boolean = JBCefApp.isSupported()
    }
}
