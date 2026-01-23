package com.github.clion.docsetviewer.editor

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import java.awt.BorderLayout
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

    val component: JComponent
        get() = this

    init {
        Disposer.register(parentDisposable, this)

        if (JBCefApp.isSupported()) {
            browser = JBCefBrowser()
            setupBrowser()
            add(browser!!.component, BorderLayout.CENTER)
        } else {
            add(createFallbackPanel(), BorderLayout.CENTER)
        }
    }

    private fun setupBrowser() {
        val cefBrowser = browser?.cefBrowser ?: return
        browser?.jbCefClient?.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                if (frame?.isMain == true) {
                    val url = browser?.url ?: return
                    history.push(url)
                    loadListener?.invoke(url)

                    // Signal color scheme preference to the page
                    injectColorSchemePreference(browser)
                }
            }
        }, cefBrowser)
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
        history.goBack()?.let { browser?.loadURL(it) }
    }

    /**
     * Navigates forward in history.
     */
    fun goForward() {
        history.goForward()?.let { browser?.loadURL(it) }
    }

    /**
     * Reloads the current page.
     */
    fun reload() {
        browser?.cefBrowser?.reload()
    }

    /**
     * Sets a listener for URL load events.
     */
    fun setLoadListener(listener: (String) -> Unit) {
        this.loadListener = listener
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
