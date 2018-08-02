package org.idea.plugin.atg.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public class JspComponentPropertiesCompletionContributor extends CompletionContributor {
    public JspComponentPropertiesCompletionContributor() {
        extend(CompletionType.BASIC, psiElement(), new JspCompletionProvider());
    }

    static class JspCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters,
                                      ProcessingContext context,
                                      @NotNull CompletionResultSet result) {
            PsiElement position = parameters.getPosition().getContext();
            VirtualFile virtualFile = position.getContainingFile().getVirtualFile();
            if (virtualFile.getCanonicalPath().endsWith(".jsp")) {
                String key = "1";

            }
        }
    }


}