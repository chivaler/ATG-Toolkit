package org.idea.plugin.atg.config;

import com.intellij.openapi.options.ConfigurableBase;
import com.intellij.openapi.project.Project;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.Constants;
import org.jetbrains.annotations.NotNull;

public class AtgToolkitConfigurable extends ConfigurableBase<AtgToolkitSettingsUi, AtgToolkitConfig> {

    private final AtgToolkitConfig atgToolkitConfig;
    private final Project project;

    protected AtgToolkitConfigurable(@NotNull AtgToolkitConfig atgToolkitConfig, @NotNull Project project) {
        super(Constants.ATG_TOOLKIT_CONFIGURABLE_ID, AtgToolkitBundle.message("gui.config.title"), Constants.HelpTopics.PLUGIN_CONFIGURABLE_EDITOR);
        this.atgToolkitConfig = atgToolkitConfig;
        this.project = project;
    }

    @NotNull
    @Override
    protected AtgToolkitConfig getSettings() {
        return this.atgToolkitConfig;
    }

    @Override
    protected AtgToolkitSettingsUi createUi() {
        return new AtgToolkitSettingsUi(project);
    }
}
