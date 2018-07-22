/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.idea.plugin.atg;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.JavaProjectRootsUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

public class CreateATGComponentIntentionAction extends PsiElementBaseIntentionAction {

    @Override
    @NotNull
    public String getText() {
        return AtgModuleBundle.message("intentions.create.component");
    }

    @Override
    @NotNull
    public String getFamilyName() {
        return getText();
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        PsiClass psiClass = PsiTreeUtil.getTopmostParentOfType(element, PsiClass.class);
        if (psiClass == null) return false;
        if (psiClass.isAnnotationType() || psiClass.isInterface() || psiClass instanceof PsiAnonymousClass)
            return false;
        PsiModifierList modifierList = psiClass.getModifierList();
        if (modifierList != null && modifierList.hasModifierProperty(PsiModifier.ABSTRACT)) return false;

        PsiFile file = psiClass.getContainingFile();
        if (file.getContainingDirectory() == null || JavaProjectRootsUtil.isOutsideJavaSourceRoot(file)) return false;

        PsiElement leftBrace = psiClass.getLBrace();
        if (leftBrace == null) return false;
        if (element.getTextOffset() >= leftBrace.getTextOffset()) return false;

        return true;
    }

    @Override
    public void invoke(final @NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        final Module srcModule = ModuleUtilCore.findModuleForPsiElement(element);
        if (srcModule == null) return;

        PsiClass srcClass = PsiTreeUtil.getTopmostParentOfType(element, PsiClass.class);

        if (srcClass == null) return;

        PsiDirectory srcDir = element.getContainingFile().getContainingDirectory();
        PsiPackage srcPackage = JavaDirectoryService.getInstance().getPackage(srcDir);

        Module module = ModuleUtilCore.findModuleForFile(element.getContainingFile());
        if (module == null || !project.equals(module.getProject())) return;

        CommandProcessor.getInstance().executeCommand(project, () -> DumbService.getInstance(project).withAlternativeResolveEnabled(() ->
                PropertiesGenerator.generatePropertiesFile(project, module, srcClass, srcPackage)), AtgModuleBundle.message("intentions.create.component"), null);
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
