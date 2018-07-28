package org.idea.plugin.atg.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "AtgToolkitConfig", storages = {@Storage("atg-toolkit.xml")})
public class AtgToolkitConfig implements PersistentStateComponent<AtgToolkitConfig> {
    public static final String ATG_TOOLKIT_ID = "atg-toolkit";
    private static final String DEFAULT_RELATIVE_CONFIG_PATH = "/src/main/config/";
    private static final String DEFAULT_IGNORED_PARENTS = "atg.nucleus.*";

    @SuppressWarnings("WeakerAccess")
    public String ignoredClassesForSetters = DEFAULT_IGNORED_PARENTS;
    @SuppressWarnings("WeakerAccess")
    public String relativeConfigPath = DEFAULT_RELATIVE_CONFIG_PATH;

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
    public static AtgToolkitConfig getInstance(Project project) {
        AtgToolkitConfig service = ServiceManager.getService(project, AtgToolkitConfig.class);
        return service != null ? service : new AtgToolkitConfig();
    }

    public String getIgnoredClassesForSetters() {
        return ignoredClassesForSetters;
    }

    public void setIgnoredClassesForSetters(String ignoredClassesForSetters) {
        this.ignoredClassesForSetters = ignoredClassesForSetters;
    }

    public String getRelativeConfigPath() {
        return relativeConfigPath;
    }

    public void setRelativeConfigPath(String relativeConfigPath) {
        this.relativeConfigPath = relativeConfigPath;
    }
}
