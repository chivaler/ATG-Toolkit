package org.idea.plugin.atg.roots;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.roots.ui.configuration.ContentRootPanel;
import com.intellij.openapi.roots.ui.configuration.ModuleSourceRootEditHandler;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.roots.IconActionComponent;
import com.intellij.util.ui.FormBuilder;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class AtgConfigurationSourceRootHandler extends ModuleSourceRootEditHandler<AtgConfigurationRootProperties> {
    public AtgConfigurationSourceRootHandler() {
        super(AtgConfigurationRootType.ATG_CONFIGURATION);
    }

    @NotNull
    @Override
    public String getRootTypeName() {
        return AtgToolkitBundle.message("root.type.name");
    }

    @NotNull
    @Override
    public Icon getRootIcon() {
        return AllIcons.Providers.Oracle;
    }

    @NotNull
    @Override
    public String getRootsGroupTitle() {
        return AtgToolkitBundle.message("root.group.title");
    }

    @NotNull
    @Override
        public Color getRootsGroupColor() {
        return new JBColor(new Color(0xFF9B17), new Color(255, 155, 23));
    }

    @NotNull
    @Override
    public String getUnmarkRootButtonText() {
        return AtgToolkitBundle.message("root.unMarkButton.name");
    }

    @NotNull
    @Override
    public String getMarkRootButtonText() {
        return AtgToolkitBundle.message("root.markButton.name");
    }

    @NotNull
    @Override
    public String getFullRootTypeName() {
        return AtgToolkitBundle.message("root.type.fullName");
    }

    @Nullable
    @Override
    public Icon getFolderUnderRootIcon() {
        return null;
    }

    @Nullable
    @Override
    public CustomShortcutSet getMarkRootShortcutSet() {
        return null;
    }

    @NotNull
    @Override
    public Icon getRootIcon(@NotNull AtgConfigurationRootProperties properties) {
        return getRootIcon();
    }

    @Nullable
    @Override
    public String getPropertiesString(@NotNull AtgConfigurationRootProperties properties) {
        StringBuilder buffer = new StringBuilder();
        String relativeOutputPath = properties.getRelativeOutputPath();
        if (!relativeOutputPath.isEmpty()) {
            buffer.append(" (").append(relativeOutputPath).append(")");
        }
        return buffer.length() > 0 ? buffer.toString() : null;
    }

    @Nullable
    @Override
    public JComponent createPropertiesEditor(@NotNull final SourceFolder folder,
                                             @NotNull final JComponent parentComponent,
                                             @NotNull final ContentRootPanel.ActionCallback callback) {
        final IconActionComponent iconComponent = new IconActionComponent(AllIcons.Modules.SetPackagePrefix,
                AllIcons.Modules.SetPackagePrefixRollover,
                ProjectBundle.message("module.paths.edit.properties.tooltip"),
                () -> {
                    AtgConfigurationRootProperties properties = folder.getJpsElement().getProperties(AtgConfigurationRootType.ATG_CONFIGURATION);
                    assert properties != null;
                    AtgRootPropertiesDialog
                            dialog = new AtgRootPropertiesDialog(parentComponent, properties);
                    if (dialog.showAndGet()) {
                        callback.onSourceRootPropertiesChanged(folder);
                    }
                });
        final JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(iconComponent, BorderLayout.CENTER);
        panel.add(Box.createHorizontalStrut(3), BorderLayout.EAST);
        return panel;
    }

    private static class AtgRootPropertiesDialog extends DialogWrapper {
        private final JTextField myRelativeOutputPathField;
        private final JPanel myMainPanel;
        @NotNull
        private final AtgConfigurationRootProperties myProperties;

        private AtgRootPropertiesDialog(@NotNull JComponent parentComponent, @NotNull AtgConfigurationRootProperties properties) {
            super(parentComponent, true);
            myProperties = properties;
            setTitle(AtgToolkitBundle.message("module.paths.edit.properties.title"));
            myRelativeOutputPathField = new JTextField();
            myMainPanel = FormBuilder.createFormBuilder()
                    .addLabeledComponent("Relative output &path:", myRelativeOutputPathField)
                    .getPanel();
            myRelativeOutputPathField.setText(myProperties.getRelativeOutputPath());
            myRelativeOutputPathField.setColumns(25);
            init();
        }

        @NotNull
        private static String normalizePath(String path) {
            return StringUtil.trimEnd(StringUtil.trimStart(FileUtil.toSystemIndependentName(path.trim()), "/"), "/");
        }

        @Nullable
        @Override
        public JComponent getPreferredFocusedComponent() {
            return myRelativeOutputPathField;
        }

        @Override
        protected void doOKAction() {
            myProperties.setRelativeOutputPath(normalizePath(myRelativeOutputPathField.getText()));
//      myProperties.setForGeneratedSources(myIsGeneratedCheckBox.isSelected());
            super.doOKAction();
        }

        @Nullable
        @Override
        protected JComponent createCenterPanel() {
            return myMainPanel;
        }
    }
}
