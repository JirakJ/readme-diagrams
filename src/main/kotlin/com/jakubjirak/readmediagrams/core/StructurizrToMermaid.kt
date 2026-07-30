/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the repository root.
 */
package com.jakubjirak.readmediagrams.core

/**
 * Transpiles a practical subset of the Structurizr DSL to a Mermaid C4 diagram.
 *
 * Supported: workspace/model nesting, person, softwareSystem, container,
 * component (with optional `id =` assignment, description and technology),
 * group blocks (transparent), and `a -> b "desc" "tech"` relationships.
 * views/styles/configuration/deployment blocks are skipped — Mermaid C4
 * has its own layout. Unsupported lines are counted and reported as warnings.
 */
object StructurizrToMermaid {

    data class Result(val mermaid: String, val warnings: List<String>)

    private data class Element(
        val id: String,
        val kind: Kind,
        val name: String,
        val description: String,
        val technology: String,
        val parentId: String?
    )

    private enum class Kind { PERSON, SYSTEM, CONTAINER, COMPONENT }

    private data class Relation(val from: String, val to: String, val description: String, val technology: String)

    // id = kind "Name" "Description" "Technology" {
    private val elementLine = Regex(
        """^(?:([A-Za-z_][\w.]*)\s*=\s*)?(person|softwareSystem|container|component)\s*(?:"((?:[^"\\]|\\.)*)")?\s*(?:"((?:[^"\\]|\\.)*)")?\s*(?:"((?:[^"\\]|\\.)*)")?[^{]*(\{)?\s*$"""
    )
    private val relationLine = Regex(
        """^([A-Za-z_][\w.]*)\s*->\s*([A-Za-z_][\w.]*)\s*(?:"((?:[^"\\]|\\.)*)")?\s*(?:"((?:[^"\\]|\\.)*)")?\s*(\{)?\s*$"""
    )
    private val skippedBlocks = setOf(
        "views", "styles", "configuration", "deployment", "deploymentEnvironment",
        "properties", "perspectives", "!docs", "!adrs", "terminology", "branding", "users"
    )
    private val transparentBlocks = setOf("workspace", "model", "group", "enterprise")
    private val ignoredKeywords = setOf(
        "tags", "description", "technology", "url", "!identifiers", "!impliedRelationships",
        "!include", "!constant", "!var", "autoLayout", "include", "exclude", "animation", "title", "this"
    )

    fun transpile(dsl: String): Result {
        val elements = mutableListOf<Element>()
        val relations = mutableListOf<Relation>()
        val warnings = mutableListOf<String>()
        var anonymousCounter = 0

        // stack entry = element id owning the scope (null = structural block), plus skip depth handling
        val scopeStack = ArrayDeque<String?>()
        var skipDepth = 0

        for (rawLine in dsl.lines()) {
            val line = rawLine.substringBefore("//").substringBefore("#").trim()
            if (line.isEmpty()) continue

            if (skipDepth > 0) {
                skipDepth += line.count { it == '{' } - line.count { it == '}' }
                if (skipDepth < 0) skipDepth = 0
                continue
            }

            val keyword = line.substringBefore(' ').substringBefore('{').trim()
            if (skippedBlocks.contains(keyword)) {
                skipDepth = line.count { it == '{' } - line.count { it == '}' }
                if (skipDepth < 0) skipDepth = 0
                continue
            }

            if (line == "}") {
                if (scopeStack.isNotEmpty()) scopeStack.removeLast()
                continue
            }

            if (transparentBlocks.contains(keyword)) {
                if (line.endsWith("{")) scopeStack.addLast(null)
                continue
            }

            val el = elementLine.find(line)
            if (el != null) {
                val (rawId, kindWord, name, description, technology, brace) = el.destructured
                val kind = when (kindWord) {
                    "person" -> Kind.PERSON
                    "softwareSystem" -> Kind.SYSTEM
                    "container" -> Kind.CONTAINER
                    else -> Kind.COMPONENT
                }
                val id = sanitizeId(rawId.ifBlank { name.ifBlank { "el${++anonymousCounter}" } })
                val parentId = scopeStack.lastOrNull { it != null }
                elements.add(Element(id, kind, name.ifBlank { id }, description, technology, parentId))
                if (brace == "{") scopeStack.addLast(id)
                continue
            }

            val rel = relationLine.find(line)
            if (rel != null) {
                val (from, to, description, technology, brace) = rel.destructured
                relations.add(Relation(sanitizeId(from), sanitizeId(to), description, technology))
                if (brace == "{") {
                    skipDepth = 1
                }
                continue
            }

            if (ignoredKeywords.contains(keyword) || keyword.startsWith("!")) {
                if (line.endsWith("{")) skipDepth = 1
                continue
            }

            if (line.endsWith("{")) {
                // Unknown block — skip its contents rather than misparse them.
                skipDepth = 1
            }
            warnings.add("Unsupported DSL line ignored: ${line.take(80)}")
        }

        if (elements.isEmpty()) {
            return Result("", warnings + "No Structurizr elements found — nothing to render")
        }
        return Result(render(elements, relations), warnings)
    }

    private fun render(elements: List<Element>, relations: List<Relation>): String {
        val hasComponents = elements.any { it.kind == Kind.COMPONENT }
        val hasContainers = elements.any { it.kind == Kind.CONTAINER }
        val header = when {
            hasComponents -> "C4Component"
            hasContainers -> "C4Container"
            else -> "C4Context"
        }
        val byParent = elements.groupBy { it.parentId }
        val out = StringBuilder(header).append('\n')

        fun emit(e: Element, indent: String) {
            val children = byParent[e.id].orEmpty()
            val line = when (e.kind) {
                Kind.PERSON -> "Person(${e.id}, ${q(e.name)}${optional(e.description)})"
                Kind.SYSTEM ->
                    if (children.isNotEmpty()) "System_Boundary(${e.id}, ${q(e.name)})"
                    else "System(${e.id}, ${q(e.name)}${optional(e.description)})"
                Kind.CONTAINER ->
                    if (children.isNotEmpty()) "Container_Boundary(${e.id}, ${q(e.name)})"
                    else "Container(${e.id}, ${q(e.name)}${optional(e.technology)}${optional(e.description)})"
                Kind.COMPONENT -> "Component(${e.id}, ${q(e.name)}${optional(e.technology)}${optional(e.description)})"
            }
            out.append(indent).append(line)
            if (children.isNotEmpty()) {
                out.append(" {\n")
                children.forEach { emit(it, "$indent    ") }
                out.append(indent).append("}")
            }
            out.append('\n')
        }

        byParent[null].orEmpty().forEach { emit(it, "") }
        relations.forEach { r ->
            out.append("Rel(${r.from}, ${r.to}, ${q(r.description.ifBlank { " " })}${optional(r.technology)})\n")
        }
        return out.toString().trimEnd()
    }

    private fun sanitizeId(id: String): String = id.replace(Regex("[^\\w]"), "_")

    private fun q(text: String): String = "\"" + text.replace("\"", "'") + "\""

    private fun optional(text: String): String = if (text.isBlank()) "" else ", ${q(text)}"
}
