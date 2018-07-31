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
