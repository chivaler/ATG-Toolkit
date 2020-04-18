package org.idea.plugin.atg.psi.reference.contribution;

import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ProcessingContext;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.psi.reference.AtgComponentReferenceCreator;
import org.idea.plugin.atg.psi.reference.JspFileReference;
import org.idea.plugin.atg.psi.reference.WebContextResourceReference;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class JspReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(PsiPlainTextFile.class)
                        .and(PlatformPatterns.psiFile().withVirtualFile(PlatformPatterns.virtualFile().withExtension("jsp", "jspf"))),
                new JspReferenceProvider());
    }

    static class JspReferenceProvider extends PsiReferenceProvider {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                     @NotNull ProcessingContext context) {
            PsiFile jspPsiFile = PsiFileFactory.getInstance(element.getProject()).createFileFromText(XMLLanguage.INSTANCE, element.getText());
            JspReferenceProviderVisitor jspVisitor = new JspReferenceProviderVisitor((PsiPlainTextFile) element.getContainingFile());
            jspPsiFile.accept(jspVisitor);
            return jspVisitor.getCreatedReferences().toArray(new PsiReference[0]);
        }
    }

    static class JspReferenceProviderVisitor extends PsiRecursiveElementWalkingVisitor {
        private final PsiPlainTextFile originalFile;
        private final Map<String, String> activeImports = new HashMap<>();
        private final List<PsiReference> createdReferences = new ArrayList<>();
        private final List<String> tagsContainingBeanAttribute = Arrays.asList("importbean", "param", "getvalueof", "a", "select", "input", "valueof", "tomap", "textarea", "setvalue", "property", "postfield");
        private final List<String> tagsContainingBeanValueAttribute = Arrays.asList("setvalue", "option", "property", "postfield", "param");

        JspReferenceProviderVisitor(@NotNull PsiPlainTextFile originalFile) {
            this.originalFile = originalFile;
        }

        @Override
        public void visitElement(@NotNull PsiElement element) {
            if (element instanceof XmlTag) {
                XmlTag xmlTag = (XmlTag) element;
                String tagName = xmlTag.getLocalName();
                if (Constants.Keywords.IMPORT_BEAN_TAG.equals(tagName)) {
                    XmlAttribute bean = xmlTag.getAttribute(Constants.Keywords.BEAN_ATTRIBUTE);
                    if (bean != null) {
                        XmlAttributeValue beanValue = bean.getValueElement();
                        if (beanValue != null) {
                            String beanName = beanValue.getValue();
                            String shortBeanName = beanName.contains("/") ? beanName.substring(beanName.lastIndexOf('/') + 1) : beanName;
                            activeImports.put(shortBeanName, beanName);
                        }
                    }
                }
                if (Constants.Keywords.DROPLET_TAG.equals(tagName)) {
                    XmlAttribute bean = xmlTag.getAttribute(Constants.Keywords.NAME_ATTRIBUTE);
                    if (bean != null) {
                        XmlAttributeValue beanValue = bean.getValueElement();
                        if (beanValue != null) {
                            createdReferences.addAll(AtgComponentReferenceCreator.createReferences(beanValue, originalFile, activeImports));
                        }
                    }
                }
                if (tagsContainingBeanAttribute.contains(tagName)) {
                    XmlAttribute bean = xmlTag.getAttribute(Constants.Keywords.BEAN_ATTRIBUTE);
                    if (bean != null) {
                        XmlAttributeValue beanValue = bean.getValueElement();
                        if (beanValue != null) {
                            createdReferences.addAll(AtgComponentReferenceCreator.createReferences(beanValue, originalFile, activeImports));
                        }
                    }
                }
                if (tagsContainingBeanValueAttribute.contains(tagName)) {
                    XmlAttribute bean = xmlTag.getAttribute(Constants.Keywords.BEAN_VALUE_ATTRIBUTE);
                    if (bean != null) {
                        XmlAttributeValue beanValue = bean.getValueElement();
                        if (beanValue != null) {
                            createdReferences.addAll(AtgComponentReferenceCreator.createReferences(beanValue, originalFile, activeImports));
                        }
                    }
                }
                if (Constants.Keywords.INCLUDE_TAG.equals(tagName)) {
                    XmlAttribute includeFile = xmlTag.getAttribute(Constants.Keywords.PAGE_ATTRIBUTE);
                    if (includeFile != null) {
                        XmlAttributeValue pageValue = includeFile.getValueElement();
                        if (pageValue != null) {
                            TextRange valueTextRange = pageValue.getValueTextRange();
                            createdReferences.add(new JspFileReference(pageValue, originalFile, valueTextRange));
                        }
                    }
                }

                if (Constants.Keywords.IMG_TAG.equals(tagName) || Constants.Keywords.SCRIPT_TAG.equals(tagName)) {
                    XmlAttribute includeFile = xmlTag.getAttribute(Constants.Keywords.SRC_ATTRIBUTE);
                    if (includeFile != null) {
                        XmlAttributeValue pageValue = includeFile.getValueElement();
                        if (pageValue != null) {
                            TextRange valueTextRange = pageValue.getValueTextRange();
                            createdReferences.add(new WebContextResourceReference(pageValue, originalFile, valueTextRange));
                        }
                    }
                }
            }
            super.visitElement(element);
        }

        @NotNull
        public List<PsiReference> getCreatedReferences() {
            return createdReferences;
        }
    }

}