package org.idea.plugin.atg.psi.reference;

import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.util.IncorrectOperationException;
import org.fest.util.Lists;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;

public class AtgComponentFieldReference extends PsiPolyVariantReferenceBase<PsiElement> {
    private final String beanName;
    private final String propertyKey;

    public AtgComponentFieldReference(@NotNull PsiElement element, @NotNull TextRange textRange, @NotNull String beanName, @NotNull String propertyKey) {
        super(element, textRange);
        this.beanName = beanName;
        this.propertyKey = propertyKey;
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        Collection<PropertiesFileImpl> applicableComponents = AtgComponentUtil.getApplicableComponentsByName(beanName, null, myElement.getProject());
        return applicableComponents.stream()
                .map(psiFile -> Lists.newArrayList(psiFile.findPropertyByKey(propertyKey), psiFile.findPropertyByKey(propertyKey + "^")))
                .flatMap(Collection::stream)
                .filter(PropertyImpl.class::isInstance)
                .map(element -> new PsiElementResolveResult((PropertyImpl)element, true))
                .toArray(ResolveResult[]::new);
    }


    @NotNull
    @Override
    public Object[] getVariants() {
        return new String[0];
    }

    @Override
    public PsiElement handleElementRename(final String newElementName) {
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
