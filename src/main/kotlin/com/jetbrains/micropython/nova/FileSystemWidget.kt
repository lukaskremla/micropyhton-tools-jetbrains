package com.jetbrains.micropython.nova

import com.intellij.icons.AllIcons
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ActionPlaces.TOOLWINDOW_CONTENT
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.util.Disposer
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.PopupHandler
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.EditSourceOnDoubleClickHandler
import com.intellij.util.ExceptionUtil
import com.intellij.util.asSafely
import com.intellij.util.containers.TreeTraversal
import com.intellij.util.ui.tree.TreeUtil
import com.jediterm.terminal.TtyConnector
import com.jetbrains.rd.util.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import java.io.IOException
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

private const val MPY_FS_SCAN = """
import os
class ___FSScan(object):

    def fld(self, name):
        for r in os.ilistdir(name):
            print(r[1],r[3] if len(r) > 3 else -1,name + r[0])
            if r[1] & 0x4000:
                self.fld(name + r[0]+ "/")

___FSScan().fld("/")
del ___FSScan
gc.collect()
  
"""

class FileSystemWidget(val project: Project, newDisposable: Disposable) :
    JBPanelWithEmptyText(BorderLayout()) {
    val ttyConnector: TtyConnector
        get() = comm.ttyConnector
    private val tree: Tree = Tree(newTreeModel())

    private val comm: WebSocketComm = WebSocketComm {
        thisLogger().warn(it)
        Notifications.Bus.notify(
            Notification(
                NOTIFICATION_GROUP,
                ExceptionUtil.getMessage(it) ?: it.toString(),
                NotificationType.WARNING
            )
        )
    }.also { Disposer.register(newDisposable, it) }

    val state: State
        get() = comm.state

    private fun newTreeModel() = DefaultTreeModel(DirNode("/", "/"), true)

    init {
        emptyText.appendText("Board is disconnected")
        emptyText.appendText("Connect...", SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES) {
            runWithModalProgressBlocking(project, "Connecting") {
                connect()
                refresh()
            }
        }
        tree.setCellRenderer(object : ColoredTreeCellRenderer() {

            override fun customizeCellRenderer(
                tree: JTree, value: Any?, selected: Boolean, expanded: Boolean,
                leaf: Boolean, row: Int, hasFocus: Boolean,
            ) {
                value as FileSystemNode
                icon = when (value) {
                    is DirNode -> AllIcons.Nodes.Folder
                    is FileNode -> FileTypeRegistry.getInstance().getFileTypeByFileName(value.name).icon
                }
                append(value.name)
                if (value is FileNode) {
                    append("  ${value.size} bytes", SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES)
                }

            }
        })
        val actionManager = ActionManager.getInstance()
        EditSourceOnDoubleClickHandler.install(tree) {
            val action = actionManager.getAction("micropython.repl.OpenFile")
            actionManager.tryToExecute(action, null, tree, TOOLWINDOW_CONTENT, true)
        }
        val popupActions = actionManager.getAction("micropython.repl.FSContextMenu") as ActionGroup
        PopupHandler.installFollowingSelectionTreePopup(tree, popupActions, ActionPlaces.UNKNOWN)
        TreeUtil.installActions(tree)

        val actions = actionManager.getAction("micropython.repl.FSToolbar") as ActionGroup
        val actionToolbar = actionManager.createActionToolbar(ActionPlaces.TOOLBAR, actions, true)
        actionToolbar.targetComponent = this

        add(JBScrollPane(tree), BorderLayout.CENTER)
        add(actionToolbar.component, BorderLayout.NORTH)
        comm.stateListeners.add {
            when (it) {
                State.CONNECTED, State.TTY_DETACHED -> tree.isVisible = true

                State.DISCONNECTING,
                State.DISCONNECTED,
                State.CONNECTING -> {
                    tree.model = newTreeModel()
                    tree.isVisible = false
                }
            }
        }
        tree.isVisible = false
    }

    suspend fun refresh() {
        comm.checkConnected()
        val newModel = newTreeModel()
        val dirList = comm.blindExecute(MPY_FS_SCAN).extractSingleResponse()
        dirList.lines().filter { it.isNotBlank() }.forEach { line ->
            line.split(" ").let { fields ->
                val flags = fields[0].toInt()
                val len = fields[1].toInt()
                val fullName = fields[2]
                val names = fullName.split('/')
                val folders = if (flags and 0x4000 == 0) names.dropLast(1) else names
                var currentFolder = newModel.root as DirNode
                folders.filter { it.isNotBlank() }.forEach { name ->
                    val child =
                        currentFolder.children().asSequence().find { (it as FileSystemNode).name == name }
                    when (child) {
                        is DirNode -> currentFolder = child
                        is FileNode -> Unit
                        null -> currentFolder = DirNode(fullName, name).also { currentFolder.add(it) }
                    }
                }
                if (flags and 0x4000 == 0) {
                    currentFolder.add(FileNode(fullName, names.last(), len))
                }
            }
        }
        withContext(Dispatchers.EDT) {
            val expandedPaths = TreeUtil.collectExpandedPaths(tree)
            val selectedPath = tree.selectionPath
            tree.model = newModel
            TreeUtil.restoreExpandedPaths(tree, expandedPaths)
            TreeUtil.selectPath(tree, selectedPath)
        }
    }

    suspend fun deleteCurrent() {

        comm.checkConnected()
        val confirmedFileSystemNodes = withContext(Dispatchers.EDT) {
            val fileSystemNodes = tree.selectionPaths?.mapNotNull { it.lastPathComponent.asSafely<FileSystemNode>() }
                ?.filter { it.fullName != "" && it.fullName != "/" } ?: emptyList()
            val title: String
            val message: String
            if (fileSystemNodes.isEmpty()) {
                return@withContext emptyList()
            } else if (fileSystemNodes.size == 1) {
                val fileName = fileSystemNodes[0].fullName
                if (fileSystemNodes[0] is DirNode) {
                    title = "Delete folder $fileName"
                    message = "Are you sure to delete the folder and it's subtree?\n\r The operation can't be undone!"
                } else {
                    title = "Delete file $fileName"
                    message = "Are you sure to delete the file?\n\r The operation can't be undone!"
                }
            } else {
                title = "Delete multiple objects"
                message =
                    "Are you sure to delete ${fileSystemNodes.size} items?\n\r The operation can't be undone!"
            }

            val sure = MessageDialogBuilder.yesNo(title, message).ask(project)
            if (sure) fileSystemNodes else emptyList()
        }
        for (confirmedFileSystemNode in confirmedFileSystemNodes) {
            val commands = mutableListOf("import os")
            TreeUtil.treeNodeTraverser(confirmedFileSystemNode)
                .traverse(TreeTraversal.POST_ORDER_DFS)
                .mapNotNull {
                    when (val node = it) {
                        is DirNode -> "os.rmdir('${node.fullName}')"
                        is FileNode -> "os.remove('${node.fullName}')"
                        else -> null
                    }
                }
                .toCollection(commands)
            comm.blindExecute(*commands.toTypedArray())
                .extractResponse()
        }
    }

    fun selectedFile(): FileSystemNode? {
        return tree.selectionPath?.lastPathComponent.asSafely<FileSystemNode>()
    }

    suspend fun disconnect() {
        comm.disconnect()
    }

    @Throws(IOException::class)
    suspend fun upload(relativeName: @NonNls String, contentsToByteArray: ByteArray) =
        comm.upload(relativeName, contentsToByteArray)

    @Throws(IOException::class)
    suspend fun instantRun(code: @NonNls String) = comm.instantRun(code)

    @Throws(IOException::class)
    suspend fun blindExecute(vararg commands: String): ExecResponse = comm.blindExecute(*commands)

    suspend fun connect() = comm.connect()
    fun setConnectionParams(uri: URI, password: String) = comm.setConnectionParams(uri, password)

}

sealed class FileSystemNode(@NonNls val fullName: String, @NonNls val name: String) : DefaultMutableTreeNode() {

    override fun toString(): String = name

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        return fullName == (other as FileSystemNode).fullName
    }

    override fun hashCode(): Int {
        return fullName.hashCode()
    }

}

class FileNode(fullName: String, name: String, val size: Int) : FileSystemNode(fullName, name) {
    override fun getAllowsChildren(): Boolean = false
    override fun isLeaf(): Boolean = true
}

class DirNode(fullName: String, name: String) : FileSystemNode(fullName, name) {
    override fun getAllowsChildren(): Boolean = true
}
