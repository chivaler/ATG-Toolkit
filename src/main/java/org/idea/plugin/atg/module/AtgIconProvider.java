package org.idea.plugin.atg.module;

import com.intellij.facet.FacetManager;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IconProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.ScalableIcon;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.ui.LayeredIcon;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AtgIconProvider extends IconProvider implements DumbAware {
    private final LayeredIcon CONFIG_FOLDER_ICON;

    {
        CONFIG_FOLDER_ICON = new LayeredIcon(2);
        CONFIG_FOLDER_ICON.setIcon(PlatformIcons.DIRECTORY_CLOSED_ICON, 0);
        CONFIG_FOLDER_ICON.setIcon(((ScalableIcon) AllIcons.Providers.Oracle).scale(0.5F), 1, SwingConstants.SOUTH_EAST);
    }

    @Override
    @Nullable
    public Icon getIcon(@NotNull PsiElement element, int flags) {
        if (element instanceof PsiDirectory) {
            final PsiDirectory psiDirectory = (PsiDirectory) element;
            Module module = ModuleUtilCore.findModuleForPsiElement(psiDirectory);
            final VirtualFile vFile = psiDirectory.getVirtualFile();
            if (module == null) return null;

            AtgModuleFacet atgFacet = FacetManager.getInstance(module).getFacetByType(AtgModuleFacet.FACET_TYPE_ID);
            if (atgFacet != null && atgFacet.getConfiguration().configRoots.contains(vFile)) {
                return CONFIG_FOLDER_ICON;
            }
        }
        return null;
    }

}

