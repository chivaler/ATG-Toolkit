package org.idea.plugin.atg.psi.reference;

import com.intellij.facet.FacetManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlAttributeValue;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.module.AtgModuleFacet;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class JspFileReference extends PsiPolyVariantReferenceBase<PsiElement> {
    private final String filePath;

    public JspFileReference(@NotNull XmlAttributeValue jspRefAttribute, @NotNull PsiPlainTextFile jspContainingFile, @NotNull TextRange range) {
        super(jspContainingFile, range);
        this.filePath = jspRefAttribute.getValue();
    }

    public JspFileReference(@NotNull XmlAttributeValue jspRefAttribute) {
        super(jspRefAttribute);
        this.filePath = jspRefAttribute.getValue();
    }

    @Override
    public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
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
                AtgModuleFacet atgFacet = FacetManager.getInstance(module).getFacetByType(Constants.FACET_TYPE_ID);
                if (atgFacet != null) {
                    Optional<VirtualFile> webRootForSourceFile = atgFacet.getConfiguration().getWebRoots().stream()
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

    @Override
    public Object @NotNull [] getVariants() {
        return new Object[0];
    }
}
