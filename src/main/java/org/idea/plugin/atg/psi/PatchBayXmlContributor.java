package org.idea.plugin.atg.psi;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.xml.XmlTokenType;
import org.idea.plugin.atg.psi.provider.XmlTextComponentNamesProvider;
import org.idea.plugin.atg.psi.provider.XmlTextJavaClassProvider;
import org.jetbrains.annotations.NotNull;

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

        //TODO
//        registrar.registerReferenceProvider(
//                PlatformPatterns.psiElement(XmlTokenType.XML_DATA_CHARACTERS).withParent(
//                        XmlPatterns.xmlText().withParent(
//                                XmlPatterns.xmlTag().withName("provider-name")
//                                        .withParent(XmlPatterns.xmlTag().withName("provider")
//                                                .withParent(XmlPatterns.xmlTag().withName("patchbay")
//                                                        .withParent(XmlPatterns.xmlTag().withName("dynamo-message-system")))))),
//                new XmlTextComponentNamesProvider());


        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement(XmlTokenType.XML_DATA_CHARACTERS).withParent(
                        XmlPatterns.xmlText().withParent(
                                XmlPatterns.xmlTag().withName("jms-type", "message-class")
                                        .withParent(XmlPatterns.xmlTag().withName("message-type")
                                                .withParent(XmlPatterns.xmlTag().withName("message-family")
                                                        .withParent(XmlPatterns.xmlTag().withName("message-registry")
                                                                .withParent(XmlPatterns.xmlTag().withName("dynamo-message-system"))))))),
                new XmlTextJavaClassProvider());

        //TODO search for destinations
        //TODO search for providers
        //TODO search for destination-names

    }

}
