/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the repository root.
 */
package com.jakubjirak.readmediagrams.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FenceExtractorTest {

    @Test
    fun `extracts mermaid and structurizr fences with line numbers`() {
        val md = """
            # Title

            ```mermaid
            graph TD
              A --> B
            ```

            Some text.

            ```structurizr
            workspace { }
            ```
        """.trimIndent()
        val fences = FenceExtractor.extract(md)
        assertEquals(2, fences.size)
        assertEquals("mermaid", fences[0].language)
        assertEquals("graph TD\n  A --> B", fences[0].code)
        assertEquals(2, fences[0].startLine)
        assertEquals("structurizr", fences[1].language)
    }

    @Test
    fun `ignores other languages and plain fences`() {
        val md = "```kotlin\nval x = 1\n```\n```\nplain\n```"
        assertTrue(FenceExtractor.extract(md).isEmpty())
    }

    @Test
    fun `supports tilde fences and longer closing runs`() {
        val md = "~~~mermaid\npie\n  \"a\": 1\n~~~~"
        val fences = FenceExtractor.extract(md)
        assertEquals(1, fences.size)
        assertTrue(fences[0].code.startsWith("pie"))
    }

    @Test
    fun `backtick content inside fence does not close it`() {
        val md = "```mermaid\ngraph TD\n  A[\"uses `code`\"]\n```"
        val fences = FenceExtractor.extract(md)
        assertEquals(1, fences.size)
        assertTrue(fences[0].code.contains("`code`"))
    }

    @Test
    fun `unclosed fence extends to end of file`() {
        val md = "```mermaid\ngraph TD"
        val fences = FenceExtractor.extract(md)
        assertEquals(1, fences.size)
        assertEquals("graph TD", fences[0].code)
    }

    @Test
    fun `case-insensitive language tag`() {
        val fences = FenceExtractor.extract("```Mermaid\ngraph TD\n```")
        assertEquals(1, fences.size)
        assertEquals("mermaid", fences[0].language)
    }
}
