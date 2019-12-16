package org.idea.plugin.atg.actions;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.JavaProjectRootsUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.PropertiesGenerator;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

public class CreateATGComponentIntentionAction extends PsiElementBaseIntentionAction {

    @Override
    @NotNull
    public String getText() {
        return AtgToolkitBundle.message("intentions.create.component");
    }

    @Override
    @NotNull
    public String getFamilyName() {
        return getText();
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        if (!ProjectFacetManager.getInstance(project).hasFacets(Constants.FACET_TYPE_ID)) return false;

        PsiClass psiClass = PsiTreeUtil.getTopmostParentOfType(element, PsiClass.class);
        if (psiClass == null) return false;

        PsiFile file = psiClass.getContainingFile();
        if (file.getContainingDirectory() == null || JavaProjectRootsUtil.isOutsideJavaSourceRoot(file)) return false;

        PsiElement leftBrace = psiClass.getLBrace();
        if (leftBrace == null) return false;
        if (element.getTextOffset() >= leftBrace.getTextOffset()) return false;
        return AtgComponentUtil.isApplicableToHaveComponents(psiClass);
    }

    @Override
    public void invoke(final @NotNull Project project, Editor editor, @NotNull PsiElement element) {
        final Module srcModule = ModuleUtilCore.findModuleForPsiElement(element);
        if (srcModule == null) return;

        PsiClass srcClass = PsiTreeUtil.getTopmostParentOfType(element, PsiClass.class);

        if (srcClass == null) return;

        PropertiesGenerator.generatePropertiesFile(srcClass);
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
