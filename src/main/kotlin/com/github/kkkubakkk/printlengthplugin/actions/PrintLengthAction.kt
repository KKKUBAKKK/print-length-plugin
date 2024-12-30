package com.github.kkkubakkk.printlengthplugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.ui.RunContentManager
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Disposer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import com.jetbrains.python.sdk.PythonSdkType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

class PrintLengthAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        if (project == null || file == null) {
            Messages.showErrorDialog(project, "Project or file is null", "Error")
            return
        }

        // Get the selected text
        val content = getSelectedText(e)
        if (content.isEmpty()) {
            Messages.showErrorDialog(project, "No text selected", "Error")
            return
        }

        // Save content to a temporary file
        val tempFilePath = saveContentToFile(content)
        if (tempFilePath.isEmpty()) {
            Messages.showErrorDialog(project, "Failed to save content to a temporary file", "Error")
            return
        }

        // Extract Python script to a temporary location
        val tempScriptPath = extractScript()
        if (tempScriptPath.isEmpty()) {
            Messages.showErrorDialog(project, "Failed to extract Python script", "Error")
            return
        }
        
        // Get the Python interpreter path from the project SDK
        val pythonInterpreterPath = getPythonInterpreterPath(project)
        if (pythonInterpreterPath.isEmpty()) {
            Messages.showErrorDialog(project, "Python interpreter not found", "Error")
            return
        }

        // Prepare Python script execution
        val commandLine = preparePythonScriptExecution(pythonInterpreterPath, tempScriptPath, tempFilePath)

        // Execute Python script
        executePythonScript(project, commandLine)
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val selectionModel = editor?.selectionModel
        val hasSelection = selectionModel?.hasSelection() ?: false
        e.presentation.isVisible = true
        e.presentation.isEnabled = hasSelection
        
        if (e.project == null) {
            e.presentation.isEnabled = false
        }
        val interpreter = getPythonInterpreterPath(e.project!!)
        if (interpreter.isEmpty()) {
            e.presentation.isEnabled = false
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
    
    // Get the selected text
    private fun getSelectedText(e: AnActionEvent): String {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return ""
        val selectionModel = editor.selectionModel
        return selectionModel.selectedText ?: ""
    }
    
    // Dave content to a temporary file
    private fun saveContentToFile(content: String): String {
        val tempFilePath = "${System.getProperty("java.io.tmpdir")}/temp_content.txt"
        try {
            Files.write(
                Paths.get(tempFilePath),
                content.toByteArray(StandardCharsets.UTF_8)
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            return ""
        }
        return tempFilePath
    }
    
    // Extract Python script to a temporary location
    private fun extractScript(): String {
        val tempScriptPath = "${System.getProperty("java.io.tmpdir")}/count_letters.py"
        try {
            val scriptStream = javaClass.getResourceAsStream("/scripts/count_letters.py") ?: return ""
            Files.copy(scriptStream, Paths.get(tempScriptPath), StandardCopyOption.REPLACE_EXISTING)
        } catch (ex: Exception) {
            ex.printStackTrace()
            return ""
        }
        return tempScriptPath
    }
    
    // Get the python interpreter path from the project SDK
    private fun getPythonInterpreterPath(project: Project): String {
        val sdk: Sdk? = ProjectRootManager.getInstance(project).projectSdk
        return if (sdk != null && sdk.sdkType is PythonSdkType) {
            sdk.homePath ?: ""
        } else {
            ""
        }
    }
    
    // Prepare python script execution using all acquired data
    private fun preparePythonScriptExecution(pythonInterpreterPath: String, 
                                             tempScriptPath: String, 
                                             tempFilePath: String
    ): GeneralCommandLine {
        return GeneralCommandLine().apply {
            exePath = pythonInterpreterPath
            addParameter(tempScriptPath)
            addParameter(tempFilePath)
        }
    }
    
    // Execute Python script
    private fun executePythonScript(project: Project, commandLine: GeneralCommandLine) {
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
            Disposer.register(descriptor, consoleView)
        } catch (ex: Exception) {
            Messages.showErrorDialog(project, "Failed to execute Python script: ${ex.message}", "Error")
        }
    }
}