package org.idea.plugin.atg.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.psi.PsiClass;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class AtgComponentLookupElement extends LookupElement {
    private final PropertiesFile targetComponent;
    private final String presentableName;

    public AtgComponentLookupElement(@NotNull final PropertiesFile propertiesFile) {
        this.targetComponent = propertiesFile;
        this.presentableName = AtgComponentUtil.getComponentCanonicalName(targetComponent).orElse(targetComponent.getVirtualFile().getNameWithoutExtension());
    }

    @NotNull
    @Override
    public Object getObject() {
        return targetComponent;
    }

    @Override
    @NotNull
    public String getLookupString() {
        return presentableName;
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(presentableName);
        Optional<PsiClass> componentClass = AtgComponentUtil.getComponentClass(targetComponent.getContainingFile());
        componentClass.ifPresent(psiClass -> presentation.setTypeText(psiClass.getQualifiedName()));
        presentation.setTypeGrayed(true);
    }
}
