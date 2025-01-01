/*
 * Copyright 2000-2024 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.micropythontools.intellij.nova

import com.intellij.icons.AllIcons
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.ActionUpdateThread.BGT
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Toggleable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.openapi.vfs.readText
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.PathUtilRt
import com.intellij.util.PathUtilRt.Platform
import com.intellij.util.asSafely
import com.jetbrains.python.PythonFileType
import dev.micropythontools.intellij.run.MpyRunConfiguration
import dev.micropythontools.intellij.settings.MpyProjectConfigurable
import dev.micropythontools.intellij.settings.mpyFacet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.charset.StandardCharsets
import kotlin.coroutines.cancellation.CancellationException

/**
 * @author elmot
 */
fun fileSystemWidget(project: Project?): FileSystemWidget? {
    return ToolWindowManager.getInstance(project ?: return null)
        .getToolWindow(TOOL_WINDOW_ID)
        ?.contentManager
        ?.contents
        ?.firstNotNullOfOrNull { it.component.asSafely<FileSystemWidget>() }
}

abstract class ReplAction(
    text: String,
    private val connectionRequired: Boolean,
    private val requiresRefreshAfter: Boolean,
    private val cancelledMessage: String = "",
) : DumbAwareAction(text) {

    abstract val actionDescription: @NlsContexts.DialogMessage String

    @Throws(IOException::class, TimeoutCancellationException::class, CancellationException::class)
    abstract suspend fun performAction(fileSystemWidget: FileSystemWidget)

    final override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        var wasCancelled = false

        try {
            performReplAction(project, connectionRequired, actionDescription, cancelledMessage, this::performAction)
        } catch (e: CancellationException) {
            wasCancelled = true
            throw e
        } finally {
            if (requiresRefreshAfter && !wasCancelled) {
                runWithModalProgressBlocking(project, "Updating file system view...") {
                    fileSystemWidget(project)?.refresh()
                }
            }
        }
    }

    protected fun fileSystemWidget(e: AnActionEvent): FileSystemWidget? = fileSystemWidget(e.project)

    fun enableIfConnected(e: AnActionEvent) {
        if (fileSystemWidget(e)?.state != State.CONNECTED) {
            e.presentation.isEnabled = false
        }
    }
}

// Overload to allow not specifying cancellation message
fun <T> performReplAction(
    project: Project,
    connectionRequired: Boolean,
    @NlsContexts.DialogMessage description: String,
    action: suspend (FileSystemWidget) -> T
): T? {
    return performReplAction(
        project,
        connectionRequired,
        description,
        cancelledMessage = "$description cancelled",
        action
    )
}

fun <T> performReplAction(
    project: Project,
    connectionRequired: Boolean,
    @NlsContexts.DialogMessage description: String,
    cancelledMessage: String,
    action: suspend (FileSystemWidget) -> T
): T? {
    val fileSystemWidget = fileSystemWidget(project) ?: return null
    if (connectionRequired && fileSystemWidget.state != State.CONNECTED) {
        if (!MessageDialogBuilder.yesNo("No device is connected", "Connect now?").ask(project)) {
            return null
        }
    }
    var result: T? = null
    runWithModalProgressBlocking(project, "Exchanging data with the board...") {
        var error: String? = null
        var errorType = NotificationType.ERROR
        try {
            if (connectionRequired) {
                doConnect(fileSystemWidget)
            }
            result = action(fileSystemWidget)
        } catch (e: TimeoutCancellationException) {
            error = "$description timed out"
            thisLogger().info(error, e)
        } catch (e: CancellationException) {
            error = cancelledMessage
            thisLogger().info(error, e)
            errorType = NotificationType.INFORMATION
        } catch (e: IOException) {
            error = "$description I/O error - ${e.localizedMessage ?: e.message ?: "No message"}"
            thisLogger().info(error, e)
        } catch (e: Exception) {
            error = e.localizedMessage ?: e.message
            error = if (error.isNullOrBlank()) "$description error - ${e::class.simpleName}"
            else "$description error - ${e::class.simpleName}: $error"
            thisLogger().error(error, e)
        }
        if (!error.isNullOrBlank()) {
            Notifications.Bus.notify(Notification(NOTIFICATION_GROUP, error, errorType), project)
        }
    }
    return result
}

