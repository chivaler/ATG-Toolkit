package org.idea.plugin.atg.psi.reference.contribution;

import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.util.ProcessingContext;
import org.apache.commons.lang.StringUtils;
import org.idea.plugin.atg.psi.provider.XmlAttributeComponentNamesProvider;
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
                                .withParent(XmlPatterns.xmlTag().withName("pipelinelink")))), new XmlAttributeComponentNamesProvider());

        registrar.registerReferenceProvider(XmlPatterns.xmlAttributeValue()
                .withParent(XmlPatterns.xmlAttribute().withName("headlink")
                        .withParent(XmlPatterns.xmlTag().withName("pipelinechain"))), new PipelineLinksProvider());

        registrar.registerReferenceProvider(XmlPatterns.xmlAttributeValue()
                .withParent(XmlPatterns.xmlAttribute().withName("link")
                        .withParent(XmlPatterns.xmlTag().withName("transition")
                                .withParent(XmlPatterns.xmlTag().withName("pipelinelink")))), new PipelineLinksProvider());

        registrar.registerReferenceProvider(XmlPatterns.xmlAttributeValue()
                .withParent(XmlPatterns.xmlAttribute().withName("name")
                                .withParent(XmlPatterns.xmlTag().withName("pipelinelink"))), new PipelineLinksProvider());
    }

    static class PipelineLinksProvider extends PsiReferenceProvider {
        @Override
        public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
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
