package org.idea.plugin.atg.module;

import com.intellij.facet.FacetConfiguration;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.xmlb.annotations.XCollection;
import com.intellij.util.xmlb.annotations.XMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AtgModuleFacetConfiguration implements FacetConfiguration, PersistentStateComponent<AtgModuleFacetConfiguration.State> {

    @NotNull
    private Set<VirtualFile> configRoots = new HashSet<>();
    @NotNull
    private Set<VirtualFile> configLayerRoots = new HashSet<>();
    //TODO as Map with ContextRoot
    @NotNull
    private Map<VirtualFile, String> webRoots = new HashMap<>();


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
        webRoots.entrySet().stream()
                .filter(Objects::nonNull)
                .forEach(r -> result.webRoots.put(r.getKey().getUrl(), r.getValue()));
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

    @NotNull
    public Map<VirtualFile, String> getWebRoots() {
        return webRoots;
    }

    @Override
    public void loadState(@NotNull State state) {
        configRoots = new HashSet<>();
        configLayerRoots = new HashSet<>();
        webRoots = new HashMap<>();
        state.configRoots
                .forEach(s -> configRoots.add(VirtualFileManager.getInstance().findFileByUrl(s)));
        state.configLayerRoots
                .forEach(s -> configLayerRoots.add(VirtualFileManager.getInstance().findFileByUrl(s)));
        state.webRoots.entrySet()
                .forEach(s -> webRoots.put(VirtualFileManager.getInstance().findFileByUrl(s.getKey()), s.getValue()));
    }

    @SuppressWarnings("WeakerAccess")
    static class State {
        @XCollection(elementName = "root", valueAttributeName = "url", propertyElementName = "configRoots")
        public List<String> configRoots = new ArrayList<>();
        @XCollection(elementName = "root", valueAttributeName = "url", propertyElementName = "configLayerRoots")
        public List<String> configLayerRoots = new ArrayList<>();
//        @XCollection(elementName = "root", valueAttributeName = "url", propertyElementName = "webRoots")

        @XMap(entryTagName = "root", keyAttributeName = "url", valueAttributeName = "context", propertyElementName = "webRoots")
        public Map<String, String> webRoots = new HashMap<>();
    }

}
