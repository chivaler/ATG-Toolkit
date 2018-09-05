package org.idea.plugin.atg.module;

import com.intellij.facet.FacetConfiguration;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.XCollection;
import com.intellij.util.xmlb.annotations.XMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AtgModuleFacetConfiguration implements FacetConfiguration, PersistentStateComponent<AtgModuleFacetConfiguration.State> {

    private String atgModuleName = "";
    @NotNull
    private List<VirtualFile> configRoots = new ArrayList<>();
    @NotNull
    private Map<VirtualFile, String> configLayerRoots = new HashMap<>();
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
        configLayerRoots
                .forEach((key, value) -> result.configLayerRoots.put(key.getUrl(), value));
        webRoots
                .forEach((key, value) -> result.webRoots.put(key.getUrl(), value));
        result.atgModuleName = atgModuleName;
        return result;
    }

    @NotNull
    public Collection<VirtualFile> getConfigRoots() {
        return configRoots;
    }

    @NotNull
    public Map<VirtualFile, String> getConfigLayerRoots() {
        return configLayerRoots;
    }


    @NotNull
    public Map<VirtualFile, String> getWebRoots() {
        return webRoots;
    }

    public String getAtgModuleName() {
        return atgModuleName;
    }

    public void setAtgModuleName(String atgModuleName) {
        this.atgModuleName = atgModuleName;
    }

    @Override
    public void loadState(@NotNull State state) {
        configRoots = new ArrayList<>();
        configLayerRoots = new HashMap<>();
        webRoots = new HashMap<>();
        atgModuleName = state.atgModuleName;
        state.configRoots
                .forEach(s -> configRoots.add(VirtualFileManager.getInstance().findFileByUrl(s)));
        state.configLayerRoots
                .forEach((key, value) -> configLayerRoots.put(VirtualFileManager.getInstance().findFileByUrl(key), value));
        state.webRoots
                .forEach((key, value) -> webRoots.put(VirtualFileManager.getInstance().findFileByUrl(key), value));
    }

    @SuppressWarnings("WeakerAccess")
    static class State {
        @Tag(value = "atgModuleName")
        public String atgModuleName = "";
        @XCollection(elementName = "root", valueAttributeName = "url", propertyElementName = "configRoots")
        public List<String> configRoots = new ArrayList<>();
        @XMap(entryTagName = "root", keyAttributeName = "url", valueAttributeName = "layerName", propertyElementName = "configLayerRoots")
        public Map<String, String> configLayerRoots = new HashMap<>();
        @XMap(entryTagName = "root", keyAttributeName = "url", valueAttributeName = "context", propertyElementName = "webRoots")
        public Map<String, String> webRoots = new HashMap<>();
    }

}
