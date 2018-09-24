package org.idea.plugin.atg.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.psi.PsiMethod;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

public class AtgPropertyLookupElement extends LookupElement {
    private final PsiMethod psiMethod;
    private final String lookupString;

    public AtgPropertyLookupElement(@NotNull final PsiMethod setterMethod) {
        this.psiMethod = setterMethod;
        this.lookupString = AtgComponentUtil.convertSetterToVariableName(psiMethod) + "=";
    }

    public AtgPropertyLookupElement(@NotNull final PsiMethod setterMethod, @NotNull final String lookupString) {
        this.psiMethod = setterMethod;
        this.lookupString = lookupString;
    }

    @NotNull
    @Override
    public Object getObject() {
        return psiMethod;
    }

    @Override
    @NotNull
    public String getLookupString() {
        return lookupString;
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(AtgComponentUtil.convertSetterToVariableName(psiMethod));
        if (psiMethod.getContainingClass() != null) {
            presentation.setTypeGrayed(true);
            presentation.setTypeText("from " + psiMethod.getContainingClass().getQualifiedName());
        }
    }
}
