package org.idea.plugin.atg.config;

import com.intellij.openapi.options.ConfigurableUi;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class AtgToolkitSettingsUi implements ConfigurableUi<AtgToolkitConfig> {

    private JPanel rootPanel;
    private JTextField configPatternsField;
    private JTextField configLayersPatternsField;
    private JCheckBox removeRootsNonMatchedToPatterns;
    private JCheckBox removeFacetsIfModuleHasNoAtgRoots;

    private JTextField ignoredClassesField;

    private JCheckBox attachClassPathOfAtgDependencies;
    private JCheckBox attachConfigsOfAtgDependencies;

    private JCheckBox showReferencesOnComponentInGoTo;
    private JCheckBox showOverridesOfComponentInGoTo;

    private JLabel errorLabel;
    private JButton detectAllConfigurationRootsButton;
    private JButton detectAndAttachDependenciesButton;

    public AtgToolkitSettingsUi(Project project) {
        detectAllConfigurationRootsButton.addActionListener(e -> new DetectAtgRootsAction()
                .runDetection(project, configPatternsField.getText(), configLayersPatternsField.getText(), removeRootsNonMatchedToPatterns.isSelected(), removeFacetsIfModuleHasNoAtgRoots.isSelected()));
        detectAndAttachDependenciesButton.addActionListener(e -> new AttachAtgDependenciesAction()
                .runProjectUpdate(project, attachClassPathOfAtgDependencies.isSelected(), attachConfigsOfAtgDependencies.isSelected()));
    }

    @Override
    public void reset(@NotNull AtgToolkitConfig atgToolkitConfig) {
        configPatternsField.setText(atgToolkitConfig.getConfigRootsPatterns());
        configLayersPatternsField.setText(atgToolkitConfig.getConfigLayerRootsPatterns());
        removeRootsNonMatchedToPatterns.setSelected(atgToolkitConfig.isRemoveRootsNonMatchedToPatterns());
        removeFacetsIfModuleHasNoAtgRoots.setSelected(atgToolkitConfig.isRemoveFacetsIfModuleHasNoAtgRoots());
        ignoredClassesField.setText(atgToolkitConfig.getIgnoredClassesForSetters());
        attachClassPathOfAtgDependencies.setSelected(atgToolkitConfig.isAttachClassPathOfAtgDependencies());
        attachConfigsOfAtgDependencies.setSelected(atgToolkitConfig.isAttachConfigsOfAtgDependencies());
        showReferencesOnComponentInGoTo.setSelected(atgToolkitConfig.isShowReferencesOnComponentInGoTo());
        showOverridesOfComponentInGoTo.setSelected(atgToolkitConfig.isShowOverridesOfComponentInGoTo());
    }

    @Override
    public boolean isModified(@NotNull AtgToolkitConfig atgToolkitConfig) {
        boolean isModified = false;

        if (!configPatternsField.getText().equals(atgToolkitConfig.getConfigRootsPatterns()))
            isModified = true;
        if (!configLayersPatternsField.getText().equals(atgToolkitConfig.getConfigLayerRootsPatterns()))
            isModified = true;
        if (removeFacetsIfModuleHasNoAtgRoots.isSelected() != atgToolkitConfig.isRemoveFacetsIfModuleHasNoAtgRoots())
            isModified = true;
        if (removeRootsNonMatchedToPatterns.isSelected() != atgToolkitConfig.isRemoveRootsNonMatchedToPatterns())
            isModified = true;
        if (!ignoredClassesField.getText().equals(atgToolkitConfig.getIgnoredClassesForSetters()))
            isModified = true;
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
        atgToolkitConfig.setRemoveRootsNonMatchedToPatterns(removeRootsNonMatchedToPatterns.isSelected());
        atgToolkitConfig.setRemoveFacetsIfModuleHasNoAtgRoots(removeFacetsIfModuleHasNoAtgRoots.isSelected());

        atgToolkitConfig.setIgnoredClassesForSetters(ignoredClassesField.getText());

        atgToolkitConfig.setAttachClassPathOfAtgDependencies(attachClassPathOfAtgDependencies.isSelected());
        atgToolkitConfig.setAttachConfigsOfAtgDependencies(attachConfigsOfAtgDependencies.isSelected());
        atgToolkitConfig.setShowOverridesOfComponentInGoTo(showOverridesOfComponentInGoTo.isSelected());
        atgToolkitConfig.setShowReferencesOnComponentInGoTo(showReferencesOnComponentInGoTo.isSelected());
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return rootPanel;
    }
}