package org.idea.plugin.atg.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.jvm.types.JvmType;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl;
import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import org.apache.commons.lang.StringUtils;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public class ComponentPropertiesCompletionContributor extends CompletionContributor {
    static final String[] AVAILABLE_SCOPES = new String[]{"global", "session", "request", "window", "prototype"};

    public ComponentPropertiesCompletionContributor() {
        extend(CompletionType.BASIC, psiElement(PropertyValueImpl.class), new SuggestDependenciesByClassCompletionProvider());
        extend(CompletionType.BASIC, psiElement(PropertyValueImpl.class), new SuggestReferencedProperty());
        extend(CompletionType.BASIC, psiElement(PropertyKeyImpl.class), new SuggestMissingPropertiesCompletionProvider());
    }

    static class SuggestDependenciesByClassCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters,
                                      ProcessingContext context,
                                      @NotNull CompletionResultSet result) {
            PsiElement position = parameters.getPosition().getContext();
            if (position instanceof PropertyImpl) {
                String key = ((PropertyImpl) position).getKey();
                if (StringUtils.isNotBlank(key) && !key.startsWith("$") && !CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED.equals(key) && !key.endsWith("^")) {
                    PsiClass dependencyClass = AtgComponentUtil.getClassForComponentDependency((PropertyImpl) position);
                    AtgComponentUtil.suggestComponentsByClassWithInheritors(dependencyClass).stream()
                            .map(AtgComponentLookupElement::new)
                            .forEach(result::addElement);
                    result.stopHere();
                } else if (Constants.Keywords.SCOPE_PROPERTY.equals(key)) {
                    for (String scope : AVAILABLE_SCOPES) {
                        result.addElement(LookupElementBuilder.create(scope));
                    }
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
                PsiFile propertyFile = position.getContainingFile();
                Optional<PsiClass> componentClass = AtgComponentUtil.getComponentClass(propertyFile);
                if (!componentClass.isPresent()) return;

                Set<String> existFields = ((PropertiesFile) propertyFile).getProperties().stream()
                        .map(IProperty::getKey)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                AtgComponentUtil.getSettersOfClass(componentClass.get()).stream()
                        .filter(m -> !existFields.contains(AtgComponentUtil.convertSetterNameToVariableName(m.getName())))
                        .map(AtgPropertyLookupElement::new)
                        .forEach(result::addElement);

                if (!existFields.contains(Constants.Keywords.CLASS_PROPERTY))
                    result.addElement(LookupElementBuilder.create(Constants.Keywords.CLASS_PROPERTY));
                result.stopHere();
            }
        }
    }

    static class SuggestReferencedProperty extends CompletionProvider<CompletionParameters> {

        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters,
                                      ProcessingContext context,
                                      @NotNull CompletionResultSet result) {
            PsiElement position = parameters.getPosition().getContext();
            if (position instanceof PropertyImpl) {
                String key = ((PropertyImpl) position).getKey();
                if (key != null && key.endsWith("^")) {
                    String value = ((PropertyImpl) position).getValue();
                    if (value != null && value.contains(".")) {
                        String componentName = value.substring(0, value.indexOf('.'));
                        Module module = ModuleUtilCore.findModuleForPsiElement(position);
                        Collection<PropertiesFileImpl> applicableComponents = AtgComponentUtil.getApplicableComponentsByName(componentName, module, position.getProject());
                        PsiClass variableClass = AtgComponentUtil.getClassForComponentDependency((PropertyImpl) position);
                        if (variableClass != null) {
                            applicableComponents.stream()
                                    .map(AtgComponentUtil::getComponentClass)
                                    .filter(Optional::isPresent)
                                    .map(Optional::get)
                                    .map(AtgComponentUtil::getSettersOfClass)
                                    .flatMap(Collection::stream)
                                    .filter(m -> AtgComponentUtil.getPsiClassForSetterMethod(m) != null)
                                    .filter(m -> variableClass.isEquivalentTo(AtgComponentUtil.getPsiClassForSetterMethod(m)) || AtgComponentUtil.getPsiClassForSetterMethod(m).isInheritorDeep(variableClass, null))

                                    .distinct()
                                    .map(m -> new AtgPropertyLookupElement(m, componentName + "." + AtgComponentUtil.convertSetterToVariableName(m)))
                                    .forEach(result::addElement);
                        } else {
                            JvmType variableType = AtgComponentUtil.getJvmTypeForComponentDependency((PropertyImpl) position);
                            applicableComponents.stream()
                                    .map(AtgComponentUtil::getComponentClass)
                                    .filter(Optional::isPresent)
                                    .map(Optional::get)
                                    .map(AtgComponentUtil::getSettersOfClass)
                                    .flatMap(Collection::stream)
                                    .filter(m -> Objects.equals(AtgComponentUtil.getJvmTypeForSetterMethod(m), variableType))
                                    .distinct()
                                    .map(m -> new AtgPropertyLookupElement(m, componentName + "." + AtgComponentUtil.convertSetterToVariableName(m)))
                                    .forEach(result::addElement);
                        }
                    } else {
                        AtgComponentUtil.getAllComponents(position.getProject()).stream()
                                .map(AtgComponentLookupElement::new)
                                .forEach(result::addElement);
                    }
                    result.stopHere();
                }
            }
        }
    }

}