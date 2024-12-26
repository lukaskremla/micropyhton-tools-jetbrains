/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package com.jetbrains.micropython.run

import com.intellij.execution.Executor
import com.intellij.execution.configuration.AbstractRunConfiguration
import com.intellij.execution.configuration.EmptyRunProfileState
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfigurationWithSuppressedDefaultDebugAction
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.facet.ui.ValidationResult
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessModuleDir
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.project.stateStore
import com.intellij.util.PathUtil
import com.intellij.util.PlatformUtils
import com.jetbrains.micropython.nova.fileSystemWidget
import com.jetbrains.micropython.settings.MicroPythonProjectConfigurable
import com.jetbrains.micropython.settings.microPythonFacet
import com.jetbrains.python.sdk.PythonSdkUtil
import org.jdom.Element
import java.nio.file.Path

/**
 * @author Mikhail Golubev
 */

class MicroPythonRunConfiguration(project: Project, factory: ConfigurationFactory) : AbstractRunConfiguration(project, factory), RunConfigurationWithSuppressedDefaultDebugAction {

  var path: String = ""
  var runReplOnSuccess: Boolean = false
  override fun getValidModules() =
          allModules.filter { it.microPythonFacet != null }.toMutableList()

  override fun getConfigurationEditor() = MicroPythonRunConfigurationEditor(this)

  private fun getClosestRoot(file: VirtualFile, roots: Set<VirtualFile>, module: Module): VirtualFile? {
    var parent: VirtualFile? = file
    while (parent != null) {
      if (parent in roots) {
        break
      }
      parent = parent.parent
    }
    return parent ?: module.guessModuleDir()
  }
//todo upload folder or project
  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
    val uploadFile = VfsUtil.findFile(Path.of(path), true) ?: return null
    val currentModule = environment.dataContext?.getData(LangDataKeys.MODULE)
      ?: module
      ?: return null

    val roots = mutableSetOf<VirtualFile>()
    val rootManager = currentModule.rootManager
    roots.addAll(rootManager.sourceRoots)
    if (roots.isEmpty()) {
      roots.addAll(rootManager.contentRoots)
    }

    val pythonPath = PythonSdkUtil.findPythonSdk(currentModule)?.homeDirectory
//    val devicePath = facet.getOrDetectDevicePathSynchronously() ?: return null
    val ideaDir = project.stateStore.directoryStorePath?.let { VfsUtil.findFile(it, false) }
    val excludeRoots = listOfNotNull(
      pythonPath,
      ideaDir,
      *ModuleRootManager.getInstance(currentModule).excludeRoots
    )

    val filesToUpload = mutableListOf<Pair<String, VirtualFile>>()
    VfsUtil.processFileRecursivelyWithoutIgnored(uploadFile) { file ->
      if (
        file.isFile && file.isValid &&
        excludeRoots.none { VfsUtil.isAncestor(it, file, false) }
      ) {
        getClosestRoot(file, roots, currentModule)?.apply {
          val shortPath = VfsUtil.getRelativePath(file, this)
          if (shortPath != null) filesToUpload.add(shortPath to file)//todo optimize
        }
      }
      true
    }
    //todo low priority create empty folders
    //todo optionally open repl
//    ComponentManagerImpl
//    if (runReplOnSuccess && state != null) {
//      return RunStateWrapper(state) {
//        ApplicationManager.getApplication().invokeLater {
//          project.service<MicroPythonReplManager>().startOrRestartRepl(false)
//          ToolWindowManager.getInstance(project).getToolWindow("MicroPython")?.show()
//        }
//      }
//    }
    val fileSystemWidget = fileSystemWidget(project) ?: return null
    runWithModalProgressBlocking(project, "Upload ${filesToUpload.size} files") {
      reportSequentialProgress(filesToUpload.size) { reporter ->
        filesToUpload.forEach { (path, file) ->
          reporter.itemStep(path)
          fileSystemWidget.upload(path, file.contentsToByteArray())
        }
      }
      fileSystemWidget.refresh()
    }
    return EmptyRunProfileState.INSTANCE
  }

  override fun checkConfiguration() {
    super.checkConfiguration()
    if (StringUtil.isEmpty(path)) {
      throw RuntimeConfigurationError("Path is not specified")
    }
    val m = module ?: throw RuntimeConfigurationError("Module for path is not found")
    val showSettings = Runnable {
      when {
        PlatformUtils.isPyCharm() ->
          ShowSettingsUtil.getInstance().showSettingsDialog(project, MicroPythonProjectConfigurable::class.java)
        PlatformUtils.isIntelliJ() ->
          ProjectSettingsService.getInstance(project).openModuleSettings(module)
        else ->
          ShowSettingsUtil.getInstance().showSettingsDialog(project)
      }
    }
    val facet = m.microPythonFacet ?: throw RuntimeConfigurationError(
            "MicroPython support is not enabled for selected module in IDE settings",
            showSettings
    )
    val validationResult = facet.checkValid()
    if (validationResult != ValidationResult.OK) {
      val runQuickFix = Runnable {
        validationResult.quickFix.run(null)
      }
      throw RuntimeConfigurationError(validationResult.errorMessage, runQuickFix)
    }
    facet.pythonPath ?: throw RuntimeConfigurationError("Python interpreter is not found")
    if (!facet.autoDetectDevicePath && facet.devicePath == null) {
      throw RuntimeConfigurationError("Device path is not specified in IDE settings", showSettings)
    }
  }

  override fun suggestedName() = "Flash ${PathUtil.getFileName(path)}"

  override fun writeExternal(element: Element) {
    super.writeExternal(element)
    element.setAttribute("path", path)
    element.setAttribute("runReplOnSuccess", if (runReplOnSuccess) "yes" else "no")
  }

  override fun readExternal(element: Element) {
    super.readExternal(element)
    configurationModule.readExternal(element)
    element.getAttributeValue("path")?.let {
      path = it
    }
    element.getAttributeValue("runReplOnSuccess")?.let {
      runReplOnSuccess = it == "yes"
    }
  }

  val module: Module?
    get() {
      val file = StandardFileSystems.local().findFileByPath(path) ?: return null
      return ModuleUtil.findModuleForFile(file, project)
    }
}
