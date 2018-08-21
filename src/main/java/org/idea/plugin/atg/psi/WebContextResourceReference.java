package org.idea.plugin.atg.psi;

import com.intellij.facet.Facet;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.idea.plugin.atg.module.AtgModuleFacet;
import org.idea.plugin.atg.module.AtgModuleFacetConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class WebContextResourceReference extends PsiPolyVariantReferenceBase<PsiElement> {
    private String filePath;

    public WebContextResourceReference(@NotNull String filePath, @NotNull PsiElement element, @NotNull TextRange range) {
        super(element, range);
        this.filePath = filePath;
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        PsiFile sourceFile = myElement.getContainingFile();
        if (filePath.startsWith("/")) {
            return ProjectFacetManager.getInstance(sourceFile.getProject()).getFacets(AtgModuleFacet.FACET_TYPE_ID).stream()
                    .map(Facet::getConfiguration)
                    .map(AtgModuleFacetConfiguration::getWebRoots)
                    .map(Map::entrySet)
                    .flatMap(Set::stream)
                    .filter(entry -> filePath.startsWith(entry.getValue()))
                    .map(entry -> VfsUtilCore.findRelativeFile(filePath.substring(entry.getValue().length()), entry.getKey()))
                    .filter(Objects::nonNull)
                    .map(vFile -> PsiManager.getInstance(myElement.getProject()).findFile(vFile))
                    .filter(Objects::nonNull)
                    .map(PsiElementResolveResult::new)
                    .toArray(ResolveResult[]::new);
        } else {
            VirtualFile relativeFile = VfsUtilCore.findRelativeFile(filePath, sourceFile.getVirtualFile());
            if (relativeFile != null) {
                PsiFile psiFile = PsiManager.getInstance(myElement.getProject()).findFile(relativeFile);
                if (psiFile != null) {
                    return new ResolveResult[]{new PsiElementResolveResult(psiFile)};
                }
            }
        }
        return new ResolveResult[0];
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return new Object[0];
    }
}
