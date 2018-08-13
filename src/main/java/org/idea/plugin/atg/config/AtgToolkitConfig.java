package org.idea.plugin.atg.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "AtgToolkitConfig", storages = {@Storage("atg-toolkit.xml")})
public class AtgToolkitConfig implements PersistentStateComponent<AtgToolkitConfig> {

    private static final String DEFAULT_RELATIVE_CONFIG_PATH = "src/main/config,src/config";
    private static final String DEFAULT_RELATIVE_CONFIG_LAYERS_PATH = "layer/config";
    private static final String DEFAULT_IGNORED_PARENTS = "atg.nucleus.*";

    @SuppressWarnings("WeakerAccess")
    public String ignoredClassesForSetters = DEFAULT_IGNORED_PARENTS;
    @SuppressWarnings("WeakerAccess")
    public String configRootsPatterns = DEFAULT_RELATIVE_CONFIG_PATH;
    @SuppressWarnings("WeakerAccess")
    public String configLayerRootsPatterns = DEFAULT_RELATIVE_CONFIG_LAYERS_PATH;

    @Nullable
    @Override
    public AtgToolkitConfig getState() {
        AtgToolkitConfig state = new AtgToolkitConfig();
        XmlSerializerUtil.copyBean(this, state);
        return state;
    }

    @Override
    public void loadState(@NotNull AtgToolkitConfig state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    @NotNull
    public static AtgToolkitConfig getInstance() {
        AtgToolkitConfig service = ServiceManager.getService(AtgToolkitConfig.class);
        return service != null ? service : new AtgToolkitConfig();
    }

    public String getIgnoredClassesForSetters() {
        return ignoredClassesForSetters;
    }

    public void setIgnoredClassesForSetters(String ignoredClassesForSetters) {
        this.ignoredClassesForSetters = ignoredClassesForSetters;
    }

    public String getConfigRootsPatterns() {
        return configRootsPatterns;
    }

    public void setConfigRootsPatterns(String configRootsPatterns) {
        this.configRootsPatterns = configRootsPatterns;
    }

    public String getConfigLayerRootsPatterns() {
        return configLayerRootsPatterns;
    }

    public void setConfigLayerRootsPatterns(String configLayerRootsPatterns) {
        this.configLayerRootsPatterns = configLayerRootsPatterns;
    }
}
