/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.idea.plugin.atg;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.ide.util.EditorHelper;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.PropertiesImplUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PostprocessReformattingAspect;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.LineSeparator;
import org.apache.commons.lang.StringUtils;
import org.idea.plugin.atg.config.AtgToolkitConfig;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PropertiesGenerator {

    private PropertiesGenerator() {
    }

    @Nullable
    public static PsiElement generatePropertiesFile(final Project project,
                                                    final Module module,
                                                    final PsiClass srcClass,
                                                    final PsiPackage srcPackage) {
        return PostprocessReformattingAspect.getInstance(project).postponeFormattingInside(
                () -> ApplicationManager.getApplication().runWriteAction(new Computable<PsiElement>() {
                    public PsiElement compute() {
                        PsiFile targetClass = null;
                        try {
                            PsiDirectory targetDirectory = AtgConfigHelper.getComponentConfigPsiDirectory(module, srcPackage);

                            FileTemplateDescriptor fileTemplateDescriptor = new FileTemplateDescriptor("ATG Properties.properties");
                            targetClass = createPropertyFileFromTemplate(fileTemplateDescriptor, targetDirectory, project, srcClass);

                            if (targetClass != null) {
                                IdeDocumentHistory.getInstance(project).includeCurrentPlaceAsChangePlace();
                                EditorHelper.openInEditor(targetClass, false);
                            }

                        } catch (IncorrectOperationException e) {
                            Notification notification = new Notification(AtgModuleBundle.message("notifications.groupId"),
                                    AtgModuleBundle.message("intentions.create.component.error"),
                                    e.getMessage(), NotificationType.INFORMATION);
                            Notifications.Bus.notify(notification, project);
                        } catch (Exception e) {
                            Notification notification = new Notification(AtgModuleBundle.message("notifications.groupId"),
                                    AtgModuleBundle.message("intentions.create.component.error"),
                                    e.getMessage(), NotificationType.ERROR);
                            Notifications.Bus.notify(notification, project);
                        }
                        return targetClass;
                    }
                }));
    }

    private static PsiFile createPropertyFileFromTemplate(final FileTemplateDescriptor fileTemplateDescriptor,
                                                          final PsiDirectory targetDirectory,
                                                          final Project project,
                                                          final PsiClass srcClass) throws Exception {
        String templateName = fileTemplateDescriptor.getFileName();
        FileTemplate fileTemplate = FileTemplateManager.getInstance(project).getCodeTemplate(templateName);
        fileTemplate.setReformatCode(false);

        AtgToolkitConfig atgToolkitConfig = AtgToolkitConfig.getInstance(project);
        String ignoredClassesForSetters = atgToolkitConfig != null ? atgToolkitConfig.getIgnoredClassesForSetters() : "";
        String[] ignoredClassesForSettersArray = ignoredClassesForSetters
                .replace(".", "\\.")
                .replace("?", ".?")
                .replace("*", ".*?")
                .split("[,;]");
        List<Pattern> ignoredClassPatterns = Stream.of(ignoredClassesForSettersArray).map(Pattern::compile).collect(Collectors.toList());

        Predicate<PsiMethod> isNotIgnoredMethod = psiMethod -> {
            for (Pattern classNamePattern : ignoredClassPatterns) {
                PsiClass containingClass = psiMethod.getContainingClass();
                String className = containingClass != null ? containingClass.getQualifiedName() : "";
                if (StringUtils.isNotBlank(className) && classNamePattern.matcher(className).matches()) return false;
            }
            return true;
        };

        Comparator<PsiMethod> methodComparatorServicesFirst = (o1, o2) -> {
            boolean isFirstSimple = o1.getParameters().length > 0
                    && (o1.getParameters()[0].getType() instanceof PsiPrimitiveType
                    || ((PsiClassType) o1.getParameters()[0].getType()).getCanonicalText().startsWith("java."));
            boolean isSecondSimple = o2.getParameters().length > 0
                    && (o2.getParameters()[0].getType() instanceof PsiPrimitiveType
                    || ((PsiClassType) o2.getParameters()[0].getType()).getCanonicalText().startsWith("java."));

            if (isFirstSimple && !isSecondSimple) return 1;
            if (!isFirstSimple && isSecondSimple) return -1;

            return o1.getName().compareTo(o2.getName());
        };

        Function<String, String> populateInjectionsWithUnambiguousValues = name -> {
            if (atgToolkitConfig != null && atgToolkitConfig.isInjectUnambiguousProperties()) {
                Set<String> foundValues = PropertiesImplUtil.findPropertiesByKey(project, name).stream().map(IProperty::getValue).collect(Collectors.toSet());
                if (foundValues.size() == 1)
                    return name + "=" + foundValues.iterator().next() + LineSeparator.getSystemLineSeparator().getSeparatorString();
            }

            return name + "=" + LineSeparator.getSystemLineSeparator().getSeparatorString();
        };

        String injections = Arrays.stream(srcClass.getAllMethods())
                .filter(m -> m.hasModifier(JvmModifier.PUBLIC))
                .filter(method -> method.getName().startsWith("set"))
                .filter(isNotIgnoredMethod)
                .sorted(methodComparatorServicesFirst)
                .map(PsiMethod::getName)
                .map(name -> name.substring(3, 4).toLowerCase() + name.substring(4))
                .map(populateInjectionsWithUnambiguousValues)
                .reduce((a, b) -> a + b).orElse("");

        Properties defaultProperties = FileTemplateManager.getInstance(project).getDefaultProperties();
        Properties properties = new Properties(defaultProperties);
        properties.setProperty(FileTemplate.ATTRIBUTE_CLASS_NAME, srcClass.getQualifiedName());
        properties.setProperty("SETTERS", injections);

        PsiElement psiElement = FileTemplateUtil.createFromTemplate(fileTemplate, srcClass.getName(), properties, targetDirectory);
        if (psiElement instanceof PsiFile) {
            return (PsiFile) psiElement;
        }
        return null;
    }

}