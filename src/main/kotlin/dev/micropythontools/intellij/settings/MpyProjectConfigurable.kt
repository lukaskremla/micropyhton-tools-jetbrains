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

package dev.micropythontools.intellij.settings

import com.intellij.application.options.ModuleAwareProjectConfigurable
import com.intellij.openapi.module.Module
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project

/**
 * @author vlan
 */
class MpyProjectConfigurable(project: Project) : ModuleAwareProjectConfigurable<Configurable>(project, "MicroPython", null), DumbAware {
    override fun createModuleConfigurable(module: Module?) = MpyModuleConfigurable(module!!)
}