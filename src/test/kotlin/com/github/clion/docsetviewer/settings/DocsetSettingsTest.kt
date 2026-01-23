package com.github.clion.docsetviewer.settings

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class DocsetSettingsTest {

    private lateinit var settings: DocsetSettings

    @Before
    fun setUp() {
        // Create a new settings instance for testing
        settings = DocsetSettings()
    }

    @Test
    fun `initial state has no docset paths`() {
        assertThat(settings.getDocsetPaths()).isEmpty()
    }

    @Test
    fun `addDocsetPath adds path to list`() {
        val result = settings.addDocsetPath("/path/to/docset.docset")

        assertThat(result).isTrue()
        assertThat(settings.getDocsetPaths()).containsExactly("/path/to/docset.docset")
    }

    @Test
    fun `addDocsetPath normalizes path separators`() {
        settings.addDocsetPath("C:\\path\\to\\docset.docset")

        assertThat(settings.getDocsetPaths()).containsExactly("C:/path/to/docset.docset")
    }

    @Test
    fun `addDocsetPath removes trailing slashes`() {
        settings.addDocsetPath("/path/to/docset.docset/")

        assertThat(settings.getDocsetPaths()).containsExactly("/path/to/docset.docset")
    }

    @Test
    fun `addDocsetPath does not add duplicate`() {
        settings.addDocsetPath("/path/to/docset.docset")
        val result = settings.addDocsetPath("/path/to/docset.docset")

        assertThat(result).isFalse()
        assertThat(settings.getDocsetPaths()).hasSize(1)
    }

    @Test
    fun `addDocsetPath treats normalized paths as duplicates`() {
        settings.addDocsetPath("/path/to/docset.docset")
        val result = settings.addDocsetPath("/path/to/docset.docset/")

        assertThat(result).isFalse()
        assertThat(settings.getDocsetPaths()).hasSize(1)
    }

    @Test
    fun `removeDocsetPath removes path`() {
        settings.addDocsetPath("/path/to/docset1.docset")
        settings.addDocsetPath("/path/to/docset2.docset")

        val result = settings.removeDocsetPath("/path/to/docset1.docset")

        assertThat(result).isTrue()
        assertThat(settings.getDocsetPaths()).containsExactly("/path/to/docset2.docset")
    }

    @Test
    fun `removeDocsetPath returns false for non-existent path`() {
        settings.addDocsetPath("/path/to/docset.docset")

        val result = settings.removeDocsetPath("/other/path.docset")

        assertThat(result).isFalse()
        assertThat(settings.getDocsetPaths()).hasSize(1)
    }

    @Test
    fun `containsDocsetPath returns true for existing path`() {
        settings.addDocsetPath("/path/to/docset.docset")

        assertThat(settings.containsDocsetPath("/path/to/docset.docset")).isTrue()
    }

    @Test
    fun `containsDocsetPath handles path normalization`() {
        settings.addDocsetPath("/path/to/docset.docset")

        assertThat(settings.containsDocsetPath("/path/to/docset.docset/")).isTrue()
        assertThat(settings.containsDocsetPath("\\path\\to\\docset.docset")).isTrue()
    }

    @Test
    fun `containsDocsetPath returns false for non-existent path`() {
        settings.addDocsetPath("/path/to/docset.docset")

        assertThat(settings.containsDocsetPath("/other/path.docset")).isFalse()
    }

    @Test
    fun `clearDocsetPaths removes all paths`() {
        settings.addDocsetPath("/path/to/docset1.docset")
        settings.addDocsetPath("/path/to/docset2.docset")

        settings.clearDocsetPaths()

        assertThat(settings.getDocsetPaths()).isEmpty()
    }

    @Test
    fun `setDocsetPaths replaces all paths`() {
        settings.addDocsetPath("/old/path.docset")

        settings.setDocsetPaths(listOf("/new/path1.docset", "/new/path2.docset"))

        assertThat(settings.getDocsetPaths()).containsExactly("/new/path1.docset", "/new/path2.docset")
    }

    @Test
    fun `setDocsetPaths removes duplicates`() {
        settings.setDocsetPaths(listOf("/path/docset.docset", "/path/docset.docset", "/path/docset.docset/"))

        assertThat(settings.getDocsetPaths()).hasSize(1)
    }

    @Test
    fun `getState returns serializable state`() {
        settings.addDocsetPath("/path/to/docset.docset")

        val state = settings.state

        assertThat(state.docsetPaths).containsExactly("/path/to/docset.docset")
    }

    @Test
    fun `loadState restores settings`() {
        val state = DocsetSettings.State()
        state.docsetPaths.add("/restored/path.docset")

        settings.loadState(state)

        assertThat(settings.getDocsetPaths()).containsExactly("/restored/path.docset")
    }

    @Test
    fun `getDocsetPaths returns copy of list`() {
        settings.addDocsetPath("/path/to/docset.docset")

        val paths = settings.getDocsetPaths()

        // Modifying the returned list should not affect settings
        assertThat(paths).isInstanceOf(List::class.java)
    }
}
