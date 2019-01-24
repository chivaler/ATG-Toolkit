package org.idea.plugin.atg.inspection;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.PropertiesInspectionBase;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
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
                        int startInParent = matcher.start(0);

                        if (beanName.contains(".")) {
                            if (startInParent > 0) {
                                if (startInParent == 1 || value.charAt(startInParent - 2) != '^') return;
                            } else {
                                PsiElement parent = element.getParent();
                                if (!(parent instanceof IProperty)) return;
                                String key = ((IProperty) parent).getKey();
                                if (key == null || !key.endsWith("^")) return;
                            }
                            beanName = beanName.substring(0, beanName.indexOf('.'));
                        }
                        beanName = beanName.replaceAll("/+$", "");
                        Project project = element.getProject();
                        AtgComponentsService componentsService = ServiceManager.getService(project, AtgComponentsService.class);
                        Collection<PropertiesFileImpl> dependencyLayers = componentsService.getComponentsByName(beanName);
                        if (dependencyLayers.isEmpty()) {
                            holder.registerProblem(element,
                                    new TextRange(startInParent, startInParent + beanName.length()),
                                    AtgToolkitBundle.message("inspection.dependenciesAbsent.text", beanName));
                        }
                    }


                }
            }
        };
    }
}


