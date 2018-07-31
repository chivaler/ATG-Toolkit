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

import com.intellij.openapi.util.text.StringUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;
import org.jetbrains.jps.model.serialization.JpsModelSerializerExtension;
import org.jetbrains.jps.model.serialization.module.JpsModuleSourceRootPropertiesSerializer;

import java.util.Arrays;
import java.util.List;

/**
 * @author nik
 */
public class AtgJpsModelSerializerExtension extends JpsModelSerializerExtension {

    @NotNull
    public List<? extends JpsModuleSourceRootPropertiesSerializer<?>> getModuleSourceRootPropertiesSerializers() {
        return Arrays.asList(new AtgResourceRootPropertiesSerializer(AtgConfigurationRootType.ATG_CONFIGURATION, "atg-config"));
    }

    private static class AtgResourceRootPropertiesSerializer extends JpsModuleSourceRootPropertiesSerializer<AtgConfigurationRootProperties> {
        private AtgResourceRootPropertiesSerializer(JpsModuleSourceRootType<AtgConfigurationRootProperties> type, String typeId) {
            super(type, typeId);
        }

        @Override
        public AtgConfigurationRootProperties loadProperties(@NotNull Element sourceRootTag) {
            String relativeOutputPath = StringUtil.notNullize(sourceRootTag.getAttributeValue("relativeOutputPath"));
            return new AtgConfigurationRootProperties(relativeOutputPath);
        }

        @Override
        public void saveProperties(@NotNull AtgConfigurationRootProperties properties, @NotNull Element sourceRootTag) {
            String relativeOutputPath = properties.getRelativeOutputPath();
            if (!relativeOutputPath.isEmpty()) {
                sourceRootTag.setAttribute("relativeOutputPath", relativeOutputPath);
            }
        }
    }
}
