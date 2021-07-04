package org.idea.plugin.atg.psi.provider;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassListReferenceProvider;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.util.ProcessingContext;
import com.jgoodies.common.base.Strings;
import org.jetbrains.annotations.NotNull;

public class XmlAttributeJavaClassProvider extends PsiReferenceProvider {
    @Override
    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                           @NotNull ProcessingContext
                                                         context) {
        XmlAttributeValue valueElement = (XmlAttributeValue) element;
        if (Strings.isNotBlank(valueElement.getValue())) {
            JavaClassListReferenceProvider javaClassListReferenceProvider = new JavaClassListReferenceProvider();
            return javaClassListReferenceProvider.getReferencesByString(((XmlAttributeValue) element).getValue(), element, 0);
        }
        return PsiReference.EMPTY_ARRAY;
    }
}
