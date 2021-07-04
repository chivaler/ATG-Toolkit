package org.idea.plugin.atg.psi.provider;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.xml.XmlText;
import com.intellij.util.ProcessingContext;
import com.jgoodies.common.base.Strings;
import org.idea.plugin.atg.psi.reference.AtgComponentReference;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class XmlTextComponentNamesProvider extends PsiReferenceProvider {
    @Override
    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                           @NotNull ProcessingContext
                                                         context) {
        List<PsiReference> results = new ArrayList<>();
        XmlText textElement = (XmlText) element.getParent();
        if (Strings.isNotBlank(textElement.getValue())) {
            results.add(new AtgComponentReference(textElement));
        }
        return results.toArray(PsiReference.EMPTY_ARRAY);
    }
}
