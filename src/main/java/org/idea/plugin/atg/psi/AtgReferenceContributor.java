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
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AtgReferenceContributor extends PsiReferenceContributor {
    private static final Pattern COMPONENT_NAME_REGEX = Pattern.compile("/[^,=]*");

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(PropertyValueImpl.class),
                new PsiReferenceProvider() {
                    @NotNull
                    @Override
                    public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                                 @NotNull ProcessingContext
                                                                         context) {
                        PropertyValueImpl propertyValue = (PropertyValueImpl) element;
                        String value = propertyValue.getText();
                        if (!CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED.equals(value)) {
                            List<PsiReference> results = new ArrayList<>();
                            Matcher matcher = COMPONENT_NAME_REGEX.matcher(value);
                            while (matcher.find()) {
                                int start = matcher.start(0);
                                int length = matcher.group(0).length();
                                results.add(new AtgComponentReference(element, new TextRange(start, start + length)));
                            }
                            return results.toArray(new PsiReference[0]);

                        }
                        return PsiReference.EMPTY_ARRAY;
                    }
                });

        registrar.registerReferenceProvider(PlatformPatterns.psiElement(PropertyValueImpl.class),
                new PsiReferenceProvider() {
                    @NotNull
                    @Override
                    public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                                 @NotNull ProcessingContext
                                                                         context) {
                        PropertyValueImpl property = (PropertyValueImpl) element;
                        String key = ((PropertyImpl) property.getParent()).getKey();
                        String value = property.getText();
                        if ("$class".equals(key) && !CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED.equals(value)) {
                            JavaClassListReferenceProvider javaClassListReferenceProvider = new JavaClassListReferenceProvider();
                            return javaClassListReferenceProvider.getReferencesByString(value, element, 0);
                        }
                        return PsiReference.EMPTY_ARRAY;
                    }
                });

        registrar.registerReferenceProvider(PlatformPatterns.psiElement(PropertyKeyImpl.class),
                new PsiReferenceProvider() {
                    @NotNull
                    @Override
                    public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                                 @NotNull ProcessingContext
                                                                         context) {
                        PropertyKeyImpl property = (PropertyKeyImpl) element;
                        String key = property.getText();
                        if (!CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED.equals(key) && !key.startsWith("$")) {
                            PropertiesFileImpl properties = PsiTreeUtil.getTopmostParentOfType(element, PropertiesFileImpl.class);
                            PsiClass componentClass = AtgComponentUtil.getComponentClass(properties);
                            if (componentClass != null) {
                                return new PsiReference[]{
                                        new JavaPropertyReference(element, componentClass, new TextRange(0, key.length()))
                                };
                            }
                        }
                        return PsiReference.EMPTY_ARRAY;
                    }
                });
    }
}
