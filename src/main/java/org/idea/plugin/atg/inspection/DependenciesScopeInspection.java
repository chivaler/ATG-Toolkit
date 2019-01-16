package org.idea.plugin.atg.inspection;

import com.google.common.collect.Lists;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.properties.PropertiesInspectionBase;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.index.AtgComponentsService;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;

public class DependenciesScopeInspection extends PropertiesInspectionBase {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        PsiFile holderFile = holder.getFile();
        if (!(holderFile instanceof PropertiesFileImpl) || !AtgComponentUtil.getComponentCanonicalName((PropertiesFileImpl) holderFile).isPresent()) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }

        List<String> restrictedScopesForDependency;
        String parentScope = AtgComponentUtil.getComponentScope((PropertiesFileImpl) holderFile);
        switch (parentScope) {
            case Constants.Scope.GLOBAL:
                restrictedScopesForDependency = Lists.newArrayList(Constants.Scope.SESSION, Constants.Scope.WINDOW, Constants.Scope.REQUEST);
                break;
            case Constants.Scope.SESSION:
                restrictedScopesForDependency = Lists.newArrayList(Constants.Scope.WINDOW, Constants.Scope.REQUEST);
                break;
            case Constants.Scope.WINDOW:
                restrictedScopesForDependency = Lists.newArrayList(Constants.Scope.REQUEST);
                break;
            default:
                restrictedScopesForDependency = Lists.newArrayList();
                break;
        }

        if (!restrictedScopesForDependency.isEmpty()) {
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
                            AtgComponentsService componentsService = ServiceManager.getService(holder.getProject(), AtgComponentsService.class);
                            Collection<PropertiesFileImpl> dependencyLayers = componentsService.getComponentsByName(beanName);
                            if (!dependencyLayers.isEmpty()) {
                                PropertiesFileImpl dependency = dependencyLayers.iterator().next();
                                String dependencyScope = AtgComponentUtil.getComponentScope(dependency);
                                if (restrictedScopesForDependency.contains(dependencyScope)) {
                                    holder.registerProblem(element,
                                            new TextRange(start, start + beanName.length()),
                                            AtgToolkitBundle.message("inspection.dependenciesScope.text", dependencyScope));
                                }
                            }
                        }


                    }
                }
            };
        } else return PsiElementVisitor.EMPTY_VISITOR;
    }

}


