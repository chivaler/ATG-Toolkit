package org.idea.plugin.atg.roots;

import com.intellij.facet.FacetManager;
import com.intellij.ide.projectView.actions.MarkRootActionBase;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.index.AtgIndexService;
import org.idea.plugin.atg.module.AtgModuleFacet;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.idea.plugin.atg.util.AtgEnvironmentUtil.runWriteAction;

public class MarkAtgConfigRootAction extends MarkRootActionBase {
    //TODO Create abstract

    public MarkAtgConfigRootAction() {
        String typeName = AtgToolkitBundle.message("gui.markAs.configRoot.title");
        Presentation presentation = getTemplatePresentation();
        presentation.setText(typeName);
        presentation.setDescription("Mark directory as a " + typeName.toLowerCase(Locale.getDefault()));
        presentation.setIcon(Constants.Icons.CONFIG_ROOT_ICON);
    }

    @Override
    protected boolean isEnabled(@NotNull RootsSelection selection, @NotNull Module module) {
        if (selection.myHaveSelectedFilesUnderSourceRoots) {
            return false;
        }

        AtgModuleFacet atgFacet = FacetManager.getInstance(module).getFacetByType(Constants.FACET_TYPE_ID);
        return (atgFacet != null && !selection.mySelectedDirectories.isEmpty());

    }

    @Override
    protected void modifyRoots(@NotNull AnActionEvent e, @NotNull Module module, @NotNull VirtualFile[] files) {
        AtgModuleFacet atgFacet = FacetManager.getInstance(module).getFacetByType(Constants.FACET_TYPE_ID);
        if (atgFacet == null) return;
        Set<VirtualFile> changedRoots = new HashSet<>();

        final ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();
        for (VirtualFile file : files) {
            ContentEntry entry = findContentEntry(model, file);
            if (entry != null) {
                final SourceFolder[] sourceFolders = entry.getSourceFolders();
                for (SourceFolder sourceFolder : sourceFolders) {
                    if (Comparing.equal(sourceFolder.getFile(), file)) {
                        entry.removeSourceFolder(sourceFolder);
                        break;
                    }
                }

                Collection<VirtualFile> configRoots = atgFacet.getConfiguration().getConfigRoots();
                if (!configRoots.contains(file)) {
                    configRoots.add(file);
                    changedRoots.add(file);
                }
            }
        }

        if (!changedRoots.isEmpty()) {
            Project project = module.getProject();
            runWriteAction(() -> {
                model.commit();
                project.save();
            });
            ServiceManager.getService(project, AtgIndexService.class).notifyConfigRootsChanged(changedRoots);
        } else {
            model.dispose();
        }

    }

    @Override
    protected void modifyRoots(@NotNull VirtualFile vFile, @NotNull ContentEntry entry) {
        //no op
    }
}