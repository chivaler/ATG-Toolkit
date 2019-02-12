package org.idea.plugin.atg.module;

import com.intellij.facet.FacetConfiguration;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.xmlb.annotations.OptionTag;
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
    private List<VirtualFile> webRoots = new ArrayList<>();


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
        configLayerRoots.entrySet().stream()
                .filter(Objects::nonNull)
                .filter(entry -> entry.getKey() != null)
                .forEach(entry -> result.configLayerRoots.put(entry.getKey().getUrl(), entry.getValue()));
        webRoots.stream()
                .filter(Objects::nonNull)
                .forEach(r -> result.webRoots.add(r.getUrl()));
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
    public List<VirtualFile> getWebRoots() {
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
        webRoots = new ArrayList<>();
        atgModuleName = state.atgModuleName;
        state.configRoots.stream()
                .filter(Objects::nonNull)
                .distinct()
                .forEach(s -> configRoots.add(VirtualFileManager.getInstance().findFileByUrl(s)));
        state.configLayerRoots.entrySet().stream()
                .filter(entry -> entry.getKey() != null)
                .distinct()
                .forEach((entry) -> configLayerRoots.put(VirtualFileManager.getInstance().findFileByUrl(entry.getKey()), entry.getValue()));
        state.webRoots.stream()
                .filter(Objects::nonNull)
                .distinct()
                .forEach(s -> webRoots.add(VirtualFileManager.getInstance().findFileByUrl(s)));
    }

    @SuppressWarnings("WeakerAccess")
    static class State {
        @OptionTag(tag = "atgModule", nameAttribute = "", valueAttribute = "name")
        public String atgModuleName = "";
        @XCollection(elementName = "root", valueAttributeName = "url", propertyElementName = "configRoots")
        public List<String> configRoots = new ArrayList<>();
        @XMap(entryTagName = "root", keyAttributeName = "url", valueAttributeName = "layerName", propertyElementName = "configLayerRoots")
        public Map<String, String> configLayerRoots = new HashMap<>();
        @XCollection(elementName = "root", valueAttributeName = "url", propertyElementName = "webRoots")
        public List<String> webRoots = new ArrayList<>();
    }

}
