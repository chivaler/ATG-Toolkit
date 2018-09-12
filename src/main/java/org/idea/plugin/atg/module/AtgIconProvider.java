package org.idea.plugin.atg.module;

import com.intellij.facet.FacetManager;
import com.intellij.ide.IconProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import org.idea.plugin.atg.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AtgIconProvider extends IconProvider implements DumbAware {

    @Override
    @Nullable
    public Icon getIcon(@NotNull PsiElement element, int flags) {
        if (element instanceof PsiDirectory) {
            final PsiDirectory psiDirectory = (PsiDirectory) element;
            Module module = ModuleUtilCore.findModuleForPsiElement(psiDirectory);
            final VirtualFile vFile = psiDirectory.getVirtualFile();
            if (module == null) return null;

            AtgModuleFacet atgFacet = FacetManager.getInstance(module).getFacetByType(Constants.FACET_TYPE_ID);
            if (atgFacet != null && atgFacet.getConfiguration().getConfigRoots().contains(vFile)) {
                return Constants.Icons.CONFIG_ROOT_ICON;
            }
            if (atgFacet != null && atgFacet.getConfiguration().getConfigLayerRoots().keySet().contains(vFile)) {
                return Constants.Icons.CONFIG_LAYER_ROOT_ICON;
            }
            if (atgFacet != null && atgFacet.getConfiguration().getWebRoots().contains(vFile)) {
                return Constants.Icons.WEB_ROOT_ICON;
            }
        }
        return null;
    }

}

