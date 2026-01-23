package io.github.vanillafairy.docsetviewer.actions

import io.github.vanillafairy.docsetviewer.core.model.DocsetEntry
import io.github.vanillafairy.docsetviewer.core.model.DocsetType
import io.github.vanillafairy.docsetviewer.core.parser.DocsetParser
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.nio.file.Paths

/**
 * Unit tests for DocsetLookupHelper.
 * Tests the exact match selection logic and token extraction.
 */
class DocsetLookupHelperTest {

    private val parser = DocsetParser()
    private val testDataPath = Paths.get("src/test/resources/testdata")

    // ========== selectEntry tests ==========

    @Test
    fun `selectEntry returns NoResults for empty list`() {
        val result = DocsetLookupHelper.selectEntry(emptyList(), "anything")

        assertThat(result).isEqualTo(DocsetLookupHelper.SelectionResult.NoResults)
    }

    @Test
    fun `selectEntry returns ExactMatch when entry name matches token exactly`() {
        val entries = listOf(
            createEntry("MyClass", DocsetType.CLASS),
            createEntry("MyClass::method1", DocsetType.METHOD),
            createEntry("MyClass::method2", DocsetType.METHOD)
        )

        val result = DocsetLookupHelper.selectEntry(entries, "MyClass")

        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.ExactMatch::class.java)
        val exactMatch = result as DocsetLookupHelper.SelectionResult.ExactMatch
        assertThat(exactMatch.entry.name).isEqualTo("MyClass")
    }

    @Test
    fun `selectEntry returns ExactMatch case-insensitive`() {
        val entries = listOf(
            createEntry("MyClass", DocsetType.CLASS),
            createEntry("MyClass::method1", DocsetType.METHOD)
        )

        val result = DocsetLookupHelper.selectEntry(entries, "myclass")

        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.ExactMatch::class.java)
        val exactMatch = result as DocsetLookupHelper.SelectionResult.ExactMatch
        assertThat(exactMatch.entry.name).isEqualTo("MyClass")
    }

    @Test
    fun `selectEntry returns ExactMatch for qualified name`() {
        val entries = listOf(
            createEntry("MyClass", DocsetType.CLASS),
            createEntry("MyClass::method1", DocsetType.METHOD),
            createEntry("MyClass::method2", DocsetType.METHOD)
        )

        val result = DocsetLookupHelper.selectEntry(entries, "MyClass::method1")

        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.ExactMatch::class.java)
        val exactMatch = result as DocsetLookupHelper.SelectionResult.ExactMatch
        assertThat(exactMatch.entry.name).isEqualTo("MyClass::method1")
    }

    @Test
    fun `selectEntry returns SingleResult when only one result and no exact match`() {
        val entries = listOf(
            createEntry("MyClass::method1", DocsetType.METHOD)
        )

        val result = DocsetLookupHelper.selectEntry(entries, "method1")

        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.SingleResult::class.java)
        val singleResult = result as DocsetLookupHelper.SelectionResult.SingleResult
        assertThat(singleResult.entry.name).isEqualTo("MyClass::method1")
    }

    @Test
    fun `selectEntry returns MultipleResults when no exact match and multiple results`() {
        val entries = listOf(
            createEntry("MyClass::method1", DocsetType.METHOD),
            createEntry("MyClass::method2", DocsetType.METHOD),
            createEntry("MyClass::method3", DocsetType.METHOD)
        )

        val result = DocsetLookupHelper.selectEntry(entries, "method")

        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.MultipleResults::class.java)
        val multipleResults = result as DocsetLookupHelper.SelectionResult.MultipleResults
        assertThat(multipleResults.entries).hasSize(3)
    }

    @Test
    fun `selectEntry prefers exact match over single result`() {
        val entries = listOf(
            createEntry("mid", DocsetType.FUNCTION)
        )

        val result = DocsetLookupHelper.selectEntry(entries, "mid")

        // Should be ExactMatch, not SingleResult
        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.ExactMatch::class.java)
    }

    @Test
    fun `selectEntry finds exact match among many results`() {
        val entries = listOf(
            createEntry("asserting", DocsetType.GUIDE),
            createEntry("assert", DocsetType.METHOD),
            createEntry("assert.equal", DocsetType.METHOD),
            createEntry("assertThat", DocsetType.METHOD)
        )

        val result = DocsetLookupHelper.selectEntry(entries, "assert")

        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.ExactMatch::class.java)
        val exactMatch = result as DocsetLookupHelper.SelectionResult.ExactMatch
        assertThat(exactMatch.entry.name).isEqualTo("assert")
    }

    // ========== selectEntry class lookup tests ==========

    @Test
    fun `selectEntry finds class entry when searching for class name among methods`() {
        val entries = listOf(
            createEntry("MyClass", DocsetType.CLASS),
            createEntry("MyClass::method1", DocsetType.METHOD),
            createEntry("MyClass::method2", DocsetType.METHOD),
            createEntry("MyClass::method3", DocsetType.METHOD)
        )

        val result = DocsetLookupHelper.selectEntry(entries, "MyClass")

        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.ExactMatch::class.java)
        val exactMatch = result as DocsetLookupHelper.SelectionResult.ExactMatch
        assertThat(exactMatch.entry.name).isEqualTo("MyClass")
        assertThat(exactMatch.entry.type).isEqualTo(DocsetType.CLASS)
    }

    @Test
    fun `selectEntry finds class entry with 'Class' suffix`() {
        val entries = listOf(
            createEntry("MyClass Class", DocsetType.CLASS),
            createEntry("MyClass::method1", DocsetType.METHOD),
            createEntry("MyClass::method2", DocsetType.METHOD)
        )

        val result = DocsetLookupHelper.selectEntry(entries, "MyClass")

        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.ExactMatch::class.java)
        val exactMatch = result as DocsetLookupHelper.SelectionResult.ExactMatch
        assertThat(exactMatch.entry.name).isEqualTo("MyClass Class")
        assertThat(exactMatch.entry.type).isEqualTo(DocsetType.CLASS)
    }

    @Test
    fun `selectEntry finds struct entry when searching for struct name`() {
        val entries = listOf(
            createEntry("MyStruct", DocsetType.STRUCT),
            createEntry("MyStruct::field1", DocsetType.FIELD),
            createEntry("MyStruct::field2", DocsetType.FIELD)
        )

        val result = DocsetLookupHelper.selectEntry(entries, "MyStruct")

        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.ExactMatch::class.java)
        val exactMatch = result as DocsetLookupHelper.SelectionResult.ExactMatch
        assertThat(exactMatch.entry.type).isEqualTo(DocsetType.STRUCT)
    }

    @Test
    fun `selectEntry finds interface entry`() {
        val entries = listOf(
            createEntry("MyInterface", DocsetType.INTERFACE),
            createEntry("MyInterface::method1", DocsetType.METHOD)
        )

        val result = DocsetLookupHelper.selectEntry(entries, "MyInterface")

        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.ExactMatch::class.java)
    }

    @Test
    fun `selectEntry does not use class lookup for qualified names`() {
        val entries = listOf(
            createEntry("MyClass", DocsetType.CLASS),
            createEntry("MyClass::method1", DocsetType.METHOD),
            createEntry("MyClass::method2", DocsetType.METHOD)
        )

        val result = DocsetLookupHelper.selectEntry(entries, "MyClass::method1")

        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.ExactMatch::class.java)
        val exactMatch = result as DocsetLookupHelper.SelectionResult.ExactMatch
        assertThat(exactMatch.entry.name).isEqualTo("MyClass::method1")
    }

    @Test
    fun `selectEntry returns MultipleResults when no class entry exists`() {
        // Only methods, no class entry - should show popup
        val entries = listOf(
            createEntry("MyClass::method1", DocsetType.METHOD),
            createEntry("MyClass::method2", DocsetType.METHOD),
            createEntry("MyClass::method3", DocsetType.METHOD)
        )

        val result = DocsetLookupHelper.selectEntry(entries, "MyClass")

        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.MultipleResults::class.java)
    }

    @Test
    fun `selectEntry finds enum entry`() {
        val entries = listOf(
            createEntry("Color", DocsetType.ENUM),
            createEntry("Color::Red", DocsetType.CONSTANT),
            createEntry("Color::Green", DocsetType.CONSTANT)
        )

        val result = DocsetLookupHelper.selectEntry(entries, "Color")

        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.ExactMatch::class.java)
        val exactMatch = result as DocsetLookupHelper.SelectionResult.ExactMatch
        assertThat(exactMatch.entry.type).isEqualTo(DocsetType.ENUM)
    }

    @Test
    fun `selectEntry finds generic class entry`() {
        val entries = listOf(
            createEntry("List<T>", DocsetType.CLASS),
            createEntry("List::append", DocsetType.METHOD)
        )

        val result = DocsetLookupHelper.selectEntry(entries, "List")

        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.ExactMatch::class.java)
        val exactMatch = result as DocsetLookupHelper.SelectionResult.ExactMatch
        assertThat(exactMatch.entry.name).isEqualTo("List<T>")
    }

    // ========== selectEntry with real docset data ==========

    @Test
    fun `selectEntry finds exact 'assert' in Chai docset entries`() {
        val docset = parser.parse(testDataPath.resolve("Chai.docset"))
        val results = docset.entries.filter { it.name.lowercase().contains("assert") }

        val result = DocsetLookupHelper.selectEntry(results, "assert")

        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.ExactMatch::class.java)
        val exactMatch = result as DocsetLookupHelper.SelectionResult.ExactMatch
        assertThat(exactMatch.entry.name).isEqualTo("assert")
        assertThat(exactMatch.entry.type).isEqualTo(DocsetType.STYLE)
    }

    @Test
    fun `selectEntry finds exact 'assert_deepEqual' method in Chai`() {
        val docset = parser.parse(testDataPath.resolve("Chai.docset"))
        val results = docset.entries.filter { it.name.lowercase().contains("deepequal") }

        val result = DocsetLookupHelper.selectEntry(results, "assert.deepEqual")

        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.ExactMatch::class.java)
        val exactMatch = result as DocsetLookupHelper.SelectionResult.ExactMatch
        assertThat(exactMatch.entry.name).isEqualTo("assert.deepEqual")
    }

    @Test
    fun `selectEntry finds exact 'BASH' variable in Bash docset`() {
        val docset = parser.parse(testDataPath.resolve("Bash.docset"))
        val results = docset.entries.filter { it.name.uppercase().contains("BASH") }

        val result = DocsetLookupHelper.selectEntry(results, "BASH")

        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.ExactMatch::class.java)
        val exactMatch = result as DocsetLookupHelper.SelectionResult.ExactMatch
        assertThat(exactMatch.entry.name).isEqualTo("BASH")
        assertThat(exactMatch.entry.type).isEqualTo(DocsetType.VARIABLE)
    }

    @Test
    fun `selectEntry finds exact 'Headers' guide in Markdown docset`() {
        val docset = parser.parse(testDataPath.resolve("Markdown.docset"))
        val results = docset.entries.filter { it.name.lowercase().contains("header") }

        val result = DocsetLookupHelper.selectEntry(results, "Headers")

        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.ExactMatch::class.java)
        val exactMatch = result as DocsetLookupHelper.SelectionResult.ExactMatch
        assertThat(exactMatch.entry.name).isEqualTo("Headers")
    }

    @Test
    fun `selectEntry returns MultipleResults for partial match in Chai`() {
        val docset = parser.parse(testDataPath.resolve("Chai.docset"))
        // "equal" matches multiple: assert.deepEqual, assert.equal, assert.notEqual, etc.
        val results = docset.entries.filter { it.name.lowercase().contains("equal") }

        val result = DocsetLookupHelper.selectEntry(results, "equal")

        // "equal" is not an exact entry name, so should return multiple
        assertThat(result).isInstanceOf(DocsetLookupHelper.SelectionResult.MultipleResults::class.java)
    }

    // ========== isWordChar tests ==========

    @Test
    fun `isWordChar returns true for letters`() {
        assertThat(DocsetLookupHelper.isWordChar('a')).isTrue()
        assertThat(DocsetLookupHelper.isWordChar('Z')).isTrue()
        assertThat(DocsetLookupHelper.isWordChar('m')).isTrue()
    }

    @Test
    fun `isWordChar returns true for digits`() {
        assertThat(DocsetLookupHelper.isWordChar('0')).isTrue()
        assertThat(DocsetLookupHelper.isWordChar('5')).isTrue()
        assertThat(DocsetLookupHelper.isWordChar('9')).isTrue()
    }

    @Test
    fun `isWordChar returns true for underscore`() {
        assertThat(DocsetLookupHelper.isWordChar('_')).isTrue()
    }

    @Test
    fun `isWordChar returns true for hyphen`() {
        assertThat(DocsetLookupHelper.isWordChar('-')).isTrue()
    }

    @Test
    fun `isWordChar returns true for colon`() {
        assertThat(DocsetLookupHelper.isWordChar(':')).isTrue()
    }

    @Test
    fun `isWordChar returns false for space`() {
        assertThat(DocsetLookupHelper.isWordChar(' ')).isFalse()
    }

    @Test
    fun `isWordChar returns false for punctuation`() {
        assertThat(DocsetLookupHelper.isWordChar('.')).isFalse()
        assertThat(DocsetLookupHelper.isWordChar('(')).isFalse()
        assertThat(DocsetLookupHelper.isWordChar(')')).isFalse()
        assertThat(DocsetLookupHelper.isWordChar(',')).isFalse()
        assertThat(DocsetLookupHelper.isWordChar(';')).isFalse()
    }

    // ========== isOperatorChar tests ==========

    @Test
    fun `isOperatorChar returns true for common operators`() {
        assertThat(DocsetLookupHelper.isOperatorChar('<')).isTrue()
        assertThat(DocsetLookupHelper.isOperatorChar('>')).isTrue()
        assertThat(DocsetLookupHelper.isOperatorChar('+')).isTrue()
        assertThat(DocsetLookupHelper.isOperatorChar('*')).isTrue()
        assertThat(DocsetLookupHelper.isOperatorChar('/')).isTrue()
        assertThat(DocsetLookupHelper.isOperatorChar('%')).isTrue()
        assertThat(DocsetLookupHelper.isOperatorChar('&')).isTrue()
        assertThat(DocsetLookupHelper.isOperatorChar('|')).isTrue()
        assertThat(DocsetLookupHelper.isOperatorChar('!')).isTrue()
        assertThat(DocsetLookupHelper.isOperatorChar('=')).isTrue()
        assertThat(DocsetLookupHelper.isOperatorChar('^')).isTrue()
        assertThat(DocsetLookupHelper.isOperatorChar('~')).isTrue()
    }

    @Test
    fun `isOperatorChar returns false for letters and digits`() {
        assertThat(DocsetLookupHelper.isOperatorChar('a')).isFalse()
        assertThat(DocsetLookupHelper.isOperatorChar('Z')).isFalse()
        assertThat(DocsetLookupHelper.isOperatorChar('5')).isFalse()
    }

    @Test
    fun `isOperatorChar returns false for non-operator punctuation`() {
        assertThat(DocsetLookupHelper.isOperatorChar('.')).isFalse()
        assertThat(DocsetLookupHelper.isOperatorChar(',')).isFalse()
        assertThat(DocsetLookupHelper.isOperatorChar(';')).isFalse()
        assertThat(DocsetLookupHelper.isOperatorChar('(')).isFalse()
        assertThat(DocsetLookupHelper.isOperatorChar(')')).isFalse()
    }

    // ========== extractTokenAt tests ==========

    @Test
    fun `extractTokenAt returns simple word`() {
        val text = "MyClass foo bar"
        val result = DocsetLookupHelper.extractTokenAt(text, 3)

        assertThat(result).isEqualTo("MyClass")
    }

    @Test
    fun `extractTokenAt returns qualified name with colons`() {
        val text = "MyClass::method is a method"
        val result = DocsetLookupHelper.extractTokenAt(text, 5)

        assertThat(result).isEqualTo("MyClass::method")
    }

    @Test
    fun `extractTokenAt returns word with underscore`() {
        val text = "my_function_name"
        val result = DocsetLookupHelper.extractTokenAt(text, 5)

        assertThat(result).isEqualTo("my_function_name")
    }

    @Test
    fun `extractTokenAt returns word with hyphen`() {
        val text = "some-hyphenated-word"
        val result = DocsetLookupHelper.extractTokenAt(text, 7)

        assertThat(result).isEqualTo("some-hyphenated-word")
    }

    @Test
    fun `extractTokenAt returns null for whitespace only`() {
        val text = "   "
        val result = DocsetLookupHelper.extractTokenAt(text, 1)

        assertThat(result).isNull()
    }

    @Test
    fun `extractTokenAt returns null for empty string`() {
        val result = DocsetLookupHelper.extractTokenAt("", 0)

        assertThat(result).isNull()
    }

    @Test
    fun `extractTokenAt handles caret at start of word`() {
        val text = "MyClass foo"
        val result = DocsetLookupHelper.extractTokenAt(text, 0)

        assertThat(result).isEqualTo("MyClass")
    }

    @Test
    fun `extractTokenAt handles caret at end of word`() {
        val text = "MyClass foo"
        val result = DocsetLookupHelper.extractTokenAt(text, 7)

        assertThat(result).isEqualTo("MyClass")
    }

    @Test
    fun `extractTokenAt handles caret at end of document`() {
        val text = "word"
        val result = DocsetLookupHelper.extractTokenAt(text, 4)

        assertThat(result).isEqualTo("word")
    }

    @Test
    fun `extractTokenAt handles caret in middle of word`() {
        val text = "MyClass"
        val result = DocsetLookupHelper.extractTokenAt(text, 3)

        assertThat(result).isEqualTo("MyClass")
    }

    @Test
    fun `extractTokenAt returns word when caret is between words`() {
        val text = "foo bar"
        // Caret at space between words - should find adjacent word
        val result = DocsetLookupHelper.extractTokenAt(text, 3)

        assertThat(result).isEqualTo("foo")
    }

    @Test
    fun `extractTokenAt handles negative offset`() {
        val result = DocsetLookupHelper.extractTokenAt("word", -1)

        assertThat(result).isNull()
    }

    @Test
    fun `extractTokenAt handles offset beyond text length`() {
        val text = "word"
        val result = DocsetLookupHelper.extractTokenAt(text, 100)

        assertThat(result).isNull()
    }

    @Test
    fun `extractTokenAt extracts deepEqual from dotted expression`() {
        val text = "chai.assert.deepEqual(a, b)"
        // Caret on "deepEqual" part - dot is not a word char, so only "deepEqual" is extracted
        val result = DocsetLookupHelper.extractTokenAt(text, 15)

        assertThat(result).isEqualTo("deepEqual")
    }

    @Test
    fun `extractTokenAt handles dot as non-word character`() {
        val text = "object.method"
        // Dot is not a word char, so should get separate tokens
        val result = DocsetLookupHelper.extractTokenAt(text, 2)

        assertThat(result).isEqualTo("object")
    }

    // ========== extractTokenAt operator tests ==========

    @Test
    fun `extractTokenAt extracts shift left operator`() {
        val text = "a << b"
        val result = DocsetLookupHelper.extractTokenAt(text, 2)

        assertThat(result).isEqualTo("<<")
    }

    @Test
    fun `extractTokenAt extracts shift right operator`() {
        val text = "a >> b"
        val result = DocsetLookupHelper.extractTokenAt(text, 3)

        assertThat(result).isEqualTo(">>")
    }

    @Test
    fun `extractTokenAt extracts equality operator`() {
        val text = "a == b"
        val result = DocsetLookupHelper.extractTokenAt(text, 2)

        assertThat(result).isEqualTo("==")
    }

    @Test
    fun `extractTokenAt extracts not-equal operator`() {
        val text = "a != b"
        val result = DocsetLookupHelper.extractTokenAt(text, 2)

        assertThat(result).isEqualTo("!=")
    }

    @Test
    fun `extractTokenAt extracts logical and operator`() {
        val text = "a && b"
        val result = DocsetLookupHelper.extractTokenAt(text, 2)

        assertThat(result).isEqualTo("&&")
    }

    @Test
    fun `extractTokenAt extracts logical or operator`() {
        val text = "a || b"
        val result = DocsetLookupHelper.extractTokenAt(text, 2)

        assertThat(result).isEqualTo("||")
    }

    @Test
    fun `extractTokenAt extracts compound assignment operators`() {
        val text = "a += b"
        val result = DocsetLookupHelper.extractTokenAt(text, 2)

        assertThat(result).isEqualTo("+=")
    }

    @Test
    fun `extractTokenAt extracts comparison operator`() {
        val text = "a <= b"
        val result = DocsetLookupHelper.extractTokenAt(text, 2)

        assertThat(result).isEqualTo("<=")
    }

    @Test
    fun `extractTokenAt extracts single character operator`() {
        val text = "a + b"
        val result = DocsetLookupHelper.extractTokenAt(text, 2)

        assertThat(result).isEqualTo("+")
    }

    @Test
    fun `extractTokenAt prefers word over adjacent operator`() {
        val text = "cout << value"
        // Caret on "cout" should extract "cout", not "<<"
        val result = DocsetLookupHelper.extractTokenAt(text, 2)

        assertThat(result).isEqualTo("cout")
    }

    @Test
    fun `extractTokenAt extracts operator between words`() {
        val text = "cout<<value"
        // Caret on "<<" should extract "<<", not adjacent identifiers
        val result = DocsetLookupHelper.extractTokenAt(text, 4)

        assertThat(result).isEqualTo("<<")
    }

    // ========== Helper methods ==========

    private fun createEntry(name: String, type: DocsetType): DocsetEntry {
        return DocsetEntry(
            id = name.hashCode().toLong(),
            name = name,
            type = type,
            path = "test.html",
            anchor = null,
            docsetIdentifier = "test-docset"
        )
    }
}
