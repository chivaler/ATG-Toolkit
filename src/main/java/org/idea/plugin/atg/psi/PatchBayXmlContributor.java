package org.idea.plugin.atg.psi;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlText;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.util.ProcessingContext;
import com.jgoodies.common.base.Strings;
import org.idea.plugin.atg.psi.reference.AtgComponentReference;
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
                new ComponentNamesProvider());


//        //TODO search for destinations
//        registrar.registerReferenceProvider(
//                PlatformPatterns.psiElement(XmlTokenType.XML_DATA_CHARACTERS).withParent(
//                        XmlPatterns.xmlText().withParent(
//                                XmlPatterns.xmlTag().withName("destination-name")
//                                        .withParent(XmlPatterns.xmlTag().withName("output-destination", "input-destination")
//                                                .withParent(XmlPatterns.xmlTag().withName("output-port", "input-port")
//                                                        .withParent(XmlPatterns.xmlTag().withName("message-source", "message-sink", "message-filter")
//                                                                .withParent(XmlPatterns.xmlTag().withName("patchbay")
//                                                                        .withParent(XmlPatterns.xmlTag().withName("dynamo-message-system")))))))),
//                        new ComponentNamesProvider());

    }

    static class ComponentNamesProvider extends PsiReferenceProvider {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
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

}
