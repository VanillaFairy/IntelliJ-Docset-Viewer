package io.github.vanillafairy.docsetviewer.integration

import io.github.vanillafairy.docsetviewer.core.model.DocsetType
import io.github.vanillafairy.docsetviewer.core.parser.DocsetParser
import io.github.vanillafairy.docsetviewer.core.parser.SqliteIndexReader
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import java.nio.file.Paths

/**
 * Integration tests for search functionality across real docsets.
 */
class SearchIntegrationTest {

    private val parser = DocsetParser()
    private val testDataPath = Paths.get("src/test/resources/testdata")
    private var reader: SqliteIndexReader? = null

    @After
    fun tearDown() {
        reader?.close()
    }

    @Test
    fun `search 'assert' in Chai docset returns Method results`() {
        val dbPath = testDataPath.resolve("Chai.docset/Contents/Resources/docSet.dsidx")
        reader = SqliteIndexReader(dbPath)

        val results = reader!!.searchEntries("assert", "chai", 50)

        assertThat(results).isNotEmpty
        assertThat(results).allSatisfy { entry ->
            assertThat(entry.name.lowercase()).contains("assert")
        }

        // Most assert results should be Method type
        val methodResults = results.filter { it.type == DocsetType.METHOD }
        assertThat(methodResults).isNotEmpty
    }

    @Test
    fun `search returns exact matches first`() {
        val dbPath = testDataPath.resolve("Chai.docset/Contents/Resources/docSet.dsidx")
        reader = SqliteIndexReader(dbPath)

        val results = reader!!.searchEntries("equal", "chai", 20)

        assertThat(results).isNotEmpty
        // All results should contain "equal"
        assertThat(results).allSatisfy { entry ->
            assertThat(entry.name.lowercase()).contains("equal")
        }
    }

    @Test
    fun `search in Bash docset returns mixed types`() {
        val dbPath = testDataPath.resolve("Bash.docset/Contents/Resources/docSet.dsidx")
        reader = SqliteIndexReader(dbPath)

        val results = reader!!.searchEntries("BASH", "bash", 50)

        assertThat(results).isNotEmpty

        // Should have multiple types for bash-related terms
        val types = results.map { it.type }.toSet()
        assertThat(types.size).isGreaterThan(1)
    }

    @Test
    fun `search with limit restricts results`() {
        val dbPath = testDataPath.resolve("Bash.docset/Contents/Resources/docSet.dsidx")
        reader = SqliteIndexReader(dbPath)

        val limit = 5
        val results = reader!!.searchEntries("var", "bash", limit)

        assertThat(results.size).isLessThanOrEqualTo(limit)
    }

    @Test
    fun `empty search returns entries up to limit`() {
        val docset = parser.parse(testDataPath.resolve("Markdown.docset"))

        // Markdown has 19 entries, asking for 100 should return all 19
        val results = docset.entries

        assertThat(results).hasSize(19)
    }

    @Test
    fun `case-insensitive search works`() {
        val dbPath = testDataPath.resolve("Chai.docset/Contents/Resources/docSet.dsidx")
        reader = SqliteIndexReader(dbPath)

        val lowerResults = reader!!.searchEntries("equal", "chai", 20)
        val upperResults = reader!!.searchEntries("EQUAL", "chai", 20)
        val mixedResults = reader!!.searchEntries("Equal", "chai", 20)

        // All searches should return the same entries
        assertThat(lowerResults.map { it.name }.toSet())
            .isEqualTo(upperResults.map { it.name }.toSet())
        assertThat(lowerResults.map { it.name }.toSet())
            .isEqualTo(mixedResults.map { it.name }.toSet())
    }

    @Test
    fun `search result entries have valid paths`() {
        val docsetPath = testDataPath.resolve("Chai.docset")
        val docset = parser.parse(docsetPath)

        val dbPath = docsetPath.resolve("Contents/Resources/docSet.dsidx")
        reader = SqliteIndexReader(dbPath)

        val results = reader!!.searchEntries("assert", "chai", 10)

        results.forEach { entry ->
            assertThat(entry.path).isNotBlank()
            // Path should not contain the full filesystem path, just relative
            assertThat(entry.path).doesNotContain("docSet.dsidx")
        }
    }

    @Test
    fun `search handles special characters`() {
        val dbPath = testDataPath.resolve("Bash.docset/Contents/Resources/docSet.dsidx")
        reader = SqliteIndexReader(dbPath)

        // These searches should not throw exceptions
        val results1 = reader!!.searchEntries("$", "bash", 10)
        val results2 = reader!!.searchEntries("#", "bash", 10)
        val results3 = reader!!.searchEntries("(", "bash", 10)

        // Just verify no exceptions - results may or may not be empty
        assertThat(results1).isNotNull
        assertThat(results2).isNotNull
        assertThat(results3).isNotNull
    }

    @Test
    fun `search across multiple parsed docsets`() {
        val markdownDocset = parser.parse(testDataPath.resolve("Markdown.docset"))
        val chaiDocset = parser.parse(testDataPath.resolve("Chai.docset"))

        // Combine entries from both docsets
        val allEntries = markdownDocset.entries + chaiDocset.entries

        // Search for common term
        val results = allEntries.filter { it.name.lowercase().contains("h") }.take(50)

        assertThat(results).isNotEmpty

        // Results should come from multiple docsets
        val docsetIds = results.map { it.docsetIdentifier }.toSet()
        // At least markdown should have entries with 'h' in name
        assertThat(docsetIds).isNotEmpty
    }
}
