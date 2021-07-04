package org.idea.plugin.atg.actions;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.siyeh.ig.psiutils.CommentTracker;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.Constants;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApiStatus.Experimental
public class WrapRepositoryCastIntentionAction extends PsiElementBaseIntentionAction {
    static final Map<Project, Boolean> repsClassAvailablePerProject = new ConcurrentHashMap<>();

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

        PsiTypeCastExpression castExpression = PsiTreeUtil.getParentOfType(element, PsiTypeCastExpression.class);
        if (castExpression == null) return;
        if (!(castExpression.getOperand() instanceof PsiMethodCallExpression)) return;
        PsiMethodCallExpression callExpression = (PsiMethodCallExpression) castExpression.getOperand();

        PsiReferenceExpression methodExpression = callExpression.getMethodExpression();
        String itemName = methodExpression.getFirstChild().getText();

        PsiExpressionList argumentList = callExpression.getArgumentList();
        String propertyName = argumentList.getChildren()[1].getText();

        PsiExpression newRetriever = factory.createExpressionFromText("Reps.getProperty(" + itemName + "," + propertyName + ")", null);
        new CommentTracker().replaceAndRestoreComments(castExpression, newRetriever);
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        if (!ProjectFacetManager.getInstance(project).hasFacets(Constants.FACET_TYPE_ID)) return false;
        repsClassAvailablePerProject.computeIfAbsent(project,
                p -> JavaPsiFacade.getInstance(project)
                        .findClass("kf.repository.Reps", GlobalSearchScope.allScope(project)) != null);

        if (!Boolean.TRUE.equals(repsClassAvailablePerProject.get(project))) return false;

        PsiTypeCastExpression castExpression = PsiTreeUtil.getParentOfType(element, PsiTypeCastExpression.class);
        if (castExpression == null) return false;
        if (!(castExpression.getOperand() instanceof PsiMethodCallExpression)) return false;
        PsiReferenceExpression methodExpression = ((PsiMethodCallExpression) castExpression.getOperand()).getMethodExpression();
        return "getPropertyValue".equals(methodExpression.getLastChild().getText());

    }

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    @NotNull
    public String getText() {
        return AtgToolkitBundle.message("intentions.wrap.getProperty");
    }

}
