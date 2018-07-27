/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.idea.plugin.atg.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.util.ProcessingContext;
import org.apache.commons.lang.StringUtils;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.intellij.patterns.PlatformPatterns.psiElement;

/**
 * @author peter
 */
public class ComponentPropertiesCompletionContributor extends CompletionContributor {
    public ComponentPropertiesCompletionContributor() {
        extend(CompletionType.BASIC, psiElement(), new SuggestDependenciesByClassCompletionProvider());
        extend(CompletionType.BASIC, psiElement(), new SuggestMissingPropertiesCompletionProvider());
    }

    static class SuggestDependenciesByClassCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters,
                                      ProcessingContext context,
                                      @NotNull CompletionResultSet result) {
            PsiElement position = parameters.getPosition().getContext();
            if (position instanceof PropertyImpl) {
                String key = ((PropertyImpl) position).getKey();
                if (StringUtils.isNotBlank(key) && !key.startsWith("$") && !CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED.equals(key)) {
                    PsiClass dependencyClass = AtgComponentUtil.getClassForComponentDependency((PropertyImpl) position);
                    AtgComponentUtil.suggestComponentsByClassWithInheritors(dependencyClass).stream()
                            .map(AtgComponentUtil::getComponentCanonicalName)
                            .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                            .map(LookupElementBuilder::create)
                            .forEach(result::addElement);
                    result.stopHere();
                }
            }

        }
    }

    static class SuggestMissingPropertiesCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters,
                                      ProcessingContext context,
                                      @NotNull CompletionResultSet result) {
            PsiElement position = parameters.getPosition().getContext();
            if (position instanceof PropertyImpl) {
                String key = ((PropertyImpl) position).getKey();
                if (CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED.equals(key)) {
                    PsiFile propertyFile = position.getContainingFile();
                    Optional<PsiClass> componentClass = AtgComponentUtil.getComponentClass(propertyFile);
                    if (!componentClass.isPresent()) return;

                    Set<String> existFields = ((PropertiesFile) propertyFile).getProperties().stream()
                            .map(IProperty::getKey)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());

                    AtgComponentUtil.getSettersOfClass(componentClass.get()).stream()
                            .map(PsiMethod::getName)
                            .sorted()
                            .map(AtgComponentUtil::convertSetterNameToVariableName)
                            .filter(var -> !existFields.contains(var))
                            .map(LookupElementBuilder::create)
                            .forEach(result::addElement);

                    if (!existFields.contains("$class")) result.addElement(LookupElementBuilder.create("$class"));
                }
            }
//        result.stopHere();
        }
    }

}