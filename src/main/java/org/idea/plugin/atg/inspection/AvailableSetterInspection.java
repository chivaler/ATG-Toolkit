package org.idea.plugin.atg.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.ASTNode;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class AvailableSetterInspection extends LocalInspectionTool {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        PsiFile psiFile = holder.getFile();
        Optional<PsiClass> componentClass = AtgComponentUtil.getSupposedComponentClass(psiFile);
        if (!componentClass.isPresent()) return PsiElementVisitor.EMPTY_VISITOR;

        return new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (element instanceof PropertyImpl) {
                    PropertyImpl propertyElement = (PropertyImpl) element;
                    if (propertyElement.getKey() != null && !propertyElement.getKey().startsWith("$")) {
                        Optional<PsiMethod> setterForProperty = AtgComponentUtil.getSetterForProperty(propertyElement);
                        ASTNode keyNode = propertyElement.getKeyNode();
                        String componentClassStr = componentClass.map(PsiClass::getQualifiedName).orElse("");
                        if (!setterForProperty.isPresent() && keyNode instanceof PropertyKeyImpl) {
                            String key = keyNode.getText();
                            if (key.endsWith("^") || key.endsWith("+") || key.endsWith("-")) {
                                key = key.substring(0, key.length() - 1);
                            }
                            String setterName = AtgComponentUtil.convertPropertyNameToSetter(key);
                            holder.registerProblem((PropertyKeyImpl) keyNode,
                                    TextRange.allOf(key),
                                    AtgToolkitBundle.message("inspection.notAvailableMethod.text", setterName, componentClassStr));
                        }
                    }
                }
            }
        };
    }

}


