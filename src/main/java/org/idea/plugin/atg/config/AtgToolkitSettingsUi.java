package org.idea.plugin.atg.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurableUi;
import com.intellij.openapi.project.DumbAwareRunnable;
import com.intellij.openapi.project.Project;
import org.idea.plugin.atg.util.AtgEnvironmentUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class AtgToolkitSettingsUi implements ConfigurableUi<AtgToolkitConfig> {

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
        configPatternsField.setText(configLayerRootPatters);

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
            ApplicationManager.getApplication().invokeLater((DumbAwareRunnable) () -> {
                AtgEnvironmentUtil.removeAtgDependenciesForAllModules(project);
                if (AtgToolkitConfig.getInstance(project).isAttachConfigsOfAtgDependencies() || AtgToolkitConfig.getInstance(project).isAttachClassPathOfAtgDependencies()) {
                    AtgEnvironmentUtil.addAtgDependenciesForAllModules(project);
                }
            }, project.getDisposed());
        }
    }

        @NotNull
        @Override
        public JComponent getComponent () {
            return rootPanel;
        }

    }