class Refresh : ReplAction("Refresh", false, false) { //todo optimize
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override val actionDescription: String = "Refresh"

    override fun update(e: AnActionEvent) = enableIfConnected(e)

    override suspend fun performAction(fileSystemWidget: FileSystemWidget) = fileSystemWidget.refresh()
}

class Disconnect(text: String = "Disconnect") : ReplAction(text, false, false), Toggleable {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override val actionDescription: String = "Disconnect"

    override suspend fun performAction(fileSystemWidget: FileSystemWidget) = fileSystemWidget.disconnect()

    override fun update(e: AnActionEvent) {
        if (fileSystemWidget(e)?.state != State.CONNECTED) {
            e.presentation.isEnabledAndVisible = false
        } else {
            Toggleable.setSelected(e.presentation, true)
        }
    }
}

class DeleteFiles : ReplAction("Delete Item(s)", true, true) {
    override val actionDescription: String = "Delete"

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override suspend fun performAction(fileSystemWidget: FileSystemWidget) {
        fileSystemWidget.deleteCurrent()
    }

    override fun update(e: AnActionEvent) {
        if (fileSystemWidget(e)?.state != State.CONNECTED) {
            e.presentation.isEnabled = false
            return
        }
        val selectedFiles = fileSystemWidget(e)?.selectedFiles()
        e.presentation.isEnabled = selectedFiles?.any { !it.isRoot } == true
    }
}

class InstantRun : DumbAwareAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        e.presentation.isEnabled = file != null &&
                !file.isDirectory &&
                files?.size == 1 &&
                (file.fileType == PythonFileType.INSTANCE ||
                        file.extension == "mpy")
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        FileDocumentManager.getInstance().saveAllDocuments()
        val code = e.getData(CommonDataKeys.VIRTUAL_FILE)?.readText() ?: return
        performReplAction(project, true, "Run code") {
            it.instantRun(code, false)
        }
    }
}

class InstantFragmentRun : ReplAction("Instant Run", true, false) {
    override val actionDescription: String = "Run code"
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        val editor = editor(e.project)
        if (editor == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }
        val emptySelection = editor.selectionModel.getSelectedText(true).isNullOrBlank()
        e.presentation.text =
            if (emptySelection) "Execute Line in Micropython REPL" else "Execute Selection in Micropython REPL"
    }

    private fun editor(project: Project?): Editor? =
        project?.let { FileEditorManager.getInstance(it).selectedTextEditor }

    override suspend fun performAction(fileSystemWidget: FileSystemWidget) {
        val code = withContext(Dispatchers.EDT) {
            val editor = editor(fileSystemWidget.project) ?: return@withContext null
            var text = editor.selectionModel.getSelectedText(true)
            if (text.isNullOrBlank()) {
                try {
                    val range = EditorUtil.calcCaretLineTextRange(editor)
                    if (!range.isEmpty) {
                        text = editor.document.getText(range).trim()
                    }
                } catch (_: Throwable) {
                }
            }
            text
        }
        if (!code.isNullOrBlank()) {
            fileSystemWidget.instantRun(code, true)
        }
    }
}

class OpenMpyFile : ReplAction("Open file", true, false) {
    override val actionDescription: String = "Open file"

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        val fileSystemWidget = fileSystemWidget(e)
        e.presentation.isEnabledAndVisible = fileSystemWidget != null &&
                fileSystemWidget.state == State.CONNECTED &&
                fileSystemWidget.selectedFiles().any { it is FileNode }
    }

    override suspend fun performAction(fileSystemWidget: FileSystemWidget) {

        val selectedFiles = withContext(Dispatchers.EDT) {
            fileSystemWidget.selectedFiles().mapNotNull { it as? FileNode }
        }
        for (file in selectedFiles) {
            var text = fileSystemWidget.download(file.fullName).toString(StandardCharsets.UTF_8)
            withContext(Dispatchers.EDT) {
                var fileType = FileTypeRegistry.getInstance().getFileTypeByFileName(file.name)
                if (fileType.isBinary) {
                    fileType = PlainTextFileType.INSTANCE
                } else {
                    //hack for LightVirtualFile and line endings
                    text = StringUtilRt.convertLineSeparators(text)
                }
                val selectedFile = LightVirtualFile("micropython: ${file.fullName}", fileType, text)
                selectedFile.isWritable = false
                FileEditorManager.getInstance(fileSystemWidget.project).openFile(selectedFile, true, true)
            }
        }
    }
}

