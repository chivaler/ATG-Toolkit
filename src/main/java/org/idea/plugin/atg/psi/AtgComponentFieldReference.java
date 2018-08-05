package org.idea.plugin.atg.psi;

import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;

public class AtgComponentFieldReference extends PsiPolyVariantReferenceBase<PsiElement> {
    private final String beanName;
    private final String propertyKey;

    public AtgComponentFieldReference(@NotNull String beanName, @NotNull String propertyKey, @NotNull TextRange textRange, @NotNull PsiPlainTextFile originalFile) {
        super(originalFile, textRange);
        this.beanName = beanName;
        this.propertyKey = propertyKey;
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        Project project = myElement.getProject();
        Collection<PropertiesFileImpl> applicableComponents = AtgComponentUtil.getApplicableComponentsByName(beanName, project);
        return applicableComponents.stream()
                .map(psiFile -> psiFile.findPropertyByKey(propertyKey))
                .filter(PropertyImpl.class::isInstance)
                .map(p -> (PropertyImpl) p)
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
        int endIndex = propertyKey.contains("/") ? propertyKey.lastIndexOf("/") + 1 : 0;
        String newComponentName = propertyKey.substring(0, endIndex) + newElementName.replace(".properties", "");
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
