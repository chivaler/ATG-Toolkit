package org.idea.plugin.atg.inspection;

import com.google.common.collect.Lists;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

public class DependenciesScopeInspection extends LocalInspectionTool {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        PsiFile holderFile = holder.getFile();
        if (!(holderFile instanceof PropertiesFileImpl) || !AtgComponentUtil.getComponentCanonicalName((PropertiesFileImpl) holderFile).isPresent()) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }

        List<String> restrictedScopesForDependency;
        Set<String> parentScope = AtgComponentUtil.getComponentScopes((PropertiesFileImpl) holderFile);
        if (parentScope.contains(Constants.Scope.GLOBAL)) {
            restrictedScopesForDependency = Lists.newArrayList(Constants.Scope.SESSION, Constants.Scope.WINDOW, Constants.Scope.REQUEST);
        } else if (parentScope.contains(Constants.Scope.SESSION)) {
            restrictedScopesForDependency = Lists.newArrayList(Constants.Scope.WINDOW, Constants.Scope.REQUEST);
        } else if (parentScope.contains(Constants.Scope.WINDOW)) {
            restrictedScopesForDependency = Lists.newArrayList(Constants.Scope.REQUEST);
        } else return PsiElementVisitor.EMPTY_VISITOR;

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element instanceof PropertyValueImpl) {
                    String value = ((PropertyValueImpl) element).getText();
                    Matcher matcher = Constants.SUSPECTED_COMPONENT_NAME_REGEX.matcher(value);
                    while (matcher.find()) {
                        String beanName = matcher.group(0);
                        int start = matcher.start(0);
                        beanName = beanName.contains(".") ? beanName.substring(0, beanName.indexOf('.')) : value;
                        Set<String> componentScopes = AtgComponentUtil.getComponentScopes(beanName, holder.getProject());
                        if (componentScopes.stream().
                                anyMatch(restrictedScopesForDependency::contains)) {
                            boolean treatAsDependency = AtgComponentUtil.getSetterForProperty((PropertyImpl) element.getParent()).
                                    map(AtgComponentUtil::treatAsDependencySetter).
                                    orElse(false);
                            ProblemHighlightType problemHighlightType =  treatAsDependency ? ProblemHighlightType.GENERIC_ERROR_OR_WARNING : ProblemHighlightType.WEAK_WARNING;
                                holder.registerProblem(element, AtgToolkitBundle.message("inspection.dependenciesScope.text", componentScopes),
                                        problemHighlightType, TextRange.from(start, beanName.length()));
                        }
                    }
                }
            }
        };
    }

}


