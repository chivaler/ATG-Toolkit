package org.idea.plugin.atg.psi;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

public class FileReference extends PsiPolyVariantReferenceBase<PsiElement> {
    private String filePath;

    public FileReference(@NotNull String filePath, @NotNull PsiElement element, @NotNull TextRange range) {
        super(element, range);
        this.filePath = filePath;
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        PsiFile referencedFile = myElement.getContainingFile();
        if (!filePath.startsWith("/")) {
            VirtualFile relativeFile = VfsUtilCore.findRelativeFile(filePath, referencedFile.getVirtualFile());
            if(relativeFile != null) {
                PsiFile psiFile = PsiManager.getInstance(myElement.getProject()).findFile(relativeFile);
                if(psiFile != null) {
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
