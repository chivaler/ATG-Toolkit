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

import com.intellij.openapi.roots.ui.configuration.*;
import com.intellij.util.containers.ContainerUtil;

import java.util.List;

/**
 * @author Eugene Zhuravlev
 */
public class AtgContentEntriesEditor extends ContentEntriesEditor {
    private LanguageLevelConfigurable myLanguageLevelConfigurable;

    public AtgContentEntriesEditor(String moduleName, final ModuleConfigurationState state) {
        super(moduleName, state);
        ContainerUtil.addIfNotNull(getEditHandlers(), ModuleSourceRootEditHandler.getEditHandler(AtgConfigurationRootType.ATG_CONFIGURATION));
        ContainerUtil.addIfNotNull(getEditHandlers(), ModuleSourceRootEditHandler.getEditHandler(AtgConfigurationRootType.ATG_CONFIGURATION_LAYER));
    }

    @Override
    protected ContentEntryEditor createContentEntryEditor(String contentEntryUrl) {
        List<ModuleSourceRootEditHandler<?>> editHandlers = getEditHandlers();
        if (editHandlers.stream().noneMatch(AtgConfigurationSourceRootHandler.class::isInstance)) editHandlers.add(new AtgConfigurationSourceRootHandler());
        return super.createContentEntryEditor(contentEntryUrl);
    }
}
