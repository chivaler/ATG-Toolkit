package org.idea.plugin.atg.module;

import com.google.common.collect.Lists;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.AddEditDeleteListPanel;
import com.intellij.uiDesigner.core.GridConstraints;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.Constants;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class AtgModuleFacetEditor extends FacetEditorTab {
    private boolean modified = false;
    private AtgModuleFacet facet;
    private FacetEditorContext context;
    private FacetValidatorsManager validatorsManager;

    private FileChooserDescriptor myDescriptor;
    private JPanel rootPanel;
    private JPanel configRootsPanel;
    private JPanel configRootsHolderPanel;
    private JPanel configLayerRootsPanel;
    private JPanel configLayerHolderRootsPanel;
    private JPanel webRootsHolderPanel;
    private JPanel webRootsPanel;
    private JTextField atgModuleName;

    protected AtgModuleFacetEditor(AtgModuleFacet facet, FacetEditorContext context, FacetValidatorsManager validatorsManager) {
        this.context = context;
        this.facet = facet;
        this.validatorsManager = validatorsManager;
    }

    @NotNull
    @Override
    public JComponent createComponent() {
        AtgModuleFacetConfiguration atgFacetConfiguration = facet.getConfiguration();

        GridConstraints constraints = new GridConstraints();
        constraints.setFill(GridConstraints.FILL_HORIZONTAL);
        constraints.setAnchor(GridConstraints.ANCHOR_SOUTH);
        constraints.setIndent(1);

        configRootsPanel = new MyPanel<>(AtgToolkitBundle.message("gui.facet.configRoots.title"), Lists.newArrayList(atgFacetConfiguration.getConfigRoots()));
        configRootsPanel.setEnabled(false);
        configLayerRootsPanel = new MyPanel<>(AtgToolkitBundle.message("gui.facet.configLayersRoots.title"), Lists.newArrayList(atgFacetConfiguration.getConfigLayerRoots()));
        configLayerRootsPanel.setEnabled(false);
        webRootsPanel = new MyPanel<>(AtgToolkitBundle.message("gui.facet.webRoots.title"), Lists.newArrayList(atgFacetConfiguration.getWebRoots().keySet()));
        webRootsPanel.setEnabled(false);

        this.configRootsHolderPanel.add(configRootsPanel, constraints);
        this.configLayerHolderRootsPanel.add(configLayerRootsPanel, constraints);
        this.webRootsHolderPanel.add(webRootsPanel, constraints);

        return rootPanel;
    }

    @Override
    public boolean isModified() {
        return isModified(atgModuleName, facet.getConfiguration().getAtgModuleName());
    }

    @Nls
    @Override
    public String getDisplayName() {
        return AtgToolkitBundle.message("gui.facet.title");
    }

    @Override
    public String getHelpTopic() {
        return Constants.HelpTopics.MODULE_FACET_EDITOR;
    }

    private class MyPanel<T> extends AddEditDeleteListPanel<T> {

        public MyPanel(String title, List<T> initialList) {
            super(title, initialList);
        }

        @Nullable
        @Override
        protected T editSelectedItem(T item) {
            return null;
        }

        @Nullable
        @Override
        protected T findItemToAdd() {
            return null;
        }
    }

    @Override
    public void apply() {
        facet.getConfiguration().setAtgModuleName(atgModuleName.getText().trim());
    }

    @Override
    public void reset() {
        atgModuleName.setText(facet.getConfiguration().getAtgModuleName());
    }

    @Override
    public boolean isModified(@NotNull JTextField textField, @NotNull String value) {
        return !StringUtil.equals(textField.getText().trim(), value);
    }


}
