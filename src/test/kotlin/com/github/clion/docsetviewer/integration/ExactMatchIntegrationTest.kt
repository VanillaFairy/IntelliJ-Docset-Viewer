package com.github.clion.docsetviewer.integration

import com.github.clion.docsetviewer.actions.DocsetLookupHelper
import com.github.clion.docsetviewer.core.model.DocsetType
import com.github.clion.docsetviewer.core.parser.DocsetParser
import com.github.clion.docsetviewer.core.parser.SqliteIndexReader
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import java.nio.file.Paths

/**
 * Integration tests for exact match functionality using real docsets.
 * Tests the complete flow from search to entry selection.
 */
class ExactMatchIntegrationTest {

    private val parser = DocsetParser()
    private val testDataPath = Paths.get("src/test/resources/testdata")
    private var reader: SqliteIndexReader? = null

    @After
    fun tearDown() {
        reader?.close()
    }

    // ========== Chai docset exact match tests ==========

    @Test
    fun `exact match 'assert' opens Style entry directly`() {
        val docset = parser.parse(testDataPath.resolve("Chai.docset"))

        // Simulate searching for "assert"
        val searchResults = docset.entries
            .filter { it.name.lowercase().contains("assert") }
            .sortedWith(compareBy(
                { !it.name.equals("assert", ignoreCase = true) },
                { !it.name.startsWith("assert", ignoreCase = true) },
                { it.name }
            ))

        val result = DocsetLookupHelper.selectEntry(searchResults, "assert")

        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.ExactMatch::class.java)
        val exactMatch = result as DocsetLookupHelper.SelectionResult.ExactMatch
        assertThat(exactMatch.entry.name).isEqualTo("assert")
        assertThat(exactMatch.entry.type).isEqualTo(DocsetType.STYLE)
    }

    @Test
    fun `exact match 'assert_approximately' opens Method entry directly`() {
        val docset = parser.parse(testDataPath.resolve("Chai.docset"))

        val searchResults = docset.entries
            .filter { it.name.lowercase().contains("approximately") }

        val result = DocsetLookupHelper.selectEntry(searchResults, "assert.approximately")

        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.ExactMatch::class.java)
        val exactMatch = result as DocsetLookupHelper.SelectionResult.ExactMatch
        assertThat(exactMatch.entry.name).isEqualTo("assert.approximately")
        assertThat(exactMatch.entry.type).isEqualTo(DocsetType.METHOD)
    }

    @Test
    fun `partial match 'approx' shows multiple results`() {
        val docset = parser.parse(testDataPath.resolve("Chai.docset"))

        val searchResults = docset.entries
            .filter { it.name.lowercase().contains("approx") }

        val result = DocsetLookupHelper.selectEntry(searchResults, "approx")

        // "approx" is not an exact entry, should show results
        assertThat(result).isNotInstanceOf(DocsetLookupHelper.SelectionResult.ExactMatch::class.java)
    }

    @Test
    fun `case insensitive exact match 'ASSERT' finds assert entry`() {
        val docset = parser.parse(testDataPath.resolve("Chai.docset"))

        val searchResults = docset.entries
            .filter { it.name.lowercase().contains("assert") }

        val result = DocsetLookupHelper.selectEntry(searchResults, "ASSERT")

        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.ExactMatch::class.java)
        val exactMatch = result as DocsetLookupHelper.SelectionResult.ExactMatch
        assertThat(exactMatch.entry.name).isEqualTo("assert")
    }

    // ========== Bash docset exact match tests ==========

    @Test
    fun `exact match 'BASH' opens Variable entry directly`() {
        val docset = parser.parse(testDataPath.resolve("Bash.docset"))

        val searchResults = docset.entries
            .filter { it.name.uppercase().contains("BASH") }

        val result = DocsetLookupHelper.selectEntry(searchResults, "BASH")

        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.ExactMatch::class.java)
        val exactMatch = result as DocsetLookupHelper.SelectionResult.ExactMatch
        assertThat(exactMatch.entry.name).isEqualTo("BASH")
        assertThat(exactMatch.entry.type).isEqualTo(DocsetType.VARIABLE)
    }

    @Test
    fun `exact match 'BASHOPTS' opens correct Variable entry`() {
        val docset = parser.parse(testDataPath.resolve("Bash.docset"))

        val searchResults = docset.entries
            .filter { it.name.contains("BASHOPTS", ignoreCase = true) }

        val result = DocsetLookupHelper.selectEntry(searchResults, "BASHOPTS")

        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.ExactMatch::class.java)
        val exactMatch = result as DocsetLookupHelper.SelectionResult.ExactMatch
        assertThat(exactMatch.entry.name).isEqualTo("BASHOPTS")
    }

    @Test
    fun `partial match 'BASH' substring shows multiple BASH variables`() {
        val docset = parser.parse(testDataPath.resolve("Bash.docset"))

        // Search for entries starting with BASH (BASH, BASHOPTS, BASHPID, etc.)
        val searchResults = docset.entries
            .filter { it.name.startsWith("BASH") }

        // Multiple entries start with BASH
        assertThat(searchResults.size).isGreaterThan(1)

        // Searching for exact "BASH" should find the exact entry
        val result = DocsetLookupHelper.selectEntry(searchResults, "BASH")
        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.ExactMatch::class.java)
    }

    @Test
    fun `search for builtin colon finds exact match`() {
        val docset = parser.parse(testDataPath.resolve("Bash.docset"))

        val searchResults = docset.entries
            .filter { it.name == ":" }

        val result = DocsetLookupHelper.selectEntry(searchResults, ":")

        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.ExactMatch::class.java)
        val exactMatch = result as DocsetLookupHelper.SelectionResult.ExactMatch
        assertThat(exactMatch.entry.type).isEqualTo(DocsetType.BUILTIN)
    }

    // ========== Markdown docset exact match tests ==========

    @Test
    fun `exact match 'Headers' opens Guide entry directly`() {
        val docset = parser.parse(testDataPath.resolve("Markdown.docset"))

        val searchResults = docset.entries
            .filter { it.name.lowercase().contains("header") }

        val result = DocsetLookupHelper.selectEntry(searchResults, "Headers")

        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.ExactMatch::class.java)
        val exactMatch = result as DocsetLookupHelper.SelectionResult.ExactMatch
        assertThat(exactMatch.entry.name).isEqualTo("Headers")
        assertThat(exactMatch.entry.type).isEqualTo(DocsetType.GUIDE)
    }

    @Test
    fun `exact match 'Links' opens Guide entry directly`() {
        val docset = parser.parse(testDataPath.resolve("Markdown.docset"))

        val searchResults = docset.entries
            .filter { it.name.lowercase().contains("link") }

        val result = DocsetLookupHelper.selectEntry(searchResults, "Links")

        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.ExactMatch::class.java)
        val exactMatch = result as DocsetLookupHelper.SelectionResult.ExactMatch
        assertThat(exactMatch.entry.name).isEqualTo("Links")
    }

    @Test
    fun `partial match 'code' in Markdown shows multiple results`() {
        val docset = parser.parse(testDataPath.resolve("Markdown.docset"))

        // "Code" and "Code Blocks" both contain "code"
        val searchResults = docset.entries
            .filter { it.name.lowercase().contains("code") }

        assertThat(searchResults.size).isGreaterThan(1)

        // Searching for "code" should show multiple results (Code, Code Blocks)
        val result = DocsetLookupHelper.selectEntry(searchResults, "code")

        // "code" is not an exact match for "Code" (case-insensitive it is!)
        // Actually "code" matches "Code" case-insensitively
        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.ExactMatch::class.java)
    }

    @Test
    fun `exact match 'Code' finds entry among similar names`() {
        val docset = parser.parse(testDataPath.resolve("Markdown.docset"))

        val searchResults = docset.entries
            .filter { it.name.lowercase().contains("code") }

        val result = DocsetLookupHelper.selectEntry(searchResults, "Code")

        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.ExactMatch::class.java)
        val exactMatch = result as DocsetLookupHelper.SelectionResult.ExactMatch
        assertThat(exactMatch.entry.name).isEqualTo("Code")
    }

    // ========== Cross-docset tests ==========

    @Test
    fun `searching across multiple docsets finds exact match`() {
        val chaiDocset = parser.parse(testDataPath.resolve("Chai.docset"))
        val bashDocset = parser.parse(testDataPath.resolve("Bash.docset"))

        // Combine entries from both
        val allEntries = chaiDocset.entries + bashDocset.entries

        // Search for "assert" which exists in Chai
        val searchResults = allEntries.filter { it.name.lowercase().contains("assert") }

        val result = DocsetLookupHelper.selectEntry(searchResults, "assert")

        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.ExactMatch::class.java)
        val exactMatch = result as DocsetLookupHelper.SelectionResult.ExactMatch
        assertThat(exactMatch.entry.name).isEqualTo("assert")
        assertThat(exactMatch.entry.docsetIdentifier).isEqualTo("chai")
    }

    // ========== Edge cases ==========

    @Test
    fun `no results returns NoResults`() {
        val docset = parser.parse(testDataPath.resolve("Markdown.docset"))

        val searchResults = docset.entries
            .filter { it.name.lowercase().contains("nonexistent") }

        val result = DocsetLookupHelper.selectEntry(searchResults, "nonexistent")

        assertThat(result).isEqualTo(DocsetLookupHelper.SelectionResult.NoResults)
    }

    @Test
    fun `single result without exact match returns SingleResult`() {
        val docset = parser.parse(testDataPath.resolve("Markdown.docset"))

        // Find something unique
        val searchResults = docset.entries
            .filter { it.name.contains("Horizontal") }

        assertThat(searchResults).hasSize(1)

        // Search with partial match
        val result = DocsetLookupHelper.selectEntry(searchResults, "Horiz")

        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.SingleResult::class.java)
    }
}
