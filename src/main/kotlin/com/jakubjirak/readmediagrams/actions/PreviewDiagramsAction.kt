/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.readmediagrams.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.wm.ToolWindowManager
import com.jakubjirak.readmediagrams.ui.DiagramPreviewService

class PreviewDiagramsAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val ext = e.getData(CommonDataKeys.VIRTUAL_FILE)?.extension?.lowercase()
        e.presentation.isEnabledAndVisible = ext == "md" || ext == "markdown"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ToolWindowManager.getInstance(project).getToolWindow("README Diagrams")?.show {
            DiagramPreviewService.getInstance(project).renderCurrent()
        }
    }
}
