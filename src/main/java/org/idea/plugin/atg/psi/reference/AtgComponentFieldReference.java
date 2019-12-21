package org.idea.plugin.atg.psi.reference;

import com.google.common.collect.Lists;
import com.intellij.codeInsight.highlighting.ReadWriteAccessDetector;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.idea.plugin.atg.index.AtgIndexService;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class AtgComponentFieldReference extends PsiPolyVariantReferenceBase<PsiElement>  implements AccessDefinedJavaFieldPsiReference{
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
        Project project = myElement.getProject();
        GlobalSearchScope projectScope = GlobalSearchScope.projectScope(project);
        AtgIndexService componentsService = ServiceManager.getService(project, AtgIndexService.class);

        Stream<PsiElement> nucleusPropertiesStream = componentsService.getComponentsByName(beanName).stream()
                .map(psiFile -> Lists.newArrayList(psiFile.findPropertyByKey(propertyKey), psiFile.findPropertyByKey(propertyKey + "^")))
                .flatMap(Collection::stream)
                .filter(PropertyImpl.class::isInstance)
                .map(PsiElement.class::cast);

        Stream<PsiField> psiFieldStream = AtgComponentUtil.getComponentClassesStr(beanName, project).stream()
                .map(clsStr -> JavaPsiFacade.getInstance(project).findClass(clsStr, projectScope))
                .filter(Objects::nonNull)
                .map(cls -> JavaPropertyReference.getPsiField(cls, propertyKey))
                .filter(Optional::isPresent)
                .map(Optional::get);

        return Stream.concat(nucleusPropertiesStream, psiFieldStream)
                .map(PsiElementResolveResult::new)
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
    public PsiElement bindToElement(@NotNull PsiElement element) {
        if (element instanceof PropertiesFileImpl) {
            Optional<String> newComponentName = AtgComponentUtil.getComponentCanonicalName((PropertiesFileImpl) element);
            if (newComponentName.isPresent()) {
                return super.handleElementRename(newComponentName.get());
            }
            return null;
        }
        return super.bindToElement(element);
    }

    @NotNull
    @Override
    public ReadWriteAccessDetector.Access getAccessType() {
        return ReadWriteAccessDetector.Access.Read;
    }
}