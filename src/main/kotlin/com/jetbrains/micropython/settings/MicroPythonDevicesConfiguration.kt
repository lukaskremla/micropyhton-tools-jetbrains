package com.jetbrains.micropython.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Attribute

/**
 * @author vlan
 */
@State(name = "MicroPythonDevices", storages = arrayOf(Storage(StoragePathMacros.WORKSPACE_FILE)))
class MicroPythonDevicesConfiguration : PersistentStateComponent<MicroPythonDevicesConfiguration> {

  companion object {
    fun getInstance(project: Project): MicroPythonDevicesConfiguration =
        ServiceManager.getService(project, MicroPythonDevicesConfiguration::class.java)
  }

  // Currently the device path is stored per project, not per module
  @Attribute var devicePath: String = ""

  override fun getState() = this

  override fun loadState(state: MicroPythonDevicesConfiguration) {
    XmlSerializerUtil.copyBean(state, this)
  }
}
