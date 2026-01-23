package com.github.clion.docsetviewer.core.parser

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import java.nio.file.Paths

class DocsetParserTest {

    private val parser = DocsetParser()
    private val testDataPath = Paths.get("src/test/resources/testdata")

    @Test
    fun `parse valid Markdown docset`() {
        val docsetPath = testDataPath.resolve("Markdown.docset")
        val docset = parser.parse(docsetPath)

        assertThat(docset.name).isEqualTo("Markdown")
        assertThat(docset.identifier).isEqualTo("markdown")
        assertThat(docset.entryCount).isEqualTo(19)
        assertThat(docset.platformFamily).isEqualTo("markdown")
        assertThat(docset.indexFilePath).isEqualTo("index.html")
    }

    @Test
    fun `parse valid Chai docset`() {
        val docsetPath = testDataPath.resolve("Chai.docset")
        val docset = parser.parse(docsetPath)

        assertThat(docset.name).isEqualTo("Chai")
        assertThat(docset.identifier).isEqualTo("chai")
        assertThat(docset.entryCount).isGreaterThan(200)
    }

    @Test
    fun `parse valid Bash docset`() {
        val docsetPath = testDataPath.resolve("Bash.docset")
        val docset = parser.parse(docsetPath)

        assertThat(docset.name).isEqualTo("Bash")
        assertThat(docset.identifier).isEqualTo("bash")
        assertThat(docset.entryCount).isGreaterThan(500)

        // Verify mixed entry types
        val typeCounts = docset.countByType()
        assertThat(typeCounts.keys.size).isGreaterThan(3)
    }

    @Test
    fun `parse throws for non-existent path`() {
        val nonExistentPath = testDataPath.resolve("NonExistent.docset")

        assertThatThrownBy { parser.parse(nonExistentPath) }
            .isInstanceOf(DocsetParseException::class.java)
            .hasMessageContaining("does not exist")
    }

    @Test
    fun `parse throws for file instead of directory`() {
        val filePath = testDataPath.resolve("Markdown.docset/Contents/Info.plist")

        assertThatThrownBy { parser.parse(filePath) }
            .isInstanceOf(DocsetParseException::class.java)
            .hasMessageContaining("not a directory")
    }

    @Test
    fun `isValidDocset returns true for valid docset`() {
        val docsetPath = testDataPath.resolve("Markdown.docset")
        assertThat(parser.isValidDocset(docsetPath)).isTrue()
    }

    @Test
    fun `isValidDocset returns false for non-existent path`() {
        val nonExistentPath = testDataPath.resolve("NonExistent.docset")
        assertThat(parser.isValidDocset(nonExistentPath)).isFalse()
    }

    @Test
    fun `isValidDocset returns false for incomplete docset`() {
        // A directory without the required structure
        val incompletePath = testDataPath.resolve("Markdown.docset/Contents")
        assertThat(parser.isValidDocset(incompletePath)).isFalse()
    }

    @Test
    fun `parsed docset has correct paths`() {
        val docsetPath = testDataPath.resolve("Markdown.docset")
        val docset = parser.parse(docsetPath)

        assertThat(docset.path).isEqualTo(docsetPath)
        assertThat(docset.documentsPath).isEqualTo(docsetPath.resolve("Contents/Resources/Documents"))
        assertThat(docset.indexDatabasePath).isEqualTo(docsetPath.resolve("Contents/Resources/docSet.dsidx"))
        assertThat(docset.infoPlistPath).isEqualTo(docsetPath.resolve("Contents/Info.plist"))
    }
}
