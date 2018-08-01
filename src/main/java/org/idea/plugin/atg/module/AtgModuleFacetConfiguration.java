package org.idea.plugin.atg.module;

import com.intellij.facet.FacetConfiguration;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.xmlb.annotations.XCollection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//@State(name = "External" + FacetManagerImpl.COMPONENT_NAME, externalStorageOnly = true)
public class AtgModuleFacetConfiguration implements FacetConfiguration, PersistentStateComponent<AtgModuleFacetConfiguration.State> {

    @NotNull
    public Set<VirtualFile> configRoots = new HashSet<>();


    @Override
    public FacetEditorTab[] createEditorTabs(FacetEditorContext editorContext, FacetValidatorsManager validatorsManager) {
        AtgModuleFacet atgModuleFacet = (AtgModuleFacet) editorContext.getFacet();
        AtgModuleFacetEditor facetEditor = new AtgModuleFacetEditor(atgModuleFacet, editorContext, validatorsManager);
        return new FacetEditorTab[]{facetEditor};
    }

    @Nullable
    @Override
    public State getState() {
        State result = new State();
        for (VirtualFile configRoot : configRoots) {
            String canonicalPath = configRoot.getCanonicalPath();
            if (canonicalPath != null) {
                result.configRoots.add(configRoot.getUrl());
            }
        }
        return result;
    }

    @NotNull
    public Set<VirtualFile> getConfigRoots() {
        return configRoots;
    }

    @Override
    public void loadState(@NotNull State state) {
        configRoots = new HashSet<>();
        for (String configRootStr : state.configRoots) {
            configRoots.add(VirtualFileManager.getInstance().findFileByUrl(configRootStr));
        }
    }

    static class State {
        @XCollection(elementName = "root", valueAttributeName = "url", propertyElementName = "configRoots")
        public List<String> configRoots = new ArrayList<>();
    }

}
