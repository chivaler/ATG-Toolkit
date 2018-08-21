package org.idea.plugin.atg.module;

import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetValidatorsManager;
import org.idea.plugin.atg.Constants;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Igor_Pidgurskyi on 4/19/2017.
 */
public class AtgModuleFacetEditor extends FacetEditorTab {
    private static final String DISPLAY_NAME = "ATG Module Parameters";
    private boolean modified = false;
    private AtgModuleFacet facet;
    private FacetEditorContext context;
    private FacetValidatorsManager validatorsManager;

    protected AtgModuleFacetEditor(AtgModuleFacet facet, FacetEditorContext context, FacetValidatorsManager validatorsManager) {
        this.context = context;
        this.facet = facet;
        this.validatorsManager = validatorsManager;
    }

    @NotNull
    @Override
    public JComponent createComponent() {
        JPanel contentPanel = new JPanel(new BorderLayout());
        JPanel myPanel = new JPanel(new GridBagLayout());
        contentPanel.add("Center", myPanel);
        JCheckBox myCheckBok = new JCheckBox("Extra Option");
        myPanel.add(myCheckBok);
        return contentPanel;
    }

    @Override
    public boolean isModified() {
        return modified;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public String getHelpTopic() {
        return Constants.HelpTopics.MODULE_FACET_EDITOR;
    }

//    private static class ConfigRootsComponent {
//
//        public JComponent createComponent() {
//            ColumnInfo[] columnInfos = new ColumnInfo[]{ColumnInfo, "LAYER"};
//            ListTableModel<DescriptorInfo> listModel = new ListTableModel(columnInfos);
//            this.myDeploymentDescriptorsEditorPanel.add(this.myConfigFilesEditor.createComponent(), "Center");
//            this.myInfoLabel.setVisible(this.myInsideAddModuleWizard);
//            this.myAddAppServerDescriptorButton.addActionListener(new EditingDeploymentDescriptorSetComponent.AddAppServerDescriptorListener(this.myConfigFilesEditor.getTableModel()));
//            this.myAddAppServerDescriptorButton.setEnabled(CreateAppServerDescriptorDialog.hasDescriptorsToCreate(this.myMetaDataProvider, this));
//            return this.myPanel;

//    protected final F myFacet;
//    private J2EEGeneralModuleViewlets myViewlets;
//    private JComponent myComponent;
//    private final FacetEditorContext myContext;
//    private final FacetValidatorsManager myValidatorsManager;
//
//    protected J2EEModuleElementsEditor(Facet facet, FacetEditorContext context, FacetValidatorsManager validatorsManager) {
//        this.myContext = context;
//        this.myFacet = facet;
//        this.myValidatorsManager = validatorsManager;
//    }
//
//    public void disposeUIResources() {
//        if(this.myViewlets != null) {
//            this.myViewlets.dispose();
//        }
//
//    }
//
//    public String getDisplayName() {
//        return getEditorName(this.myFacet.getType());
//    }
//
//    public static String getEditorName(FacetType type) {
//        return J2EEBundle.message("javaee.facet.settings.display.name", new Object[]{type.getPresentableName()});
//    }
//
//    public void apply() {
//        (new WriteAction() {
//            protected void run(@NotNull Result result) {
//                J2EEModuleElementsEditor.this.saveData();
//            }
//        }).execute();
//    }
//
//    public void saveData() {
//        if(this.myViewlets != null) {
//            this.myViewlets.saveData();
//        }
//
//    }
//
//    public boolean isModified() {
//        return this.myViewlets.isModified() || this.myViewlets.isEditing();
//    }
//
//    public JComponent createComponentImpl() {
//        this.myViewlets = this.createViewlets(this.myContext);
//        return this.myViewlets.createComponent();
//    }
//
//
//    @NotNull
//    public JComponent createComponent() {
//        if(this.myComponent == null) {
//            this.myComponent = this.createComponentImpl();
//        }
//
//        JComponent var10000 = this.myComponent;
//        if(this.myComponent == null) {
//            throw new IllegalStateException(String.format("@NotNull method %s.%s must not return null", new Object[]{"com/intellij/javaee/module/view/common/J2EEModuleElementsEditor", "createComponent"}));
//        } else {
//            return var10000;
//        }
//    }
//
//    public void reset() {
//    }
//
//    public void onFacetInitialized(@NotNull Facet facet) {
//    }
//

//
//    public void onTabEntering() {
//        this.myValidatorsManager.validate();
//    }
//
//
//    protected J2EEGeneralModuleViewlets createViewlets(FacetEditorContext context) {
//        return new ConfigureWebGeneralModuleViewlets(this.myFacet, context);
//    }
//
//

}
