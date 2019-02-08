package org.idea.plugin.atg.psi.reference;

import com.google.common.collect.Lists;
import com.intellij.lang.properties.PropertiesFileType;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlText;
import org.idea.plugin.atg.index.AtgIndexService;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;

public class AtgComponentReference extends PsiPolyVariantReferenceBase<PsiElement> {
    private final String componentName;

    public AtgComponentReference(@NotNull PropertyValueImpl element, @NotNull TextRange textRange) {
        super(element, textRange);
        componentName = element.getText().substring(textRange.getStartOffset(), textRange.getEndOffset());
    }

    public AtgComponentReference(@NotNull XmlAttributeValue element) {
        super(element);
        componentName = element.getValue();
    }

    public AtgComponentReference(@NotNull XmlText element) {
        super(element);
        componentName = element.getValue().trim();
    }

    public AtgComponentReference(@NotNull String componentName, @NotNull TextRange textRange, @NotNull PsiElement element) {
        super(element, textRange);
        this.componentName = componentName;
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        Project project = myElement.getProject();
        AtgIndexService componentsService = ServiceManager.getService(project, AtgIndexService.class);
        Collection<PsiFile> applicableComponents;
        if (componentName.endsWith(".xml")) {
            applicableComponents = Lists.newArrayList(componentsService.getXmlsByName(componentName));
        } else {
            applicableComponents = Lists.newArrayList(componentsService.getComponentsByName(componentName));
        }
        return applicableComponents.stream()
                .map(element -> new PsiElementResolveResult(element, true))
                .toArray(ResolveResult[]::new);
    }


    @NotNull
    @Override
    public Object[] getVariants() {
        return new String[0];
    }

    @Override
    public PsiElement handleElementRename(final String newElementName) {
        int endIndex = componentName.contains("/") ? componentName.lastIndexOf("/") + 1 : 0;
        String newComponentName = componentName.substring(0, endIndex) + newElementName.replace(PropertiesFileType.DOT_DEFAULT_EXTENSION, "");
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
}
