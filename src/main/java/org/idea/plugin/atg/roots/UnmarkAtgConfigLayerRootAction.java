package org.idea.plugin.atg.roots;

import com.intellij.facet.FacetManager;
import com.intellij.ide.projectView.actions.MarkRootActionBase;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.index.AtgIndexService;
import org.idea.plugin.atg.module.AtgModuleFacet;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.idea.plugin.atg.util.AtgEnvironmentUtil.runWriteAction;

public class UnmarkAtgConfigLayerRootAction extends MarkRootActionBase {

    public UnmarkAtgConfigLayerRootAction() {
        String typeName = AtgToolkitBundle.message("gui.unMarkAs.configRoot.title");
        Presentation presentation = getTemplatePresentation();
        presentation.setText(typeName);
        presentation.setDescription("Mark directory as an ordinary ");
    }

    @Override
    protected boolean isEnabled(@NotNull RootsSelection selection, @NotNull Module module) {
        if (selection.mySelectedDirectories.isEmpty()) {
            return false;
        }

        AtgModuleFacet atgFacet = FacetManager.getInstance(module).getFacetByType(Constants.FACET_TYPE_ID);
        if (atgFacet == null) {
            return false;
        }

        for (VirtualFile selectedRoot : selection.mySelectedDirectories) {
            boolean configRootFound = atgFacet.getConfiguration().getConfigRoots().contains(selectedRoot);
            boolean configLayerFound = atgFacet.getConfiguration().getConfigLayerRoots().containsKey(selectedRoot);
            boolean webRootFound = atgFacet.getConfiguration().getWebRoots().contains(selectedRoot);
            if (configRootFound || configLayerFound || webRootFound) return true;
        }

        return false;

    }

    @Override
    protected void modifyRoots(@NotNull AnActionEvent e, @NotNull Module module, @NotNull VirtualFile[] files) {
        AtgModuleFacet atgFacet = FacetManager.getInstance(module).getFacetByType(Constants.FACET_TYPE_ID);
        if (atgFacet == null) return;

        List<VirtualFile> configRootsToRemove = new ArrayList<>();
        List<VirtualFile> configLayerRootsToRemove = new ArrayList<>();
        List<VirtualFile> webRootsToRemove = new ArrayList<>();

        for (VirtualFile selectedFile : files) {
            atgFacet.getConfiguration().getConfigRoots().stream()
                    .filter(Objects::nonNull)
                    .filter(c -> c.equals(selectedFile))
                    .forEach(configRootsToRemove::add);
            atgFacet.getConfiguration().getConfigLayerRoots().keySet().stream()
                    .filter(Objects::nonNull)
                    .filter(c -> c.equals(selectedFile))
                    .forEach(configLayerRootsToRemove::add);
            atgFacet.getConfiguration().getWebRoots().stream()
                    .filter(Objects::nonNull)
                    .filter(c -> c.equals(selectedFile))
                    .forEach(webRootsToRemove::add);
        }

        if (!configRootsToRemove.isEmpty() || !configLayerRootsToRemove.isEmpty() || !webRootsToRemove.isEmpty()) {
            atgFacet.getConfiguration().getConfigRoots().removeAll(configRootsToRemove);
            atgFacet.getConfiguration().getConfigLayerRoots().keySet().removeAll(configLayerRootsToRemove);
            atgFacet.getConfiguration().getWebRoots().removeAll(webRootsToRemove);

            Project project = module.getProject();
            runWriteAction(() -> {
                ModuleRootManager.getInstance(module).getModifiableModel().commit();
                project.save();
            });
            AtgIndexService componentsService = ServiceManager.getService(project, AtgIndexService.class);
            componentsService.notifyConfigRootsChanged(configRootsToRemove);
            componentsService.notifyConfigRootsChanged(configLayerRootsToRemove);
        }


    }

    @Override
    protected void modifyRoots(@NotNull VirtualFile vFile, @NotNull ContentEntry entry) {
        //no op
    }
}