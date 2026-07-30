/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.readmediagrams.core

/**
 * Builds the self-contained preview page: bundled mermaid.js inlined,
 * one section per extracted fence, IDE-theme-aware colors.
 */
object DiagramHtml {

    fun build(fences: List<FenceExtractor.Fence>, mermaidJs: String, dark: Boolean, fileName: String): String {
        val theme = if (dark) "dark" else "default"
        val bg = if (dark) "#2b2d30" else "#ffffff"
        val fg = if (dark) "#dfe1e5" else "#000000"
        val border = if (dark) "#43454a" else "#d0d0d0"

        val sections = StringBuilder()
        if (fences.isEmpty()) {
            sections.append(
                "<p class='empty'>No <code>```mermaid</code> or <code>```structurizr</code> " +
                    "code blocks found in <b>${escape(fileName)}</b>.</p>"
            )
        }
        fences.forEachIndexed { index, fence ->
            val (diagram, warnings) = when (fence.language) {
                "structurizr" -> {
                    val result = StructurizrToMermaid.transpile(fence.code)
                    result.mermaid to result.warnings
                }
                else -> fence.code to emptyList()
            }
            sections.append("<div class='block'>")
            sections.append("<div class='label'>${fence.language} · line ${fence.startLine + 1}</div>")
            if (diagram.isNotBlank()) {
                sections.append("<pre class='mermaid' id='d$index'>${escape(diagram)}</pre>")
            }
            if (warnings.isNotEmpty()) {
                sections.append("<details class='warn'><summary>${warnings.size} warning(s)</summary><ul>")
                warnings.forEach { sections.append("<li>${escape(it)}</li>") }
                sections.append("</ul></details>")
            }
            sections.append("</div>")
        }

        return """<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
  body { background: $bg; color: $fg; font-family: sans-serif; margin: 12px; }
  .block { border: 1px solid $border; border-radius: 8px; padding: 8px 12px; margin-bottom: 16px; }
  .label { font-size: 11px; opacity: 0.65; margin-bottom: 6px; text-transform: uppercase; letter-spacing: 0.05em; }
  .warn summary { cursor: pointer; font-size: 12px; opacity: 0.8; }
  .warn ul { font-size: 12px; }
  .empty { opacity: 0.7; }
  pre.mermaid { background: transparent; overflow-x: auto; }
</style>
</head>
<body>
$sections
<script>$mermaidJs</script>
<script>
  mermaid.initialize({ startOnLoad: true, theme: "$theme", securityLevel: "strict" });
</script>
</body>
</html>"""
    }

    fun escape(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
