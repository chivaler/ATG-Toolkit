package org.idea.plugin.atg.psi.reference;

import com.intellij.facet.Facet;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlAttributeValue;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.config.AtgConfigHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Set;

public class WebContextResourceReference extends PsiPolyVariantReferenceBase<PsiElement> {
    private final String filePath;

    public WebContextResourceReference(@NotNull XmlAttributeValue jspRefAttribute, @NotNull PsiPlainTextFile jspContainingFile, @NotNull TextRange range) {
        super(jspContainingFile, range);
        this.filePath = jspRefAttribute.getValue();
    }

    public WebContextResourceReference(@NotNull XmlAttributeValue jspRefAttribute) {
        super(jspRefAttribute);
        this.filePath = jspRefAttribute.getValue();
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        PsiFile sourceFile = myElement.getContainingFile();
        Project project = sourceFile.getProject();
        if (filePath.startsWith("/")) {
            return ProjectFacetManager.getInstance(project).getFacets(Constants.FACET_TYPE_ID).stream()
                    .map(Facet::getConfiguration)
                    .map(c -> AtgConfigHelper.getWebRootsWithContexts(c, project))
                    .flatMap(Set::stream)
                    .filter(entry -> filePath.startsWith(entry.getSecond()))
                    .map(entry -> VfsUtilCore.findRelativeFile(filePath.substring(entry.getSecond().length()), entry.getFirst()))
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
