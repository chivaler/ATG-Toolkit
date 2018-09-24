package org.idea.plugin.atg.psi;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlText;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.util.ProcessingContext;
import com.jgoodies.common.base.Strings;
import org.idea.plugin.atg.psi.provider.XmlTextComponentNamesProvider;
import org.idea.plugin.atg.psi.reference.PatchBayDestinationReference;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PatchBayXmlContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement(XmlTokenType.XML_DATA_CHARACTERS).withParent(
                        XmlPatterns.xmlText().withParent(
                                XmlPatterns.xmlTag().withName("nucleus-name")
                                        .withParent(XmlPatterns.xmlTag().withName("message-source", "message-sink", "message-filter")
                                                .withParent(XmlPatterns.xmlTag().withName("patchbay")
                                                        .withParent(XmlPatterns.xmlTag().withName("dynamo-message-system")))))),
                new XmlTextComponentNamesProvider());

        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement(XmlTokenType.XML_DATA_CHARACTERS).withParent(
                        XmlPatterns.xmlText().withParent(
                                XmlPatterns.xmlTag().withName("initial-context-factory")
                                        .withParent(XmlPatterns.xmlTag().withName("provider")
                                                .withParent(XmlPatterns.xmlTag().withName("patchbay")
                                                        .withParent(XmlPatterns.xmlTag().withName("dynamo-message-system")))))),
                new XmlTextComponentNamesProvider());

        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement(XmlTokenType.XML_DATA_CHARACTERS).withParent(
                        XmlPatterns.xmlText().withParent(
                                XmlPatterns.xmlTag().withName("destination-name")
                                        .withParent(XmlPatterns.xmlTag().withName("input-destination", "output-destination")
                                                .withParent(XmlPatterns.xmlTag().withName("input-port", "output-port", "redelivery-port")
                                                        .withParent(XmlPatterns.xmlTag().withName("message-sink", "message-source", "message-filter")
                                                                .withParent(XmlPatterns.xmlTag().withName("patchbay")
                                                                        .withParent(XmlPatterns.xmlTag().withName("dynamo-message-system")))))))),
                new PatchBayDestinationProvider());


//        registrar.registerReferenceProvider(
//                PlatformPatterns.psiElement(XmlTokenType.XML_DATA_CHARACTERS).withParent(
//                        XmlPatterns.xmlText().withParent(
//                                XmlPatterns.xmlTag().withName("jms-type", "message-class")
//                                        .withParent(XmlPatterns.xmlTag().withName("message-type")
//                                                .withParent(XmlPatterns.xmlTag().withName("message-family")
//                                                        .withParent(XmlPatterns.xmlTag().withName("message-registry")
//                                                                .withParent(XmlPatterns.xmlTag().withName("dynamo-message-system"))))))),
//                new XmlTextJavaClassProvider());

        //TODO search for providers
    }

    static class PatchBayDestinationProvider extends PsiReferenceProvider {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                     @NotNull ProcessingContext
                                                             context) {

            List<PsiReference> results = new ArrayList<>();
            XmlText textElement = (XmlText) element.getParent();
            if (Strings.isNotBlank(textElement.getText())) {
                results.add(new PatchBayDestinationReference(textElement));
            }
            return results.toArray(PsiReference.EMPTY_ARRAY);
        }
    }
}
