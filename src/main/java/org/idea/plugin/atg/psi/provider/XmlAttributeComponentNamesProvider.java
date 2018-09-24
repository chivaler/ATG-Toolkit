package org.idea.plugin.atg.psi.provider;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.util.ProcessingContext;
import com.jgoodies.common.base.Strings;
import org.idea.plugin.atg.psi.reference.AtgComponentReference;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class XmlAttributeComponentNamesProvider extends PsiReferenceProvider {
    @NotNull
    @Override
    public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                 @NotNull ProcessingContext
                                                         context) {
        List<PsiReference> results = new ArrayList<>();
        XmlAttributeValue valueElement = (XmlAttributeValue) element;
        if (Strings.isNotBlank(valueElement.getValue())) {
            results.add(new AtgComponentReference(valueElement));
        }
        return results.toArray(PsiReference.EMPTY_ARRAY);
    }
}
