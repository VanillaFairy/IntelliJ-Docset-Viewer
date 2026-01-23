package io.github.vanillafairy.docsetviewer.editor

import io.github.vanillafairy.docsetviewer.core.model.Docset
import io.github.vanillafairy.docsetviewer.core.model.DocsetEntry
import io.github.vanillafairy.docsetviewer.core.model.DocsetType
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.nio.file.Paths

class DocsetVirtualFileTest {

    private val testDataPath = Paths.get("src/test/resources/testdata")

    private fun createTestDocset(): Docset {
        return Docset(
            name = "Markdown",
            identifier = "markdown",
            path = testDataPath.resolve("Markdown.docset"),
            entries = emptyList()
        )
    }

    private fun createTestEntry(path: String = "index.html", anchor: String? = null): DocsetEntry {
        return DocsetEntry(
            id = 1,
            name = "Test Entry",
            type = DocsetType.GUIDE,
            path = path,
            anchor = anchor,
            docsetIdentifier = "markdown"
        )
    }

    @Test
    fun `getName returns docset and entry name`() {
        val docset = createTestDocset()
        val entry = createTestEntry()
        val file = DocsetVirtualFile(entry, docset)

        assertThat(file.name).isEqualTo("Markdown: Test Entry")
    }

    @Test
    fun `getPath returns absolute path to document`() {
        val docset = createTestDocset()
        val entry = createTestEntry("index.html")
        val file = DocsetVirtualFile(entry, docset)

        val expectedPath = testDataPath.resolve("Markdown.docset/Contents/Resources/Documents/index.html")
        assertThat(file.path).isEqualTo(expectedPath.toString())
    }

    @Test
    fun `isWritable returns false`() {
        val docset = createTestDocset()
        val entry = createTestEntry()
        val file = DocsetVirtualFile(entry, docset)

        assertThat(file.isWritable).isFalse()
    }

    @Test
    fun `isDirectory returns false`() {
        val docset = createTestDocset()
        val entry = createTestEntry()
        val file = DocsetVirtualFile(entry, docset)

        assertThat(file.isDirectory).isFalse()
    }

    @Test
    fun `isValid returns true for existing file`() {
        val docset = createTestDocset()
        val entry = createTestEntry("index.html")
        val file = DocsetVirtualFile(entry, docset)

        assertThat(file.isValid).isTrue()
    }

    @Test
    fun `isValid returns false for non-existing file`() {
        val docset = createTestDocset()
        val entry = createTestEntry("nonexistent.html")
        val file = DocsetVirtualFile(entry, docset)

        assertThat(file.isValid).isFalse()
    }

    @Test
    fun `getDocumentUrl returns URL without anchor`() {
        val docset = createTestDocset()
        val entry = createTestEntry("index.html", null)
        val file = DocsetVirtualFile(entry, docset)

        val url = file.getDocumentUrl()

        assertThat(url).contains("index.html")
        assertThat(url).doesNotContain("#")
    }

    @Test
    fun `getDocumentUrl returns URL with anchor`() {
        val docset = createTestDocset()
        val entry = createTestEntry("index.html", "section1")
        val file = DocsetVirtualFile(entry, docset)

        val url = file.getDocumentUrl()

        assertThat(url).contains("index.html")
        assertThat(url).contains("#section1")
    }

    @Test
    fun `equals returns true for same entry`() {
        val docset = createTestDocset()
        val entry1 = DocsetEntry(1, "Test", DocsetType.GUIDE, "test.html", null, "markdown")
        val entry2 = DocsetEntry(1, "Test", DocsetType.GUIDE, "test.html", null, "markdown")

        val file1 = DocsetVirtualFile(entry1, docset)
        val file2 = DocsetVirtualFile(entry2, docset)

        assertThat(file1).isEqualTo(file2)
    }

    @Test
    fun `equals returns false for different entries`() {
        val docset = createTestDocset()
        val entry1 = DocsetEntry(1, "Test1", DocsetType.GUIDE, "test1.html", null, "markdown")
        val entry2 = DocsetEntry(2, "Test2", DocsetType.GUIDE, "test2.html", null, "markdown")

        val file1 = DocsetVirtualFile(entry1, docset)
        val file2 = DocsetVirtualFile(entry2, docset)

        assertThat(file1).isNotEqualTo(file2)
    }

    @Test
    fun `hashCode is consistent with equals`() {
        val docset = createTestDocset()
        val entry1 = DocsetEntry(1, "Test", DocsetType.GUIDE, "test.html", null, "markdown")
        val entry2 = DocsetEntry(1, "Test", DocsetType.GUIDE, "test.html", null, "markdown")

        val file1 = DocsetVirtualFile(entry1, docset)
        val file2 = DocsetVirtualFile(entry2, docset)

        assertThat(file1.hashCode()).isEqualTo(file2.hashCode())
    }

    @Test
    fun `getFileSystem returns DocsetFileSystem`() {
        val docset = createTestDocset()
        val entry = createTestEntry()
        val file = DocsetVirtualFile(entry, docset)

        assertThat(file.fileSystem).isInstanceOf(DocsetFileSystem::class.java)
        assertThat(file.fileSystem.protocol).isEqualTo("docset")
    }
}
