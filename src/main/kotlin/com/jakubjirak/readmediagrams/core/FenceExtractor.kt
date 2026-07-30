/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the repository root.
 */
package com.jakubjirak.readmediagrams.core

/**
 * Extracts fenced code blocks from Markdown text (CommonMark rules:
 * ``` or ~~~ fences, up to 3 leading spaces, closing run at least as
 * long as the opening one, same fence character).
 */
object FenceExtractor {

    data class Fence(val language: String, val code: String, val startLine: Int)

    val SUPPORTED_LANGUAGES = setOf("mermaid", "structurizr")

    private val openingFence = Regex("""^ {0,3}(`{3,}|~{3,})\s*([A-Za-z0-9_+-]*)\s*$""")

    fun extract(markdown: String): List<Fence> {
        val fences = mutableListOf<Fence>()
        val lines = markdown.lines()
        var i = 0
        while (i < lines.size) {
            val open = openingFence.find(lines[i])
            if (open == null) {
                i++
                continue
            }
            val marker = open.groupValues[1]
            val language = open.groupValues[2].lowercase()
            val closing = Regex("""^ {0,3}${Regex.escape(marker.first().toString())}{${marker.length},}\s*$""")
            val body = StringBuilder()
            var j = i + 1
            while (j < lines.size && !closing.matches(lines[j])) {
                body.appendLine(lines[j])
                j++
            }
            if (language in SUPPORTED_LANGUAGES) {
                fences.add(Fence(language, body.toString().trimEnd('\n'), i))
            }
            i = if (j < lines.size) j + 1 else j
        }
        return fences
    }
}
