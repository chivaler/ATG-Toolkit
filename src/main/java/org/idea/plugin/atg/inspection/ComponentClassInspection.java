package org.idea.plugin.atg.inspection;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.properties.PropertiesInspectionBase;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

public class ComponentClassInspection extends PropertiesInspectionBase {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        PsiFile holderFile = holder.getFile();
        if (!(holderFile instanceof PropertiesFileImpl) || !AtgComponentUtil.getComponentCanonicalName((PropertiesFileImpl) holderFile).isPresent()) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element instanceof PropertyValueImpl) {
                    PsiElement parent = element.getParent();
                    if (parent instanceof PropertyImpl) {
                        String key = ((PropertyImpl) parent).getKey();
                        if (Constants.Keywords.Properties.CLASS_PROPERTY.equals(key)) {
                            String javaClass = element.getText();
                            GlobalSearchScope scope = GlobalSearchScope.allScope(holderFile.getProject());
                            PsiClass psiClass = JavaPsiFacade.getInstance(holderFile.getProject()).findClass(javaClass, scope);
                            if (psiClass == null) {
                                holder.registerProblem(element, AtgToolkitBundle.message("inspection.classNotFound.text", javaClass));
                            }
                        }
                    }
                }
            }
        };
    }

}


