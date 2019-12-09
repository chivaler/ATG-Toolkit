package org.idea.plugin.atg.editorActions;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.editorActions.CompletionAutoPopupHandler;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class AtgCompletionPropertyAutoPopupHandler extends CompletionAutoPopupHandler {

    @NotNull
    @Override
    public Result checkAutoPopup(char charTyped, Project project, Editor editor, PsiFile file) {
        return needStopped(charTyped, project, editor, file) ? Result.STOP : Result.CONTINUE;
    }

    private boolean needStopped(char charTyped, Project project, Editor editor, PsiFile file) {
        boolean result = false;
        if (file instanceof PropertiesFileImpl && charTyped == '=') {
            AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
            result = true;
        }
        return result;
    }
}
