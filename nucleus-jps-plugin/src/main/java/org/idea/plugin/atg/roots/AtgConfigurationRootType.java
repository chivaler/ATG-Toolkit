/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.ex.JpsElementTypeBase;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

public class AtgConfigurationRootType extends JpsElementTypeBase<AtgConfigurationRootProperties> implements
        JpsModuleSourceRootType<AtgConfigurationRootProperties> {
    public static final AtgConfigurationRootType ATG_CONFIGURATION = new AtgConfigurationRootType();
    public static final AtgConfigurationRootType ATG_CONFIGURATION_LAYER = new AtgConfigurationRootType();

    private AtgConfigurationRootType() {
    }

    @NotNull
    @Override
    public AtgConfigurationRootProperties createDefaultProperties() {
        return new AtgConfigurationRootProperties("");
//        return JpsJavaExtensionService.getInstance().createResourceRootProperties("", false);
    }
}