open class UploadFile : DumbAwareAction("Upload Selected to MicroPython Device") {
    override fun getActionUpdateThread(): ActionUpdateThread = BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (project != null && files != null) {
            var directoryCount = 0
            var fileCount = 0

            for (file in files.iterator()) {
                if (file == null || !file.isInLocalFileSystem || ModuleUtil.findModuleForFile(
                        file,
                        project
                    )?.mpyFacet == null
                ) {
                    return
                }

                if (file.isDirectory) {
                    directoryCount++
                } else {
                    fileCount++
                }
            }

            if (fileCount == 0) {
                if (directoryCount == 1) {
                    e.presentation.text = "Upload Directory to MicroPython Device"
                } else {
                    e.presentation.text = "Upload Directories to MicroPython Device"
                }
            } else if (directoryCount == 0) {
                if (fileCount == 1) {
                    e.presentation.text = "Upload File to MicroPython Device"
                } else {
                    e.presentation.text = "Upload Files to MicroPython Device"
                }
            } else {
                e.presentation.text = "Upload Items to MicroPython Device"
            }
        } else {
            e.presentation.isEnabledAndVisible = false
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        FileDocumentManager.getInstance().saveAllDocuments()
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (files != null) {
            MpyRunConfiguration.uploadItems(e.project ?: return, files.toSet())
        }
    }
}

class OpenSettingsAction : DumbAwareAction("Settings") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ShowSettingsUtil.getInstance().showSettingsDialog(project, MpyProjectConfigurable::class.java)
    }
}

class InterruptAction : ReplAction("Interrupt", true, false) {
    override fun getActionUpdateThread(): ActionUpdateThread = BGT
    override fun update(e: AnActionEvent) = enableIfConnected(e)
    override val actionDescription: @NlsContexts.DialogMessage String = "Interrupt..."

    override suspend fun performAction(fileSystemWidget: FileSystemWidget) {
        fileSystemWidget.interrupt()
    }
}

class SoftResetAction : ReplAction("Reset", true, false) {
    override fun getActionUpdateThread(): ActionUpdateThread = BGT
    override fun update(e: AnActionEvent) = enableIfConnected(e)
    override val actionDescription: @NlsContexts.DialogMessage String = "Reset..."

    override suspend fun performAction(fileSystemWidget: FileSystemWidget) {
        fileSystemWidget.reset()
        fileSystemWidget.clearTerminalIfNeeded()
    }
}

class CreateDeviceFolderAction : ReplAction("New Folder", true, false) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    private fun selectedFolder(fileSystemWidget: FileSystemWidget): DirNode? =
        fileSystemWidget.selectedFiles().firstOrNull().asSafely<DirNode>()

    override fun update(e: AnActionEvent) {
        val fileSystemWidget = fileSystemWidget(e)
        if (fileSystemWidget?.state != State.CONNECTED || selectedFolder(fileSystemWidget) == null) {
            e.presentation.isEnabled = false
        }
    }

    override val actionDescription: @NlsContexts.DialogMessage String = "Creating new folder..."

    override suspend fun performAction(fileSystemWidget: FileSystemWidget) {

        val parent = selectedFolder(fileSystemWidget) ?: return

        val validator = object : InputValidator {
            override fun checkInput(inputString: String): Boolean {
                if (!PathUtilRt.isValidFileName(inputString, Platform.UNIX, true, Charsets.US_ASCII)) return false
                return parent.children().asSequence().none { it.asSafely<FileSystemNode>()?.name == inputString }
            }

            override fun canClose(inputString: String): Boolean = checkInput(inputString)
        }

        val newName = withContext(Dispatchers.EDT) {
            Messages.showInputDialog(
                fileSystemWidget.project,
                "Name:", "Create New Folder", AllIcons.Actions.AddDirectory,
                "new_folder", validator
            )
        }

        if (!newName.isNullOrBlank()) {
            fileSystemWidget.blindExecute(TIMEOUT, "import os; os.mkdir('${parent.fullName}/$newName')")
                .extractSingleResponse()
            fileSystemWidget.refresh()
        }
    }
}