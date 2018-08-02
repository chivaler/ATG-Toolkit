package org.idea.plugin.atg.config;

import com.intellij.openapi.options.ConfigurableUi;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.ui.UIUtil;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class AtgToolkitSettingsUi implements ConfigurableUi<AtgToolkitConfig> {
    private final JPanel rootPanel;
    private final JPanel controlsPanel;
    private final JLabel configPatternsLabel;
    private final JLabel configLayersPatternsLabel;
    private final JLabel ignoredClassesLabel;
    private final JTextField configPatternsField;
    private final JTextField configLayersPatternsField;
    private final JTextField ignoredClassesField;

    public AtgToolkitSettingsUi() {
        rootPanel = new JPanel(new VerticalLayout(0));
        controlsPanel = new JPanel(new GridBagLayout());

        configPatternsLabel = new JLabel(AtgToolkitBundle.message("gui.config.configRoots.title"));
        configLayersPatternsLabel = new JLabel(AtgToolkitBundle.message("gui.config.configLayersRoots.title"));
        ignoredClassesLabel = new JLabel(AtgToolkitBundle.message("gui.config.ignoredClasses"));
        configPatternsField = new JTextField();
        configLayersPatternsField = new JTextField();
        ignoredClassesField = new JTextField();

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets.right = UIUtil.DEFAULT_HGAP;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.BASELINE_LEADING;

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.0;
        controlsPanel.add(configPatternsLabel, constraints);

        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        controlsPanel.add(configPatternsField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 0.0;
        controlsPanel.add(configLayersPatternsLabel, constraints);

        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.weightx = 1.0;
        controlsPanel.add(configLayersPatternsField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.weightx = 0.0;
        controlsPanel.add(ignoredClassesLabel, constraints);

        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.weightx = 1;
        controlsPanel.add(ignoredClassesField, constraints);

        rootPanel.add(controlsPanel, VerticalLayout.TOP);
    }

    @Override
    public void reset(@NotNull AtgToolkitConfig atgToolkitConfig) {
        configPatternsField.setText(atgToolkitConfig.getConfigRootsPatterns());
        configLayersPatternsField.setText(atgToolkitConfig.getConfigLayerRootsPatterns());
        ignoredClassesField.setText(atgToolkitConfig.getIgnoredClassesForSetters());
    }

    @Override
    public boolean isModified(@NotNull AtgToolkitConfig atgToolkitConfig) {
        return !(configPatternsField.getText().equals(atgToolkitConfig.getConfigRootsPatterns()) &&
                configLayersPatternsField.getText().equals(atgToolkitConfig.getConfigLayerRootsPatterns()) &&
                ignoredClassesField.getText().equals(atgToolkitConfig.getIgnoredClassesForSetters()));
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
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return rootPanel;
    }
}
