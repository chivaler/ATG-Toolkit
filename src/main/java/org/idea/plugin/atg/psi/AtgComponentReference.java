package org.idea.plugin.atg.psi;

import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.util.IncorrectOperationException;
import org.apache.commons.lang.StringUtils;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class AtgComponentReference extends PsiPolyVariantReferenceBase<PropertyValueImpl> {
    private final String key;

    public AtgComponentReference(@NotNull PropertyValueImpl element, TextRange textRange) {
        super(element, textRange);
        key = element.getText().substring(textRange.getStartOffset(), textRange.getEndOffset());
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        Project project = myElement.getProject();
        String componentName = key.contains(".") ? key.substring(0, key.lastIndexOf('.')) : key;
        String componentProperty = key.contains(".") ? key.substring(key.lastIndexOf('.') + 1) : "";
        Collection<PsiElement> applicableComponents = AtgComponentUtil.getApplicableComponentsByName(componentName, project);
        Collection<PsiElement> result;
        if (StringUtils.isBlank(componentProperty)) {
            result = applicableComponents;
        } else {
            result = applicableComponents.stream()
                    .filter(PropertiesFileImpl.class::isInstance)
                    .map(psiFile -> ((PropertiesFileImpl) psiFile).findPropertyByKey(componentProperty))
                    .filter(Objects::nonNull)
                    .filter(PropertyImpl.class::isInstance)
                    .map(p -> (PropertyImpl) p)
                    .collect(Collectors.toList());
        }

        return result.stream()
                .map(element -> new PsiElementResolveResult(element, true))
                .toArray(ResolveResult[]::new);
    }


    @NotNull
    @Override
    public Object[] getVariants() {
        return new String[0];
    }

    @Override
    public PsiElement handleElementRename(final String newElementName) throws IncorrectOperationException {
        int endIndex = key.contains("/") ? key.lastIndexOf("/") + 1 : 0;
        String newComponentName = key.substring(0, endIndex) + newElementName.replace(".properties", "");
        return super.handleElementRename(newComponentName);
    }

    @Override
    @SuppressWarnings("OptionalIsPresent")
    public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
        if (element instanceof PropertiesFileImpl) {
            Optional<String> newComponentName = AtgComponentUtil.getComponentCanonicalName((PropertiesFileImpl) element);
            if (newComponentName.isPresent()) {
                return super.handleElementRename(newComponentName.get());
            }
            return null;
        }
        return super.bindToElement(element);
    }
}
