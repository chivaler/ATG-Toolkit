package org.idea.plugin.atg.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.index.AtgIndexService;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DuplicatedPropertyInspection extends LocalInspectionTool {

    private static final String INSPECTION_DUPLICATED_PROPERTY_KEY = "inspection.duplicatedProperty.text";

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element instanceof PropertiesFileImpl) {
                    PropertiesFileImpl propertiesFile = (PropertiesFileImpl) element;
                    Project project = element.getProject();
                    List<PropertiesFileImpl> propertiesFiles = getComponentPropertiesFiles(propertiesFile, project);

                    for (IProperty propertyItem : propertiesFile.getProperties()) {
                        PropertyImpl property = (PropertyImpl) propertyItem;
                        if (isDuplicatedProperty(property, propertiesFiles)) {
                            String propertyText = property.getText();
                            holder.registerProblem(property, TextRange.allOf(propertyText),
                                    AtgToolkitBundle.message(INSPECTION_DUPLICATED_PROPERTY_KEY, propertyText));
                        }
                    }
                }
            }
        };
    }

    @NotNull
    private List<PropertiesFileImpl> getComponentPropertiesFiles(@NotNull PropertiesFileImpl propertiesFile,
                                                                 @NotNull Project project) {
        AtgIndexService indexService = ServiceManager.getService(project, AtgIndexService.class);
        Optional<String> componentName = AtgComponentUtil.getComponentCanonicalName(propertiesFile);
        return componentName
                .map(indexService::getComponentsByName)
                .orElse(Collections.emptyList());
    }

    private boolean isDuplicatedProperty(@NotNull PropertyImpl property,
                                         @NotNull List<PropertiesFileImpl> propertiesFiles) {
        return propertiesFiles.stream()
                .anyMatch(file -> hasFilePropertyWithValue(file, property));
    }

    private boolean hasFilePropertyWithValue(@NotNull PropertiesFileImpl propertiesFile,
                                             @NotNull PropertyImpl property) {
        boolean hasProperty = false;
        String propertyKey = property.getKey();
        String propertyValue = property.getValue();
        if (propertyKey != null && propertyValue != null) {
            IProperty fileProperty = propertiesFile.findPropertyByKey(propertyKey);
            hasProperty = fileProperty != null && propertyValue.equals(fileProperty.getValue());
        }
        return hasProperty;
    }

}


