package io.github.vanillafairy.docsetviewer.core.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DocsetTypeTest {

    @Test
    fun `fromString maps known types correctly`() {
        assertThat(DocsetType.fromString("Class")).isEqualTo(DocsetType.CLASS)
        assertThat(DocsetType.fromString("Method")).isEqualTo(DocsetType.METHOD)
        assertThat(DocsetType.fromString("Function")).isEqualTo(DocsetType.FUNCTION)
        assertThat(DocsetType.fromString("Property")).isEqualTo(DocsetType.PROPERTY)
        assertThat(DocsetType.fromString("Guide")).isEqualTo(DocsetType.GUIDE)
        assertThat(DocsetType.fromString("Variable")).isEqualTo(DocsetType.VARIABLE)
        assertThat(DocsetType.fromString("Builtin")).isEqualTo(DocsetType.BUILTIN)
    }

    @Test
    fun `fromString is case insensitive`() {
        assertThat(DocsetType.fromString("class")).isEqualTo(DocsetType.CLASS)
        assertThat(DocsetType.fromString("CLASS")).isEqualTo(DocsetType.CLASS)
        assertThat(DocsetType.fromString("Class")).isEqualTo(DocsetType.CLASS)
        assertThat(DocsetType.fromString("ClAsS")).isEqualTo(DocsetType.CLASS)
    }

    @Test
    fun `fromString handles whitespace`() {
        assertThat(DocsetType.fromString("  Class  ")).isEqualTo(DocsetType.CLASS)
        assertThat(DocsetType.fromString("\tMethod\t")).isEqualTo(DocsetType.METHOD)
    }

    @Test
    fun `fromString returns UNKNOWN for unknown types`() {
        assertThat(DocsetType.fromString("SomethingRandom")).isEqualTo(DocsetType.UNKNOWN)
        assertThat(DocsetType.fromString("")).isEqualTo(DocsetType.UNKNOWN)
        assertThat(DocsetType.fromString("XYZ123")).isEqualTo(DocsetType.UNKNOWN)
    }

    @Test
    fun `displayName returns correct values`() {
        assertThat(DocsetType.CLASS.displayName).isEqualTo("Class")
        assertThat(DocsetType.METHOD.displayName).isEqualTo("Method")
        assertThat(DocsetType.FUNCTION.displayName).isEqualTo("Function")
        assertThat(DocsetType.UNKNOWN.displayName).isEqualTo("Unknown")
    }

    @Test
    fun `all types have display names`() {
        DocsetType.entries.forEach { type ->
            assertThat(type.displayName).isNotBlank()
        }
    }
}
