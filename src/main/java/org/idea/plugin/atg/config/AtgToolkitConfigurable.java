package org.idea.plugin.atg.config;

import com.intellij.openapi.options.ConfigurableBase;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.jetbrains.annotations.NotNull;

public class AtgToolkitConfigurable extends ConfigurableBase<AtgToolkitSettingsUi, AtgToolkitConfig> {

    private final AtgToolkitConfig atgToolkitConfig;

    protected AtgToolkitConfigurable(@NotNull AtgToolkitConfig atgToolkitConfig) {
        super(AtgToolkitConfig.ATG_TOOLKIT_ID, AtgToolkitBundle.message("gui.config.title"), null);
        this.atgToolkitConfig = atgToolkitConfig;
    }

    @NotNull
    @Override
    protected AtgToolkitConfig getSettings() {
        return this.atgToolkitConfig;
    }

    @Override
    protected AtgToolkitSettingsUi createUi() {
        return new AtgToolkitSettingsUi();
    }
}
