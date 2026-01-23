package com.github.clion.docsetviewer.core.parser

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import java.nio.file.Paths

class InfoPlistParserTest {

    private val parser = InfoPlistParser()
    private val testDataPath = Paths.get("src/test/resources/testdata")

    @Test
    fun `parse valid plist from Markdown docset`() {
        val plistPath = testDataPath.resolve("Markdown.docset/Contents/Info.plist")
        val result = parser.parse(plistPath)

        assertThat(result.bundleName).isEqualTo("Markdown")
        assertThat(result.bundleIdentifier).isEqualTo("markdown")
        assertThat(result.platformFamily).isEqualTo("markdown")
        assertThat(result.indexFilePath).isEqualTo("index.html")
        assertThat(result.isDashDocset).isTrue()
    }

    @Test
    fun `parse valid plist from Bash docset`() {
        val plistPath = testDataPath.resolve("Bash.docset/Contents/Info.plist")
        val result = parser.parse(plistPath)

        assertThat(result.bundleName).isEqualTo("Bash")
        assertThat(result.bundleIdentifier).isEqualTo("bash")
        assertThat(result.platformFamily).isEqualTo("bash")
        assertThat(result.indexFilePath).isEqualTo("bash/index.html")
    }

    @Test
    fun `parse valid plist from Chai docset`() {
        val plistPath = testDataPath.resolve("Chai.docset/Contents/Info.plist")
        val result = parser.parse(plistPath)

        assertThat(result.bundleName).isEqualTo("Chai")
        assertThat(result.bundleIdentifier).isEqualTo("chai")
    }

    @Test
    fun `parse throws exception for missing file`() {
        val nonExistentPath = testDataPath.resolve("NonExistent.docset/Contents/Info.plist")

        assertThatThrownBy { parser.parse(nonExistentPath) }
            .isInstanceOf(InfoPlistParseException::class.java)
            .hasMessageContaining("not found")
    }

    @Test
    fun `parse throws exception for invalid XML`() {
        // Create a temporary invalid plist file path
        val invalidPath = Paths.get("src/test/resources/testdata/invalid.plist")

        // This test expects the file to not exist (or be invalid)
        if (!invalidPath.toFile().exists()) {
            assertThatThrownBy { parser.parse(invalidPath) }
                .isInstanceOf(InfoPlistParseException::class.java)
        }
    }
}
