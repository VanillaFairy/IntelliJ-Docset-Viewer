package com.github.clion.docsetviewer.core.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DocsetEntryTest {

    @Test
    fun `create entry with simple path`() {
        val entry = DocsetEntry.create(
            id = 1,
            name = "Test Entry",
            type = DocsetType.CLASS,
            path = "test/file.html",
            docsetIdentifier = "test-docset"
        )

        assertThat(entry.id).isEqualTo(1)
        assertThat(entry.name).isEqualTo("Test Entry")
        assertThat(entry.type).isEqualTo(DocsetType.CLASS)
        assertThat(entry.path).isEqualTo("test/file.html")
        assertThat(entry.anchor).isNull()
        assertThat(entry.docsetIdentifier).isEqualTo("test-docset")
    }

    @Test
    fun `create entry with anchor in path`() {
        val entry = DocsetEntry.create(
            id = 2,
            name = "Test Method",
            type = DocsetType.METHOD,
            path = "api/class.html#methodName",
            docsetIdentifier = "test-docset"
        )

        assertThat(entry.path).isEqualTo("api/class.html")
        assertThat(entry.anchor).isEqualTo("methodName")
    }

    @Test
    fun `fullPath returns path without anchor`() {
        val entry = DocsetEntry(
            id = 1,
            name = "Test",
            type = DocsetType.GUIDE,
            path = "guide/intro.html",
            anchor = null,
            docsetIdentifier = "test"
        )

        assertThat(entry.fullPath).isEqualTo("guide/intro.html")
    }

    @Test
    fun `fullPath returns path with anchor`() {
        val entry = DocsetEntry(
            id = 1,
            name = "Test",
            type = DocsetType.METHOD,
            path = "api/class.html",
            anchor = "someMethod",
            docsetIdentifier = "test"
        )

        assertThat(entry.fullPath).isEqualTo("api/class.html#someMethod")
    }

    @Test
    fun `create handles empty anchor`() {
        val entry = DocsetEntry.create(
            id = 1,
            name = "Test",
            type = DocsetType.CLASS,
            path = "test.html#",
            docsetIdentifier = "test"
        )

        assertThat(entry.path).isEqualTo("test.html")
        assertThat(entry.anchor).isEqualTo("")
    }

    @Test
    fun `create handles multiple hash symbols`() {
        val entry = DocsetEntry.create(
            id = 1,
            name = "Test",
            type = DocsetType.METHOD,
            path = "test.html#section#subsection",
            docsetIdentifier = "test"
        )

        assertThat(entry.path).isEqualTo("test.html")
        assertThat(entry.anchor).isEqualTo("section#subsection")
    }
}
