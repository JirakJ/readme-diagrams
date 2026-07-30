/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the repository root.
 */
package com.jakubjirak.readmediagrams.ui

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.icons.AllIcons
import com.intellij.ui.components.JBLabel
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JPanel

class DiagramPreviewToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val service = DiagramPreviewService.getInstance(project)
        val panel = JPanel(BorderLayout())

        val browser = service.browser
        if (browser == null) {
            panel.border = JBUI.Borders.empty(12)
            panel.add(
                JBLabel(
                    "<html><b>JCEF is not available in this IDE.</b><br>" +
                        "Diagram preview requires the embedded browser (default in all " +
                        "JetBrains runtimes since 2020.2; check that the IDE runs with JBR).</html>"
                ),
                BorderLayout.NORTH
            )
        } else {
            panel.add(browser.component, BorderLayout.CENTER)
        }

        toolWindow.setTitleActions(listOf(object : AnAction("Refresh", "Re-render diagrams", AllIcons.Actions.Refresh) {
            override fun actionPerformed(e: AnActionEvent) = service.renderCurrent()
        }))

        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
        service.renderCurrent()
    }
}
