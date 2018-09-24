package org.idea.plugin.atg.psi.provider;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassListReferenceProvider;
import com.intellij.psi.xml.XmlText;
import com.intellij.util.ProcessingContext;
import com.jgoodies.common.base.Strings;
import org.jetbrains.annotations.NotNull;

public class XmlTextJavaClassProvider extends PsiReferenceProvider {
    @NotNull
    @Override
    public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                 @NotNull ProcessingContext
                                                         context) {
        XmlText textElement = (XmlText) element.getParent();
        if (Strings.isNotBlank(textElement.getValue())) {
            JavaClassListReferenceProvider javaClassListReferenceProvider = new JavaClassListReferenceProvider();
            return javaClassListReferenceProvider.getReferencesByString(textElement.getValue(), element, element.getStartOffsetInParent());
        }
        return PsiReference.EMPTY_ARRAY;
    }
}
