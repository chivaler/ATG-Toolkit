package org.idea.plugin.atg.config;

import com.intellij.openapi.options.ConfigurableUi;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class AtgToolkitSettingsUi implements ConfigurableUi<AtgToolkitConfig> {

    private JPanel rootPanel;
    private JTextField configPatternsField;
    private JTextField configLayersPatternsField;
    private JTextField ignoredClassesField;
    private JCheckBox attachClassPathOfAtgDependencies;
    private JCheckBox attachConfigsOfAtgDependencies;
    private JButton detectAllFacets;
    private JLabel errorLabel;

    @Override
    public void reset(@NotNull AtgToolkitConfig atgToolkitConfig) {
        configPatternsField.setText(atgToolkitConfig.getConfigRootsPatterns());
        configLayersPatternsField.setText(atgToolkitConfig.getConfigLayerRootsPatterns());
        ignoredClassesField.setText(atgToolkitConfig.getIgnoredClassesForSetters());
        attachClassPathOfAtgDependencies.setSelected(atgToolkitConfig.isAttachClassPathOfAtgDependencies());
        attachConfigsOfAtgDependencies.setSelected(atgToolkitConfig.isAttachConfigsOfAtgDependencies());
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
        atgToolkitConfig.setAttachClassPathOfAtgDependencies(attachClassPathOfAtgDependencies.isSelected());
        atgToolkitConfig.setAttachConfigsOfAtgDependencies(attachConfigsOfAtgDependencies.isSelected());
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return rootPanel;
    }

}
