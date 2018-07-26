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
    private final JLabel configRelativePathLabel;
    private final JLabel ignoredClassesLabel;
    private final JTextField configRelativePathField;
    private final JTextField ignoredClassesField;

    public AtgToolkitSettingsUi() {
        rootPanel = new JPanel(new VerticalLayout(0));
        controlsPanel = new JPanel(new GridBagLayout());

        configRelativePathLabel = new JLabel(AtgToolkitBundle.message("gui.config.config.dir"));
        ignoredClassesLabel = new JLabel(AtgToolkitBundle.message("gui.config.config.ignoredClasses"));
        configRelativePathField = new JTextField();
        ignoredClassesField = new JTextField();

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

        rootPanel.add(controlsPanel, VerticalLayout.TOP);
    }

    @Override
    public void reset(@NotNull AtgToolkitConfig atgToolkitConfig) {
        configRelativePathField.setText(atgToolkitConfig.getRelativeConfigPath());
        ignoredClassesField.setText(atgToolkitConfig.getIgnoredClassesForSetters());
    }

    @Override
    public boolean isModified(@NotNull AtgToolkitConfig atgToolkitConfig) {
        return !(configRelativePathField.getText().equals(atgToolkitConfig.getRelativeConfigPath()) &&
                ignoredClassesField.getText().equals(atgToolkitConfig.getIgnoredClassesForSetters()));
    }

    @Override
    public void apply(@NotNull AtgToolkitConfig atgToolkitConfig) {
        String relativeConfigPath = configRelativePathField.getText().replaceAll("\\s", "");
        if (!relativeConfigPath.startsWith("/")) relativeConfigPath = "/" + relativeConfigPath;
        if (!relativeConfigPath.endsWith("/")) relativeConfigPath = relativeConfigPath + "/";

        configRelativePathField.setText(relativeConfigPath);

        atgToolkitConfig.setRelativeConfigPath(relativeConfigPath);
        atgToolkitConfig.setIgnoredClassesForSetters(ignoredClassesField.getText());
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return rootPanel;
    }
}
