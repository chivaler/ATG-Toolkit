package org.idea.plugin.atg.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurableUi;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbModeTask;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.util.AtgEnvironmentUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class AtgToolkitSettingsUi implements ConfigurableUi<AtgToolkitConfig> {
    private static final String DEPENDENCIES_TASK = "DEPENDENCIES_TASK";
    private Project project;

    private JPanel rootPanel;
    private JTextField configPatternsField;
    private JTextField configLayersPatternsField;
    private JTextField ignoredClassesField;

    private JCheckBox attachClassPathOfAtgDependencies;
    private JCheckBox attachConfigsOfAtgDependencies;

    private JCheckBox showReferencesOnComponentInGoTo;
    private JCheckBox showOverridesOfComponentInGoTo;

    private JLabel errorLabel;

    public AtgToolkitSettingsUi(Project project) {
        this.project = project;
    }

    @Override
    public void reset(@NotNull AtgToolkitConfig atgToolkitConfig) {
        configPatternsField.setText(atgToolkitConfig.getConfigRootsPatterns());
        configLayersPatternsField.setText(atgToolkitConfig.getConfigLayerRootsPatterns());
        ignoredClassesField.setText(atgToolkitConfig.getIgnoredClassesForSetters());
        attachClassPathOfAtgDependencies.setSelected(atgToolkitConfig.isAttachClassPathOfAtgDependencies());
        attachConfigsOfAtgDependencies.setSelected(atgToolkitConfig.isAttachConfigsOfAtgDependencies());
        showReferencesOnComponentInGoTo.setSelected(atgToolkitConfig.isShowReferencesOnComponentInGoTo());
        showOverridesOfComponentInGoTo.setSelected(atgToolkitConfig.isShowOverridesOfComponentInGoTo());
    }

    @Override
    public boolean isModified(@NotNull AtgToolkitConfig atgToolkitConfig) {
        boolean isModified = false;

        if (!configPatternsField.getText().equals(atgToolkitConfig.getConfigRootsPatterns())) isModified = true;
        if (!configLayersPatternsField.getText().equals(atgToolkitConfig.getConfigLayerRootsPatterns()))
            isModified = true;
        if (!ignoredClassesField.getText().equals(atgToolkitConfig.getIgnoredClassesForSetters())) isModified = true;
        if (attachClassPathOfAtgDependencies.isSelected() != atgToolkitConfig.isAttachClassPathOfAtgDependencies())
            isModified = true;
        if (attachConfigsOfAtgDependencies.isSelected() != atgToolkitConfig.isAttachConfigsOfAtgDependencies())
            isModified = true;
        if (showReferencesOnComponentInGoTo.isSelected() != atgToolkitConfig.isShowReferencesOnComponentInGoTo())
            isModified = true;
        if (showOverridesOfComponentInGoTo.isSelected() != atgToolkitConfig.isShowOverridesOfComponentInGoTo())
            isModified = true;

        return isModified;
    }

    @Override
    public void apply(@NotNull AtgToolkitConfig atgToolkitConfig) {
        String configRootPatters = configPatternsField.getText().replaceAll("\\s", "");
        configPatternsField.setText(configRootPatters);
        String configLayerRootPatters = configLayersPatternsField.getText().replaceAll("\\s", "");
        configLayersPatternsField.setText(configLayerRootPatters);

        atgToolkitConfig.setConfigRootsPatterns(configRootPatters);
        atgToolkitConfig.setConfigLayerRootsPatterns(configLayerRootPatters);
        atgToolkitConfig.setIgnoredClassesForSetters(ignoredClassesField.getText());

        boolean dependenciesResolvingChanged = false;
        if (attachClassPathOfAtgDependencies.isSelected() != atgToolkitConfig.isAttachClassPathOfAtgDependencies()) {
            dependenciesResolvingChanged = true;
        }
        if (attachConfigsOfAtgDependencies.isSelected() != atgToolkitConfig.isAttachConfigsOfAtgDependencies()) {
            dependenciesResolvingChanged = true;
        }
        atgToolkitConfig.setAttachClassPathOfAtgDependencies(attachClassPathOfAtgDependencies.isSelected());
        atgToolkitConfig.setAttachConfigsOfAtgDependencies(attachConfigsOfAtgDependencies.isSelected());
        atgToolkitConfig.setShowOverridesOfComponentInGoTo(showOverridesOfComponentInGoTo.isSelected());
        atgToolkitConfig.setShowReferencesOnComponentInGoTo(showReferencesOnComponentInGoTo.isSelected());

        if (dependenciesResolvingChanged) {
            DumbService.getInstance(project).queueTask(new DumbModeTask(DEPENDENCIES_TASK) {
                @Override
                public void performInDumbMode(@NotNull ProgressIndicator indicator) {
                    ApplicationManager.getApplication().runReadAction(() -> {
                        AtgEnvironmentUtil.removeAtgDependenciesForAllModules(project, indicator);
                        if (AtgToolkitConfig.getInstance(project).isAttachConfigsOfAtgDependencies() || AtgToolkitConfig.getInstance(project).isAttachClassPathOfAtgDependencies()) {
                            AtgEnvironmentUtil.addAtgDependenciesForAllModules(project, indicator);
                        }
                    });
                    indicator.setText(AtgToolkitBundle.message("update.dependencies.indexing.text"));
                    indicator.setText2(null);
                }
            });
        }
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return rootPanel;
    }

}