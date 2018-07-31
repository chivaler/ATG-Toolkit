/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package org.idea.plugin.atg.roots;

import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleConfigurationEditor;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.DefaultModuleEditorsProvider;
import com.intellij.openapi.roots.ui.configuration.JavaContentEntriesEditor;
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationEditorProviderEx;
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationState;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AtgModuleEditorsProvider extends DefaultModuleEditorsProvider implements ModuleConfigurationEditorProviderEx {
    @Override
    public ModuleConfigurationEditor[] createEditors(ModuleConfigurationState state) {
        ModifiableRootModel rootModel = state.getRootModel();
        Module module = rootModel.getModule();
        if (!(ModuleType.get(module) instanceof JavaModuleType)) {
            return ModuleConfigurationEditor.EMPTY;
        }
        String moduleName = module.getName();
        ModuleConfigurationEditor[] editors = super.createEditors(state);
        List<ModuleConfigurationEditor> javaContentEntriesEditor = Arrays.stream(editors).filter(e -> !(e instanceof JavaContentEntriesEditor)).collect(Collectors.toList());
        javaContentEntriesEditor.add(new AtgContentEntriesEditor(moduleName, state));
        return editors;
    }


    @Override
    public boolean isCompleteEditorSet() {
        return true;
    }
}