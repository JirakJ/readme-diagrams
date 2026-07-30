/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the repository root.
 */
package com.jakubjirak.readmediagrams.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DiagramHtmlTest {

    @Test
    fun `builds page with mermaid sections and escaped content`() {
        val fences = listOf(FenceExtractor.Fence("mermaid", "graph TD\n  A-->B", 4))
        val html = DiagramHtml.build(fences, "/*js*/", dark = true, fileName = "README.md")
        assertTrue(html.contains("class='mermaid'"))
        assertTrue(html.contains("A--&gt;B"))
        assertTrue(html.contains("theme: \"dark\""))
        assertTrue(html.contains("mermaid · line 5"))
    }

    @Test
    fun `structurizr fences are transpiled and warnings surfaced`() {
        val fences = listOf(FenceExtractor.Fence("structurizr", "model {\n  a = person \"A\"\n  bogus line here\n}", 0))
        val html = DiagramHtml.build(fences, "", dark = false, fileName = "README.md")
        assertTrue(html.contains("Person(a"))
        assertTrue(html.contains("warning"))
    }

    @Test
    fun `empty fence list renders hint`() {
        val html = DiagramHtml.build(emptyList(), "", dark = false, fileName = "README.md")
        assertTrue(html.contains("No <code>"))
    }

    @Test
    fun `html in diagram code cannot inject markup`() {
        val fences = listOf(FenceExtractor.Fence("mermaid", "graph TD\n  A[\"<script>alert(1)</script>\"]", 0))
        val html = DiagramHtml.build(fences, "", dark = false, fileName = "x.md")
        assertFalse(html.contains("<script>alert"))
        assertTrue(html.contains("&lt;script&gt;alert"))
    }
}
