package org.idea.plugin.atg.psi.reference.contribution;

import com.intellij.patterns.StandardPatterns;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ProcessingContext;
import com.jgoodies.common.base.Strings;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.Constants.Keywords.Repository;
import org.idea.plugin.atg.psi.provider.XmlAttributeComponentNamesProvider;
import org.idea.plugin.atg.psi.provider.XmlAttributeJavaClassProvider;
import org.idea.plugin.atg.psi.reference.ItemDescriptorReference;
import org.idea.plugin.atg.psi.reference.JavaPropertyReference;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RepositoryXmlContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(XmlPatterns.xmlAttributeValue()
                        .withParent(XmlPatterns.xmlAttribute().withName(Repository.VALUE, Repository.BEAN)
                                .withParent(XmlPatterns.xmlTag().withName(Repository.ATTRIBUTE)
                                        .withParent(XmlPatterns.xmlTag().withName(Repository.PROPERTY)
                                                .andOr(XmlPatterns.xmlTag().withParent(XmlPatterns.xmlTag().withName(Repository.ITEM_DESCRIPTOR)),
                                                        XmlPatterns.xmlTag().withSuperParent(2, XmlPatterns.xmlTag().withName(Repository.ITEM_DESCRIPTOR)))
                                        )))
                        .and(XmlPatterns.xmlAttributeValue().withValue(StandardPatterns.string().startsWith("/"))),
                new XmlAttributeComponentNamesProvider());

        registrar.registerReferenceProvider(XmlPatterns.xmlAttributeValue()
                        .withParent(XmlPatterns.xmlAttribute().withName(Repository.REPOSITORY)
                                .withParent(XmlPatterns.xmlTag().withName(Repository.PROPERTY)
                                        .andOr(XmlPatterns.xmlTag().withParent(XmlPatterns.xmlTag().withName(Repository.ITEM_DESCRIPTOR)),
                                                XmlPatterns.xmlTag().withSuperParent(2, XmlPatterns.xmlTag().withName(Repository.ITEM_DESCRIPTOR)))
                                ))
                        .and(XmlPatterns.xmlAttributeValue().withValue(StandardPatterns.string().startsWith("/"))),
                new XmlAttributeComponentNamesProvider());

        registrar.registerReferenceProvider(XmlPatterns.xmlAttributeValue()
                        .withParent(XmlPatterns.xmlAttribute().withName(Repository.PROPERTY_TYPE, Repository.EDITOR_CLASS)
                                .withParent(XmlPatterns.xmlTag().withName(Repository.PROPERTY)
                                        .andOr(XmlPatterns.xmlTag().withParent(XmlPatterns.xmlTag().withName(Repository.ITEM_DESCRIPTOR)),
                                                XmlPatterns.xmlTag().withSuperParent(2, XmlPatterns.xmlTag().withName(Repository.ITEM_DESCRIPTOR))))),
                new XmlAttributeJavaClassProvider());

        registrar.registerReferenceProvider(XmlPatterns.xmlAttributeValue()
                        .withParent(XmlPatterns.xmlAttribute().withName(Repository.NAME)
                                .withParent(XmlPatterns.xmlTag().withName(Repository.ATTRIBUTE)
                                        .withParent(XmlPatterns.xmlTag().withName(Repository.PROPERTY)
                                                .andOr(XmlPatterns.xmlTag().withParent(XmlPatterns.xmlTag().withName(Repository.ITEM_DESCRIPTOR)),
                                                        XmlPatterns.xmlTag().withSuperParent(2, XmlPatterns.xmlTag().withName(Repository.ITEM_DESCRIPTOR)))))),
                new PropertyDescriptorAttributesProvider());

        registrar.registerReferenceProvider(XmlPatterns.xmlAttributeValue()
                        .withParent(XmlPatterns.xmlAttribute().withName(Repository.NAME, Repository.SUPER_TYPE)
                                .withParent(XmlPatterns.xmlTag().withName(Repository.ITEM_DESCRIPTOR))),
                new ItemDescriptorReferenceProvider());

        registrar.registerReferenceProvider(XmlPatterns.xmlAttributeValue()
                        .withParent(XmlPatterns.xmlAttribute().withName(Repository.COMPONENT_ITEM_TYPE, Repository.ITEM_TYPE)
                                .withParent(XmlPatterns.xmlTag().withName(Repository.PROPERTY)
                                        .andOr(XmlPatterns.xmlTag().withParent(XmlPatterns.xmlTag().withName(Repository.ITEM_DESCRIPTOR)),
                                                XmlPatterns.xmlTag().withSuperParent(2, XmlPatterns.xmlTag().withName(Repository.ITEM_DESCRIPTOR))))),
                new ItemDescriptorReferenceProvider());
        //TODO Consider Repository sibling tag for item-type resolve
        //TODO inspection whether item desc is present there
    }

    static class ItemDescriptorReferenceProvider extends PsiReferenceProvider {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                     @NotNull ProcessingContext
                                                             context) {
            List<PsiReference> results = new ArrayList<>();
            XmlAttributeValue valueElement = (XmlAttributeValue) element;
            if (Strings.isNotBlank(valueElement.getValue())) {
                results.add(new ItemDescriptorReference(valueElement));
            }
            return results.toArray(PsiReference.EMPTY_ARRAY);
        }
    }

    static class PropertyDescriptorAttributesProvider extends PsiReferenceProvider {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                     @NotNull ProcessingContext
                                                             context) {
            List<PsiReference> results = new ArrayList<>();
            XmlAttributeValue valueElement = (XmlAttributeValue) element;
            if (Strings.isNotBlank(valueElement.getValue()) && !Constants.IGNORED_ATTRIBUTES_NAMES_FOR_DESCRIPTOR.contains(valueElement.getValue())) {
                XmlTag propertyTag = (XmlTag) element.getParent().getParent().getParent();
                if (propertyTag != null) {
                    String propertyTagAttributeValue = propertyTag.getAttributeValue(Repository.PROPERTY_TYPE);
                    String propertyDescriptorClass = propertyTagAttributeValue != null
                            ? propertyTagAttributeValue
                            : Constants.DEFAULT_ITEM_DESCRIPTOR_CLASS;
                    GlobalSearchScope scope = GlobalSearchScope.allScope(element.getProject());
                    PsiClass psiClass = JavaPsiFacade.getInstance(element.getProject()).findClass(propertyDescriptorClass, scope);
                    if (psiClass != null) {
                        results.add(new JavaPropertyReference(valueElement, psiClass));
                    }
                }
            }
            return results.toArray(PsiReference.EMPTY_ARRAY);
        }
    }
}
