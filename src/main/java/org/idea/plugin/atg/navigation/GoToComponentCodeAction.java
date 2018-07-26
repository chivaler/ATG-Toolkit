package org.idea.plugin.atg.navigation;

import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.impl.source.PsiClassImpl;
import com.intellij.psi.util.PsiUtilBase;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

public class GoToComponentCodeAction extends BaseCodeInsightAction {
    @Override
    @NotNull
    protected GoToComponentTargetHandler getHandler() {
        return new GoToComponentTargetHandler();
    }

    @Override
    public void update(AnActionEvent e) {
        final Presentation presentation = e.getPresentation();
        presentation.setEnabledAndVisible(false);

        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);

        if (editor == null || project == null) return;

        PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
        if (!(psiFile instanceof PsiJavaFile)) return;
        Optional<PsiClass> srcClass = Arrays.stream(((PsiJavaFile) psiFile).getClasses())
                .filter(PsiClassImpl.class::isInstance)
                .filter(c -> c.hasModifierProperty(PsiModifier.PUBLIC))
                .findFirst();

        if (srcClass.isPresent() && !AtgComponentUtil.suggestComponentsByClass(srcClass.get()).isEmpty()) {
            presentation.setText(AtgToolkitBundle.message("navigation.goto.component.text"));
            presentation.setDescription(AtgToolkitBundle.message("navigation.goto.component.description"));
            presentation.setEnabledAndVisible(true);
        }

    }
}
