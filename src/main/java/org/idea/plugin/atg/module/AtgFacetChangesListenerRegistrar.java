package org.idea.plugin.atg.module;

import com.intellij.facet.ProjectWideFacetAdapter;
import com.intellij.facet.ProjectWideFacetListenersRegistry;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.index.AtgIndexService;

public class AtgFacetChangesListenerRegistrar implements ProjectComponent {
    private final Project project;
    private final ProjectWideFacetListenersRegistry facetListenersRegistry;

    public AtgFacetChangesListenerRegistrar(Project project, ProjectWideFacetListenersRegistry facetListenersRegistry) {
        this.project = project;
        this.facetListenersRegistry = facetListenersRegistry;
    }

    @Override
    public void projectOpened() {
        facetListenersRegistry.registerListener(Constants.FACET_TYPE_ID, new ProjectWideFacetAdapter<AtgModuleFacet>() {
            @Override
            public void facetRemoved(AtgModuleFacet atgModuleFacet) {
                AtgModuleFacetConfiguration configuration = atgModuleFacet.getConfiguration();
                AtgIndexService indexService = ServiceManager.getService(project, AtgIndexService.class);
                indexService.notifyConfigRootsChanged(configuration.getConfigLayerRoots().keySet());
                indexService.notifyConfigRootsChanged(configuration.getConfigRoots());
            }
        });
    }
}
