package com.github.clion.docsetviewer.core.parser

import com.github.clion.docsetviewer.core.model.DocsetType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.After
import org.junit.Test
import java.nio.file.Paths

class SqliteIndexReaderTest {

    private val testDataPath = Paths.get("src/test/resources/testdata")
    private var reader: SqliteIndexReader? = null

    @After
    fun tearDown() {
        reader?.close()
    }

    @Test
    fun `readAllEntries returns all entries from Markdown docset`() {
        val dbPath = testDataPath.resolve("Markdown.docset/Contents/Resources/docSet.dsidx")
        reader = SqliteIndexReader(dbPath)

        val entries = reader!!.readAllEntries("markdown")

        assertThat(entries).hasSize(19)
        assertThat(entries).allSatisfy { entry ->
            assertThat(entry.docsetIdentifier).isEqualTo("markdown")
            assertThat(entry.name).isNotBlank()
            assertThat(entry.path).isNotBlank()
        }
    }

    @Test
    fun `readAllEntries returns entries with correct types from Bash docset`() {
        val dbPath = testDataPath.resolve("Bash.docset/Contents/Resources/docSet.dsidx")
        reader = SqliteIndexReader(dbPath)

        val entries = reader!!.readAllEntries("bash")

        assertThat(entries.size).isGreaterThan(500)

        // Bash docset should have multiple types
        val typeCount = entries.groupingBy { it.type }.eachCount()
        assertThat(typeCount).containsKeys(DocsetType.VARIABLE, DocsetType.FUNCTION, DocsetType.GUIDE)
    }

    @Test
    fun `searchEntries returns exact matches first`() {
        val dbPath = testDataPath.resolve("Bash.docset/Contents/Resources/docSet.dsidx")
        reader = SqliteIndexReader(dbPath)

        val results = reader!!.searchEntries("BASH", "bash", 10)

        assertThat(results).isNotEmpty
        // Exact match should be first if it exists
        val exactMatch = results.find { it.name.equals("BASH", ignoreCase = true) }
        if (exactMatch != null) {
            assertThat(results.first().name).isEqualToIgnoringCase("BASH")
        }
    }

    @Test
    fun `searchEntries returns prefix matches`() {
        val dbPath = testDataPath.resolve("Chai.docset/Contents/Resources/docSet.dsidx")
        reader = SqliteIndexReader(dbPath)

        val results = reader!!.searchEntries("assert", "chai", 50)

        assertThat(results).isNotEmpty
        assertThat(results).allSatisfy { entry ->
            assertThat(entry.name.lowercase()).contains("assert")
        }
    }

    @Test
    fun `searchEntries respects limit`() {
        val dbPath = testDataPath.resolve("Bash.docset/Contents/Resources/docSet.dsidx")
        reader = SqliteIndexReader(dbPath)

        val limit = 10
        val results = reader!!.searchEntries("", "bash", limit)

        assertThat(results).hasSize(limit)
    }

    @Test
    fun `searchEntries with empty query returns entries up to limit`() {
        val dbPath = testDataPath.resolve("Markdown.docset/Contents/Resources/docSet.dsidx")
        reader = SqliteIndexReader(dbPath)

        val results = reader!!.searchEntries("", "markdown", 100)

        assertThat(results).hasSize(19) // Markdown has 19 entries total
    }

    @Test
    fun `getEntryCount returns correct count`() {
        val dbPath = testDataPath.resolve("Markdown.docset/Contents/Resources/docSet.dsidx")
        reader = SqliteIndexReader(dbPath)

        val count = reader!!.getEntryCount()

        assertThat(count).isEqualTo(19)
    }

    @Test
    fun `getEntryCounts returns type breakdown`() {
        val dbPath = testDataPath.resolve("Chai.docset/Contents/Resources/docSet.dsidx")
        reader = SqliteIndexReader(dbPath)

        val counts = reader!!.getEntryCounts()

        assertThat(counts).isNotEmpty
        assertThat(counts[DocsetType.METHOD]).isNotNull()
    }

    @Test
    fun `close releases connection`() {
        val dbPath = testDataPath.resolve("Markdown.docset/Contents/Resources/docSet.dsidx")
        reader = SqliteIndexReader(dbPath)

        reader!!.readAllEntries("markdown")
        reader!!.close()

        // After close, reader should be usable again (reconnects)
        val entries = reader!!.readAllEntries("markdown")
        assertThat(entries).hasSize(19)
    }

    @Test
    fun `constructor throws for non-existent database`() {
        val nonExistentPath = testDataPath.resolve("NonExistent/docSet.dsidx")

        assertThatThrownBy { SqliteIndexReader(nonExistentPath) }
            .isInstanceOf(SqliteIndexException::class.java)
            .hasMessageContaining("not found")
    }
}
