package org.idea.plugin.atg.psi.reference.contribution;

import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTagChild;
import com.intellij.util.ProcessingContext;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.psi.reference.AtgComponentReferenceCreator;
import org.idea.plugin.atg.psi.reference.JspFileReference;
import org.idea.plugin.atg.psi.reference.WebContextResourceReference;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class JspUltimateReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(XmlPatterns.xmlAttributeValue()
                .withParent(XmlPatterns.xmlAttribute().withLocalName(Constants.Keywords.BEAN_ATTRIBUTE)
                        .withParent(XmlPatterns.xmlTag().withLocalName(Constants.Keywords.IMPORT_BEAN_TAG))), new JspUltimateReferenceProvider());

        registrar.registerReferenceProvider(XmlPatterns.xmlAttributeValue()
                .withParent(XmlPatterns.xmlAttribute().withLocalName(Constants.Keywords.NAME_ATTRIBUTE)
                        .withParent(XmlPatterns.xmlTag().withLocalName(Constants.Keywords.DROPLET_TAG))), new JspUltimateImportedReferenceProvider());

        registrar.registerReferenceProvider(XmlPatterns.xmlAttributeValue()
                .withParent(XmlPatterns.xmlAttribute().withLocalName(Constants.Keywords.BEAN_ATTRIBUTE)), new JspUltimateImportedReferenceProvider());


        registrar.registerReferenceProvider(XmlPatterns.xmlAttributeValue()
                .withParent(XmlPatterns.xmlAttribute().withLocalName(Constants.Keywords.BEAN_VALUE_ATTRIBUTE)), new JspUltimateImportedReferenceProvider());

        registrar.registerReferenceProvider(XmlPatterns.xmlAttributeValue()
                .withParent(XmlPatterns.xmlAttribute().withLocalName(Constants.Keywords.PAGE_ATTRIBUTE)
                        .withParent(XmlPatterns.xmlTag().withLocalName(Constants.Keywords.INCLUDE_TAG))), new JspIncludeReferenceProvider());

        registrar.registerReferenceProvider(XmlPatterns.xmlAttributeValue()
                .withParent(XmlPatterns.xmlAttribute().withLocalName(Constants.Keywords.SRC_ATTRIBUTE)
                        .withParent(XmlPatterns.xmlTag().withLocalName(Constants.Keywords.SCRIPT_TAG, Constants.Keywords.IMG_TAG))), new WebReferenceProvider());

    }

    static class JspUltimateReferenceProvider extends PsiReferenceProvider {
        @Override
        public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                               @NotNull ProcessingContext context) {
            return AtgComponentReferenceCreator.createReferences((XmlAttributeValue) element, null, new HashMap<>()).toArray(new PsiReference[0]);
        }
    }

    static class JspUltimateImportedReferenceProvider extends PsiReferenceProvider {
        @Override
        public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                               @NotNull ProcessingContext context) {
            HashMap<String, String> importedBeans = new HashMap<>();
            XmlTag parentTag = (XmlTag) element.getParent().getParent();
            while (parentTag != null) {
                XmlTagChild prevSiblingInTag = parentTag;
                while (prevSiblingInTag != null) {
                    prevSiblingInTag = prevSiblingInTag.getPrevSiblingInTag();
                    if (prevSiblingInTag instanceof XmlTag && Constants.Keywords.IMPORT_BEAN_TAG.equals(((XmlTag) prevSiblingInTag).getLocalName())) {
                        XmlAttribute bean = ((XmlTag) prevSiblingInTag).getAttribute(Constants.Keywords.BEAN_ATTRIBUTE);
                        if (bean != null) {
                            XmlAttributeValue beanValue = bean.getValueElement();
                            if (beanValue != null) {
                                String beanName = beanValue.getValue();
                                String shortBeanName = beanName.contains("/") ? beanName.substring(beanName.lastIndexOf('/') + 1) : beanName;
                                importedBeans.put(shortBeanName, beanName);
                            }
                        }
                    }
                }
                PsiElement parentElement = parentTag.getParent();
                parentTag = parentElement instanceof XmlTag ? (XmlTag) parentElement : null;
            }
            return AtgComponentReferenceCreator.createReferences((XmlAttributeValue) element, null, importedBeans).toArray(new PsiReference[0]);
        }
    }

    static class JspIncludeReferenceProvider extends PsiReferenceProvider {
        @NotNull
        @Override
        public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                               @NotNull ProcessingContext context) {
            return new PsiReference[]{new JspFileReference((XmlAttributeValue) element)};
        }
    }

    static class WebReferenceProvider extends PsiReferenceProvider {
        @NotNull
        @Override
        public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                               @NotNull ProcessingContext context) {
            return new PsiReference[]{new WebContextResourceReference((XmlAttributeValue) element)};
        }
    }
}
