package org.idea.plugin.atg.render;

import com.intellij.ide.util.PsiElementListCellRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.presentation.java.SymbolPresentationUtil;

public class AtgPropertiesFileRenderer extends PsiElementListCellRenderer<PsiElement> {

    public static final AtgPropertiesFileRenderer INSTANCE = new AtgPropertiesFileRenderer();

    @Override
    protected int getIconFlags() {
        return Iconable.ICON_FLAG_VISIBILITY;
    }

    @Override
    public String getElementText(PsiElement element) {
        return SymbolPresentationUtil.getSymbolPresentableText(element);
    }

    @Override
    public String getContainerText(PsiElement element, final String name) {
        PsiFile file = element.getContainingFile();
        if (file != null) {
            VirtualFile vFile = file.getVirtualFile();
            Project project = file.getProject();
            ProjectFileIndex projectFileIndex = ProjectFileIndex.getInstance(project);
            String relativePath;
            VirtualFile contentRoot = projectFileIndex.getContentRootForFile(vFile);
            if (contentRoot != null) {
                relativePath = VfsUtilCore.getRelativePath(vFile, contentRoot);
            } else {
                VirtualFile librarySource = projectFileIndex.getClassRootForFile(vFile);
                relativePath = VfsUtilCore.getRelativePath(vFile, librarySource);
            }

            if (relativePath != null) {
                return "/" + relativePath.replace(name, "");
            }
        }

        return SymbolPresentationUtil.getSymbolContainerText(element);
    }

}
