package com.github.kkkubakkk.printlengthplugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.ui.RunContentManager
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.util.Disposer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class PrintLengthAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)

        if (project == null || file == null) {
            return
        }

        // Get the selected text
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val selectionModel = editor.selectionModel
        val content = selectionModel.selectedText ?: ""

        // Save content to a temporary file
        val tempFilePath = "${System.getProperty("java.io.tmpdir")}/temp_content.txt"
        try {
            Files.write(
                Paths.get(tempFilePath),
                content.toByteArray(StandardCharsets.UTF_8)
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            return
        }

        // Extract Python script to a temporary location
        val tempScriptPath = "${System.getProperty("java.io.tmpdir")}/count_letters.py"
        try {
            val scriptStream = javaClass.getResourceAsStream("/scripts/count_letters.py") ?: return
            Files.copy(scriptStream, Paths.get(tempScriptPath), StandardCopyOption.REPLACE_EXISTING)
        } catch (ex: Exception) {
            ex.printStackTrace()
            return
        }

        // Prepare Python script execution
//        val pythonScript = javaClass.getResource("/scripts/count_letters.py")?.path ?: return
        val commandLine = GeneralCommandLine().apply {
            exePath = "python3"
            addParameter(tempScriptPath)
            addParameter(tempFilePath)
        }

        try {
            // Create console view
            val consoleView = com.intellij.execution.impl.ConsoleViewImpl(project, true)
            val processHandler = OSProcessHandler(commandLine)

            // Create descriptor for the console content
            val descriptor = RunContentDescriptor(
                consoleView,
                processHandler,
                consoleView.component,
                "Letter Count Results"
            )

            // Show console in the Run tool window
            RunContentManager.getInstance(project).showRunContent(DefaultRunExecutor.getRunExecutorInstance(), descriptor)

            // Attach console to process and start
            consoleView.attachToProcess(processHandler)
            processHandler.startNotify()

            // Ensure proper disposal
            Disposer.register(project, consoleView)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val selectionModel = editor?.selectionModel
        val hasSelection = selectionModel?.hasSelection() ?: false
        e.presentation.isEnabledAndVisible = hasSelection
    }
}