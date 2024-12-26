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

package com.jetbrains.micropython.settings

import com.intellij.facet.FacetConfiguration
import com.intellij.facet.ui.FacetEditorContext
import com.intellij.facet.ui.FacetEditorTab
import com.intellij.facet.ui.FacetEditorValidator
import com.intellij.facet.ui.FacetValidatorsManager
import com.jetbrains.micropython.devices.MicroPythonDeviceProvider
import org.jdom.Element

/**
 * @author vlan
 */
class MicroPythonFacetConfiguration : FacetConfiguration {
  var deviceProvider = MicroPythonDeviceProvider.default

  override fun createEditorTabs(editorContext: FacetEditorContext, validatorsManager: FacetValidatorsManager): Array<FacetEditorTab> {
    val facet = editorContext.facet as MicroPythonFacet
    validatorsManager.registerValidator(object: FacetEditorValidator() {
      override fun check() = facet.checkValid()
    })
    return arrayOf(MicroPythonFacetEditorTab(this, facet))
  }

  override fun readExternal(element: Element?) {
    val deviceName = element?.getChild("device")?.getAttribute("name")?.value
    val device = MicroPythonDeviceProvider.providers.firstOrNull { it.persistentName == deviceName }
    deviceProvider = device ?: MicroPythonDeviceProvider.default
  }

  override fun writeExternal(element: Element?) {
    val deviceElement = Element("device")
    deviceElement.setAttribute("name", deviceProvider.persistentName)
    element?.addContent(deviceElement)
  }
}