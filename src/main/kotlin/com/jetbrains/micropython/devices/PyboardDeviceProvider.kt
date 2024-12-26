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

package com.jetbrains.micropython.devices

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.jetbrains.micropython.run.MicroPythonRunConfiguration
import com.jetbrains.micropython.settings.MicroPythonFacet
import com.jetbrains.micropython.settings.MicroPythonTypeHints
import com.jetbrains.micropython.settings.MicroPythonUsbId
import com.jetbrains.micropython.settings.microPythonFacet
import com.jetbrains.python.packaging.PyRequirement

/**
 * @author stefanhoelzl
 */
class PyboardDeviceProvider : MicroPythonDeviceProvider {
  override val persistentName: String
    get() = "Pyboard"

  override val documentationURL: String
    get() = "https://github.com/vlasovskikh/intellij-micropython/wiki/Pyboard"

  override val usbIds: List<MicroPythonUsbId>
    get() = listOf(MicroPythonUsbId(0xF055, 0x9800))

  override val typeHints: MicroPythonTypeHints by lazy {
    MicroPythonTypeHints(listOf("stdlib", "micropython"))
  }

  override val packageRequirements: List<PyRequirement> by lazy {
    PyRequirement.fromText("""|pyserial>=3.3,<4.0
                              |docopt>=0.6.2,<0.7
                              |adafruit-ampy>=1.0.1,<1.1""".trimMargin())
  }

  override fun getRunCommandLineState(configuration: MicroPythonRunConfiguration,
                                      environment: ExecutionEnvironment): CommandLineState? {
    val facet = configuration.module?.microPythonFacet ?: return null
    val pythonPath = facet.pythonPath ?: return null
    val devicePath = facet.devicePath ?: return null
    val rootPath = configuration.project.basePath ?: return null
    return object : CommandLineState(environment) {
      override fun startProcess() =
          OSProcessHandler(GeneralCommandLine(pythonPath,
                                              "${MicroPythonFacet.scriptsPath}/microupload.py",
                                              "-C",
                                              rootPath,
                                              "-v",
                                              devicePath,
                                              configuration.path))
    }
  }
}