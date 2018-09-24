package org.idea.plugin.atg.psi;

import com.intellij.codeInsight.completion.CompletionUtilCore;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl;
import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassListReferenceProvider;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.psi.reference.AtgComponentFieldReference;
import org.idea.plugin.atg.psi.reference.AtgComponentReference;
import org.idea.plugin.atg.psi.reference.JavaPropertyReference;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

public class AtgReferenceContributor extends PsiReferenceContributor {
    //TODO replace filters

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(PropertyValueImpl.class),
                new ComponentByPropertyValueProvider());

        //TODO General
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(PropertyValueImpl.class),
                new JavaClassByPropertyValueProvider());

        registrar.registerReferenceProvider(PlatformPatterns.psiElement(PropertyKeyImpl.class),
                new NucleusDefaultPropertiesProvider());
    }

    static class NucleusDefaultPropertiesProvider extends PsiReferenceProvider {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                     @NotNull ProcessingContext
                                                             context) {
            PropertiesFileImpl propertiesFile = PsiTreeUtil.getTopmostParentOfType(element, PropertiesFileImpl.class);
            if (propertiesFile != null && AtgComponentUtil.getComponentCanonicalName(propertiesFile).isPresent()) {
                PropertyKeyImpl propertyKey = (PropertyKeyImpl) element;
                String key = propertyKey.getText();
                if (!CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED.equals(key) && !key.startsWith("$")) {
                    PropertiesFileImpl properties = PsiTreeUtil.getTopmostParentOfType(element, PropertiesFileImpl.class);
                    Optional<PsiClass> componentClass = AtgComponentUtil.getComponentClass(properties);
                    if (componentClass.isPresent()) {
                        int offsetForRange = 0;
                        if (key.endsWith("^") || key.endsWith("+") || key.endsWith("-")) {
                            offsetForRange++;
                        }
                        return new PsiReference[]{
                                new JavaPropertyReference(propertyKey, componentClass.get(), new TextRange(0, key.length() - offsetForRange))
                        };
                    }
                }
            }
            return PsiReference.EMPTY_ARRAY;
        }
    }

    static class JavaClassByPropertyValueProvider extends PsiReferenceProvider {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                     @NotNull ProcessingContext
                                                             context) {
            PropertiesFileImpl propertiesFile = PsiTreeUtil.getTopmostParentOfType(element, PropertiesFileImpl.class);
            if (propertiesFile != null && AtgComponentUtil.getComponentCanonicalName(propertiesFile).isPresent()) {
                PropertyValueImpl property = (PropertyValueImpl) element;
                String key = ((PropertyImpl) property.getParent()).getKey();
                String value = property.getText();
                if (Constants.Keywords.Properties.CLASS_PROPERTY.equals(key) && !CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED.equals(value)) {
                    JavaClassListReferenceProvider javaClassListReferenceProvider = new JavaClassListReferenceProvider();
                    return javaClassListReferenceProvider.getReferencesByString(value, element, 0);
                }
            }
            return PsiReference.EMPTY_ARRAY;
        }
    }

    static class ComponentByPropertyValueProvider extends PsiReferenceProvider {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                     @NotNull ProcessingContext
                                                             context) {
            PropertiesFileImpl propertiesFile = PsiTreeUtil.getTopmostParentOfType(element, PropertiesFileImpl.class);
            if (propertiesFile != null && AtgComponentUtil.getComponentCanonicalName(propertiesFile).isPresent()) {
                PropertyValueImpl propertyValue = (PropertyValueImpl) element;
                String value = propertyValue.getText();
                if (!CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED.equals(value)) {
                    List<PsiReference> results = new ArrayList<>();
                    Matcher matcher = Constants.SUSPECTED_COMPONENT_NAME_REGEX.matcher(value);
                    while (matcher.find()) {
                        String beanName = matcher.group(0);
                        int start = matcher.start(0);
                        int length = beanName.length();
                        TextRange beanTextRange = new TextRange(start, start + length);
                        if (beanName.endsWith(".xml")) {
                            results.add(new AtgComponentReference(propertyValue, beanTextRange));
                        } else if (beanName.contains(".")) {
                            String firstField = beanName.substring(beanName.indexOf('.') + 1);
                            beanName = beanName.substring(0, beanName.indexOf('.'));
                            length = beanName.length();
                            beanTextRange = new TextRange(start, start + length);
                            TextRange fieldTextRange = new TextRange(start + length + 1, start + length + firstField.length() + 1);
                            results.add(new AtgComponentReference(propertyValue, beanTextRange));
                            results.add(new AtgComponentFieldReference(beanName, firstField, fieldTextRange, element.getContainingFile()));
                        } else {
                            results.add(new AtgComponentReference(propertyValue, beanTextRange));
                        }
                    }
                    return results.toArray(new PsiReference[0]);

                }
            }
            return PsiReference.EMPTY_ARRAY;
        }
    }
}
