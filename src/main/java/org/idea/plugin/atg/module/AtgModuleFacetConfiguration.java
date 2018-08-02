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

import java.util.*;

//@State(name = "External" + FacetManagerImpl.COMPONENT_NAME, externalStorageOnly = true)
public class AtgModuleFacetConfiguration implements FacetConfiguration, PersistentStateComponent<AtgModuleFacetConfiguration.State> {

    @NotNull
    private Set<VirtualFile> configRoots = new HashSet<>();
    @NotNull
    private Set<VirtualFile> configLayerRoots = new HashSet<>();


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
        configRoots.stream()
                .filter(Objects::nonNull)
                .forEach(r -> result.configRoots.add(r.getUrl()));
        configLayerRoots.stream()
                .filter(Objects::nonNull)
                .forEach(r -> result.configLayerRoots.add(r.getUrl()));
        return result;
    }

    @NotNull
    public Set<VirtualFile> getConfigRoots() {
        return configRoots;
    }

    @NotNull
    public Set<VirtualFile> getConfigLayerRoots() {
        return configLayerRoots;
    }

    @Override
    public void loadState(@NotNull State state) {
        configRoots = new HashSet<>();
        state.configRoots
                .forEach(s -> configRoots.add(VirtualFileManager.getInstance().findFileByUrl(s)));
        state.configLayerRoots
                .forEach(s -> configLayerRoots.add(VirtualFileManager.getInstance().findFileByUrl(s)));
    }

    @SuppressWarnings("WeakerAccess")
    static class State {
        @XCollection(elementName = "root", valueAttributeName = "url", propertyElementName = "configRoots")
        public List<String> configRoots = new ArrayList<>();
        @XCollection(elementName = "root", valueAttributeName = "url", propertyElementName = "configLayerRoots")
        public List<String> configLayerRoots = new ArrayList<>();
    }

}
