/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.idea.plugin.atg.navigation;

import com.intellij.codeInsight.navigation.GotoTargetHandler;
import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.util.PsiUtilCore;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.PropertiesGenerator;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

public class GoToComponentTargetHandler extends GotoTargetHandler {
    @NotNull
    public static PsiElement getSelectedElement(Editor editor, PsiFile file) {
        return PsiUtilCore.getElementAtOffset(file, editor.getCaretModel().getOffset());
    }

    @Override
    protected String getFeatureUsedKey() {
        return "navigation.goto.component";
    }

    @Override
    @Nullable
    protected GotoData getSourceAndTargetElements(final Editor editor, final PsiFile file) {
        if (!(file instanceof PsiJavaFile)) return null;

        Optional<PsiClass> srcClass = Arrays.stream(((PsiJavaFile) file).getClasses())
                .filter(AtgComponentUtil::isApplicableToHaveComponents)
                .findFirst();
        if (srcClass.isPresent()) {
            Collection<PsiFile> candidates = new ArrayList<>(AtgComponentUtil.suggestComponentsByClass(srcClass.get()));
            List<AdditionalAction> actions = Collections.singletonList(new AdditionalAction() {
                @NotNull
                @Override
                public String getText() {
                    return AtgToolkitBundle.message("intentions.create.component");
                }

                @Override
                public Icon getIcon() {
                    return AllIcons.Actions.IntentionBulb;
                }

                @Override
                public void execute() {
                    PropertiesGenerator.generatePropertiesFile(srcClass.get());
                }
            });
            return new GotoData(srcClass.get(), PsiUtilCore.toPsiElementArray(candidates), actions);
        }


        return null;
    }

    @Override
    protected boolean shouldSortTargets() {
        return false;
    }

    @NotNull
    @Override
    protected String getChooserTitle(@NotNull PsiElement sourceElement, String name, int length, boolean finished) {
        String suffix = finished ? "" : " so far";
        return AtgToolkitBundle.message("navigation.goto.component.chooserTitle.subject", name, length, suffix);
    }

    @NotNull
    @Override
    protected String getFindUsagesTitle(@NotNull PsiElement sourceElement, String name, int length) {
        return AtgToolkitBundle.message("navigation.goto.component.findUsages", name);
    }

    @NotNull
    @Override
    protected String getNotFoundMessage(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return AtgToolkitBundle.message("navigation.goto.component.notFound");
    }

    @Nullable
    @Override
    protected String getAdText(PsiElement source, int length) {
        return null;
    }

    @Override
    protected void navigateToElement(@NotNull Navigatable element) {
        if (element instanceof PsiElement) {
            NavigationUtil.activateFileWithPsiElement((PsiElement) element, true);
        } else {
            element.navigate(true);
        }
    }
}
