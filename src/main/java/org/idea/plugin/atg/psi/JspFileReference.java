package org.idea.plugin.atg.psi;

import com.intellij.facet.FacetManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.idea.plugin.atg.module.AtgModuleFacet;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class JspFileReference extends PsiPolyVariantReferenceBase<PsiElement> {
    private String filePath;

    public JspFileReference(@NotNull String filePath, @NotNull PsiElement element, @NotNull TextRange range) {
        super(element, range);
        this.filePath = filePath;
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        PsiFile sourceFile = myElement.getContainingFile();
        if (!filePath.startsWith("/")) {
            VirtualFile relativeFile = VfsUtilCore.findRelativeFile(filePath, sourceFile.getVirtualFile());
            if (relativeFile != null) {
                PsiFile psiFile = PsiManager.getInstance(myElement.getProject()).findFile(relativeFile);
                if (psiFile != null) {
                    return new ResolveResult[]{new PsiElementResolveResult(psiFile)};
                }
            }
        } else {
            Module module = ModuleUtilCore.findModuleForFile(sourceFile);
            if (module != null) {
                AtgModuleFacet atgFacet = FacetManager.getInstance(module).getFacetByType(AtgModuleFacet.FACET_TYPE_ID);
                if (atgFacet != null) {
                    Optional<VirtualFile> webRootForSourceFile = atgFacet.getConfiguration().getWebRoots().keySet().stream()
                            .filter(r -> VfsUtilCore.isAncestor(r, sourceFile.getVirtualFile(), false))
                            .findFirst();
                    if (webRootForSourceFile.isPresent()) {
                        VirtualFile absoluteFile = VfsUtilCore.findRelativeFile(filePath, webRootForSourceFile.get());
                        if (absoluteFile != null) {
                            PsiFile psiFile = PsiManager.getInstance(myElement.getProject()).findFile(absoluteFile);
                            if (psiFile != null) {
                                return new ResolveResult[]{new PsiElementResolveResult(psiFile)};
                            }
                        }
                    }
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
