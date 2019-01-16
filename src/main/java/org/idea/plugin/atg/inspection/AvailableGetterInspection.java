package org.idea.plugin.atg.inspection;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.properties.PropertiesInspectionBase;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.index.AtgComponentsService;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.regex.Matcher;

public class AvailableGetterInspection extends PropertiesInspectionBase {

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
                        int indexOfDot = beanName.indexOf('.');
                        if (indexOfDot >= 0) {
                            int start = matcher.start(0) + indexOfDot + 1;
                            String linkedPropertyName = beanName.substring(indexOfDot + 1);
                            beanName = beanName.contains(".") ? beanName.substring(0, indexOfDot) : value;
                            Project project = holder.getProject();
                            AtgComponentsService componentsService = ServiceManager.getService(project, AtgComponentsService.class);
                            Collection<PropertiesFileImpl> dependencyLayers = componentsService.getComponentsByName(beanName);
                            if (!dependencyLayers.isEmpty()) {
                                PropertiesFileImpl dependency = dependencyLayers.iterator().next();
                                Optional<PsiClass> dependencyClass = AtgComponentUtil.getComponentClass(dependency);
                                if (dependencyClass.isPresent()) {
                                    Optional<PsiMethod> getter = AtgComponentUtil.getGetter(dependencyClass.get(), linkedPropertyName);
                                    if (!getter.isPresent()) {
                                        holder.registerProblem(element,
                                                new TextRange(start, start + linkedPropertyName.length()),
                                                AtgToolkitBundle.message("inspection.notAvailableMethod.text",
                                                        AtgComponentUtil.convertVariableToGetters(linkedPropertyName),
                                                        dependencyClass.get().getQualifiedName()));
                                    }
                                }
                            }
                        }
                    }


                }
            }

        };
    }
}

