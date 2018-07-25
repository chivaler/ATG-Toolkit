package org.idea.plugin.atg.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@State(name = "AtgToolkitConfig", storages = {@Storage("atg-toolkit.xml")})
public class AtgToolkitConfig implements PersistentStateComponent<AtgToolkitConfig> {
    public static final String ATG_TOOLKIT_ID = "atg-toolkit";
    private static final String DEFAULT_RELATIVE_CONFIG_PATH = "/src/main/config/";

    @SuppressWarnings("WeakerAccess")
    public String ignoredClassesForSetters = "";
    @SuppressWarnings("WeakerAccess")
    public String relativeConfigPath = DEFAULT_RELATIVE_CONFIG_PATH;
    @SuppressWarnings("WeakerAccess")
    public boolean injectUnambiguousProperties = true;

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

    @Nullable
    public static AtgToolkitConfig getInstance(Project project) {
        return ServiceManager.getService(project, AtgToolkitConfig.class);
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

    public boolean isInjectUnambiguousProperties() {
        return injectUnambiguousProperties;
    }

    public void setInjectUnambiguousProperties(boolean injectUnambiguousProperties) {
        this.injectUnambiguousProperties = injectUnambiguousProperties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AtgToolkitConfig that = (AtgToolkitConfig) o;
        return injectUnambiguousProperties == that.injectUnambiguousProperties &&
                Objects.equals(ignoredClassesForSetters, that.ignoredClassesForSetters) &&
                Objects.equals(relativeConfigPath, that.relativeConfigPath);
    }

    @Override
    public int hashCode() {

        return Objects.hash(ignoredClassesForSetters, relativeConfigPath, injectUnambiguousProperties);
    }
}
