/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.readmediagrams.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.Alarm
import com.intellij.util.ui.UIUtil
import com.jakubjirak.readmediagrams.core.DiagramHtml
import com.jakubjirak.readmediagrams.core.FenceExtractor

/**
 * Owns the JCEF browser shown in the README Diagrams tool window and keeps it
 * in sync with the selected Markdown editor (debounced on typing).
 */
@Service(Service.Level.PROJECT)
class DiagramPreviewService(private val project: Project) : Disposable {

    val browser: JBCefBrowser? = if (JBCefApp.isSupported()) JBCefBrowser() else null

    private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)
    private var trackedDocument: Document? = null
    private val mermaidJs: String by lazy {
        javaClass.getResourceAsStream("/web/mermaid.min.js")?.bufferedReader()?.readText() ?: ""
    }

    private val documentListener = object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) {
            alarm.cancelAllRequests()
            alarm.addRequest({ renderCurrent() }, 500)
        }
    }

    init {
        browser?.let {
            com.intellij.openapi.util.Disposer.register(this, it)
            project.messageBus.connect(this).subscribe(
                FileEditorManagerListener.FILE_EDITOR_MANAGER,
                object : FileEditorManagerListener {
                    override fun selectionChanged(event: FileEditorManagerEvent) {
                        renderCurrent()
                    }
                }
            )
        }
    }

    fun renderCurrent() {
        val browser = browser ?: return
        val file = FileEditorManager.getInstance(project).selectedEditor?.file
        val document = file?.let { FileDocumentManager.getInstance().getDocument(it) }

        trackedDocument?.removeDocumentListener(documentListener)
        trackedDocument = document?.also { it.addDocumentListener(documentListener) }

        val html = if (file == null || !isMarkdown(file) || document == null) {
            DiagramHtml.build(emptyList(), mermaidJs, isDark(), file?.name ?: "no file")
        } else {
            DiagramHtml.build(FenceExtractor.extract(document.text), mermaidJs, isDark(), file.name)
        }
        browser.loadHTML(html)
    }

    private fun isMarkdown(file: VirtualFile): Boolean =
        file.extension?.lowercase() in setOf("md", "markdown")

    private fun isDark(): Boolean = !UIUtil.getPanelBackground().let { it.red + it.green + it.blue > 382 }

    override fun dispose() {
        trackedDocument?.removeDocumentListener(documentListener)
        trackedDocument = null
    }

    companion object {
        fun getInstance(project: Project): DiagramPreviewService =
            project.getService(DiagramPreviewService::class.java)
    }
}
