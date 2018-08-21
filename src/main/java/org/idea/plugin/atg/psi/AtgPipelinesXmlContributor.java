package org.idea.plugin.atg.psi;

import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.util.ProcessingContext;
import com.jgoodies.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.idea.plugin.atg.psi.reference.AtgComponentReference;
import org.idea.plugin.atg.psi.reference.PipelineLinkReference;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AtgPipelinesXmlContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(XmlPatterns.xmlAttributeValue()
                .withParent(XmlPatterns.xmlAttribute().withName("jndi")
                        .withParent(XmlPatterns.xmlTag().withName("processor")
                                .withParent(XmlPatterns.xmlTag().withName("pipelinelink")))), new ComponentNamesProvider());

        registrar.registerReferenceProvider(XmlPatterns.xmlAttributeValue()
                .withParent(XmlPatterns.xmlAttribute().withName("headlink")
                        .withParent(XmlPatterns.xmlTag().withName("pipelinechain"))), new PipelineLinksProvider());

        registrar.registerReferenceProvider(XmlPatterns.xmlAttributeValue()
                .withParent(XmlPatterns.xmlAttribute().withName("link")
                        .withParent(XmlPatterns.xmlTag().withName("transition")
                                .withParent(XmlPatterns.xmlTag().withName("pipelinelink")))), new PipelineLinksProvider());
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

    static class PipelineLinksProvider extends PsiReferenceProvider {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                     @NotNull ProcessingContext
                                                             context) {
            List<PsiReference> results = new ArrayList<>();
            XmlAttributeValue valueElement = (XmlAttributeValue) element;
            if (StringUtils.isNotBlank(valueElement.getValue())) {
                results.add(new PipelineLinkReference(valueElement));
            }

            return results.toArray(PsiReference.EMPTY_ARRAY);
        }
    }

}
