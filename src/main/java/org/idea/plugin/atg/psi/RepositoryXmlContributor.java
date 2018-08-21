package org.idea.plugin.atg.psi;

import com.intellij.patterns.StandardPatterns;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.util.ProcessingContext;
import com.jgoodies.common.base.Strings;
import org.idea.plugin.atg.psi.reference.AtgComponentReference;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RepositoryXmlContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(XmlPatterns.xmlAttributeValue()
                .withParent(XmlPatterns.xmlAttribute().withName("value")
                .withParent(XmlPatterns.xmlTag().withName("attribute")
                .withParent(XmlPatterns.xmlTag().withName("property")
                .withParent(XmlPatterns.xmlTag().withName("item-descriptor")))))
                .withValue(StandardPatterns.string().startsWith("/")), new ComponentNamesProvider());

    }

    static class ComponentNamesProvider extends PsiReferenceProvider {
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
}
