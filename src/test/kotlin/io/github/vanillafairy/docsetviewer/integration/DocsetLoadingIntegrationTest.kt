package io.github.vanillafairy.docsetviewer.integration

import io.github.vanillafairy.docsetviewer.core.model.DocsetType
import io.github.vanillafairy.docsetviewer.core.parser.DocsetParser
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.nio.file.Paths

/**
 * Integration tests for loading real docsets.
 * These tests use the actual test docsets downloaded from Kapeli feeds.
 */
class DocsetLoadingIntegrationTest {

    private val parser = DocsetParser()
    private val testDataPath = Paths.get("src/test/resources/testdata")

    @Test
    fun `load Markdown docset and verify 19 entries`() {
        val docsetPath = testDataPath.resolve("Markdown.docset")
        val docset = parser.parse(docsetPath)

        assertThat(docset.name).isEqualTo("Markdown")
        assertThat(docset.identifier).isEqualTo("markdown")
        assertThat(docset.entryCount).isEqualTo(19)

        // All entries should be Guide type
        val typeCounts = docset.countByType()
        assertThat(typeCounts[DocsetType.GUIDE]).isEqualTo(19)
    }

    @Test
    fun `load Chai docset and verify entry types`() {
        val docsetPath = testDataPath.resolve("Chai.docset")
        val docset = parser.parse(docsetPath)

        assertThat(docset.name).isEqualTo("Chai")
        assertThat(docset.identifier).isEqualTo("chai")
        assertThat(docset.entryCount).isGreaterThan(200)

        // Chai should have mostly Method entries
        val typeCounts = docset.countByType()
        assertThat(typeCounts[DocsetType.METHOD]).isNotNull()
        assertThat(typeCounts[DocsetType.METHOD]!!).isGreaterThan(100)
    }

    @Test
    fun `load Bash docset and verify mixed types`() {
        val docsetPath = testDataPath.resolve("Bash.docset")
        val docset = parser.parse(docsetPath)

        assertThat(docset.name).isEqualTo("Bash")
        assertThat(docset.identifier).isEqualTo("bash")
        assertThat(docset.entryCount).isGreaterThan(500)

        // Bash should have Variable, Guide, Function, Builtin types
        val typeCounts = docset.countByType()
        assertThat(typeCounts.keys).contains(DocsetType.VARIABLE)
        assertThat(typeCounts.keys).contains(DocsetType.GUIDE)
        assertThat(typeCounts.keys).contains(DocsetType.FUNCTION)
        assertThat(typeCounts.keys).contains(DocsetType.BUILTIN)
    }

    @Test
    fun `entry paths resolve to existing files in Markdown docset`() {
        val docsetPath = testDataPath.resolve("Markdown.docset")
        val docset = parser.parse(docsetPath)

        // At least the index.html should exist
        val indexEntry = docset.entries.find { it.path == "index.html" }
        assertThat(indexEntry).isNotNull

        val resolvedPath = docset.resolveDocumentPath(indexEntry!!.path)
        assertThat(resolvedPath.toFile().exists()).isTrue()
    }

    @Test
    fun `validate all test docsets exist`() {
        val docsets = listOf(
            "Markdown.docset",
            "Chai.docset",
            "Bash.docset"
        )

        docsets.forEach { docsetName ->
            val docsetPath = testDataPath.resolve(docsetName)
            assertThat(parser.isValidDocset(docsetPath))
                .withFailMessage("$docsetName should be a valid docset")
                .isTrue()
        }
    }

    @Test
    fun `docset entries have valid identifiers`() {
        val docsetPath = testDataPath.resolve("Markdown.docset")
        val docset = parser.parse(docsetPath)

        docset.entries.forEach { entry ->
            assertThat(entry.id).isGreaterThan(0)
            assertThat(entry.docsetIdentifier).isEqualTo(docset.identifier)
        }
    }
}
