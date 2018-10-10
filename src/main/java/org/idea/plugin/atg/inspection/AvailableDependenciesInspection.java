package org.idea.plugin.atg.inspection;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.properties.PropertiesInspectionBase;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.regex.Matcher;

public class AvailableDependenciesInspection extends PropertiesInspectionBase {

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
                    String value = ((PropertyValueImpl) element).getText();
                    Matcher matcher = Constants.SUSPECTED_COMPONENT_NAME_REGEX.matcher(value);
                    while (matcher.find()) {
                        String beanName = matcher.group(0);
                        int start = matcher.start(0);
                        beanName = beanName.contains(".") ? beanName.substring(0, beanName.indexOf('.')) : beanName;
                        Collection<PropertiesFileImpl> dependencyLayers = AtgComponentUtil.getApplicableComponentsByName(beanName, null, holder.getProject());
                        if (dependencyLayers.isEmpty()) {
                            holder.registerProblem(element,
                                    new TextRange(start, start + beanName.length()),
                                    AtgToolkitBundle.message("inspection.dependenciesAbsent.text", beanName));
                        }
                    }


                }
            }
        };
    }
}


