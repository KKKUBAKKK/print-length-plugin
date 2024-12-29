package com.github.kkkubakkk.printlengthplugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys

class PrintLengthAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(PlatformDataKeys.EDITOR)
        val document = editor?.document
        val text = document?.text
        val length = text?.length
        println("Length of the text is $length")
    }
}