package org.idea.plugin.atg.config;

import com.intellij.openapi.options.ConfigurableUi;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.ui.UIUtil;
import org.idea.plugin.atg.AtgModuleBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class AtgToolkitSettingsUi implements ConfigurableUi<AtgToolkitConfig> {
    private JPanel rootPanel;
    private JPanel controlsPanel;
    private JLabel configRelativePathLabel;
    private JLabel ignoredClassesLabel;
    private JTextField configRelativePathField;
    private JTextField ignoredClassesField;
    private JCheckBox injectUnambiguousPropertiesCheckBox;

    public AtgToolkitSettingsUi(AtgToolkitConfig atgToolkitConfig) {
        rootPanel = new JPanel(new VerticalLayout(0));
        controlsPanel = new JPanel(new GridBagLayout());

        configRelativePathLabel = new JLabel(AtgModuleBundle.message("gui.config.config.dir"));
        ignoredClassesLabel = new JLabel(AtgModuleBundle.message("gui.config.config.ignoredClasses"));
        configRelativePathField = new JTextField();
        ignoredClassesField = new JTextField();
        injectUnambiguousPropertiesCheckBox = new JCheckBox(AtgModuleBundle.message("gui.config.config.injectUnambiguousProperties"));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets.right = UIUtil.DEFAULT_HGAP;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.BASELINE_LEADING;

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.0;
        controlsPanel.add(configRelativePathLabel, constraints);

        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        controlsPanel.add(configRelativePathField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 0.0;
        controlsPanel.add(ignoredClassesLabel, constraints);

        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.weightx = 1;
        controlsPanel.add(ignoredClassesField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.weightx = 0.0;
        controlsPanel.add(injectUnambiguousPropertiesCheckBox, constraints);


        rootPanel.add(controlsPanel, VerticalLayout.TOP);
    }

    @Override
    public void reset(@NotNull AtgToolkitConfig atgToolkitConfig) {
        configRelativePathField.setText(atgToolkitConfig.getRelativeConfigPath());
        ignoredClassesField.setText(atgToolkitConfig.getIgnoredClassesForSetters());
        injectUnambiguousPropertiesCheckBox.setSelected(atgToolkitConfig.isInjectUnambiguousProperties());
    }

    @Override
    public boolean isModified(@NotNull AtgToolkitConfig atgToolkitConfig) {
        return !(configRelativePathField.getText().equals(atgToolkitConfig.getRelativeConfigPath()) &&
                ignoredClassesField.getText().equals(atgToolkitConfig.getIgnoredClassesForSetters()) &&
                injectUnambiguousPropertiesCheckBox.isSelected() == atgToolkitConfig.isInjectUnambiguousProperties());
    }

    @Override
    public void apply(@NotNull AtgToolkitConfig atgToolkitConfig) {
        atgToolkitConfig.setRelativeConfigPath(configRelativePathField.getText());
        atgToolkitConfig.setIgnoredClassesForSetters(ignoredClassesField.getText());
        atgToolkitConfig.setInjectUnambiguousProperties(injectUnambiguousPropertiesCheckBox.isSelected());
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return rootPanel;
    }
}
