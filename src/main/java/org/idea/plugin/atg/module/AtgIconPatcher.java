package org.idea.plugin.atg.module;

import com.intellij.facet.FacetManager;
import com.intellij.icons.AllIcons;
import com.intellij.ide.FileIconPatcher;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.LayeredIcon;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AtgIconPatcher implements FileIconPatcher {

    @Override
    public Icon patchIcon(Icon baseIcon, VirtualFile file, int flags, @Nullable Project project) {
        if (project == null || project.isDisposed() || !file.isDirectory()) {
            return baseIcon;
        }

        Module module = ModuleUtilCore.findModuleForFile(file, project);
        if (module == null) return baseIcon;

        AtgModuleFacet atgFacet = FacetManager.getInstance(module).getFacetByType(AtgModuleFacet.FACET_TYPE_ID);
        if (atgFacet != null && atgFacet.getConfiguration().configRoots.contains(file)) {
            return new LayeredIcon(baseIcon, AllIcons.Providers.Oracle);
        }

        return baseIcon;
    }
}

