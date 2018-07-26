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
import com.intellij.lang.jvm.types.JvmType;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PostprocessReformattingAspect;
import com.intellij.util.IncorrectOperationException;
import org.idea.plugin.atg.util.AtgComponentUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Predicate;

public class PropertiesGenerator {

    private PropertiesGenerator() {
    }

    public static void generatePropertiesFile(final PsiClass srcClass) {
        Project project = srcClass.getProject();
        PsiDirectory srcDir = srcClass.getContainingFile().getContainingDirectory();
        PsiPackage srcPackage = JavaDirectoryService.getInstance().getPackage(srcDir);
        Module module = ModuleUtilCore.findModuleForFile(srcClass.getContainingFile());
        if (module == null || srcPackage == null) return;

        CommandProcessor.getInstance().executeCommand(project, () -> DumbService.getInstance(project).withAlternativeResolveEnabled(() ->
                PropertiesGenerator.generatePropertiesFile(project, module, srcClass, srcPackage)), AtgToolkitBundle.message("intentions.create.component"), null);

    }

    @Nullable
    public static PsiElement generatePropertiesFile(final Project project,
                                                    final Module module,
                                                    final PsiClass srcClass,
                                                    final PsiPackage srcPackage) {
        return PostprocessReformattingAspect.getInstance(project).postponeFormattingInside(
                () -> ApplicationManager.getApplication().runWriteAction((Computable<PsiElement>) () -> {
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
                        Notification notification = new Notification(AtgToolkitBundle.message("notifications.groupId"),
                                AtgToolkitBundle.message("intentions.create.component.error"),
                                e.getMessage(), NotificationType.INFORMATION);
                        Notifications.Bus.notify(notification, project);
                    } catch (Exception e) {
                        Notification notification = new Notification(AtgToolkitBundle.message("notifications.groupId"),
                                AtgToolkitBundle.message("intentions.create.component.error"),
                                e.getMessage(), NotificationType.ERROR);
                        Notifications.Bus.notify(notification, project);
                    }
                    return targetClass;
                }));
    }

    private static PsiFile createPropertyFileFromTemplate(final FileTemplateDescriptor fileTemplateDescriptor,
                                                          final PsiDirectory targetDirectory,
                                                          final Project project,
                                                          final PsiClass srcClass) throws Exception {
        String templateName = fileTemplateDescriptor.getFileName();
        FileTemplate fileTemplate = FileTemplateManager.getInstance(project).getCodeTemplate(templateName);
        fileTemplate.setReformatCode(false);

        Predicate<PsiMethod> treatAsDependencySetter = m -> {
            JvmType parameterType = m.getParameters()[0].getType();
            if (!(parameterType instanceof PsiClassType)) return false;
            String parameterClassName = ((PsiClassType) parameterType).getCanonicalText();
            if (parameterClassName.startsWith("java")) return false;
            if (parameterClassName.equals("atg.xml.XMLFile")) return false;
            if (parameterClassName.equals("atg.repository.rql.RqlStatement")) return false;
            if (parameterClassName.equals("atg.nucleus.ResolvingMap")) return false;
            return !parameterClassName.equals("atg.nucleus.ServiceMap");
        };

        Function<PsiMethod, String> populatePropertiesWithSuggestedComponents = psiMethod -> {
            String variableName = AtgComponentUtil.convertSetterToVariableName(psiMethod);
            String dependencyClassName = AtgComponentUtil.getClassNameForSetterMethod(psiMethod);
            List<String> possibleComponents = AtgComponentUtil.suggestComponentsByClassName(dependencyClassName, psiMethod.getProject());
            if (possibleComponents.size() == 1) {
                return variableName + "=" + possibleComponents.get(0);
            }
            return variableName + "=";
        };

        List<PsiMethod> allSetters = AtgComponentUtil.getSettersOfClass(srcClass);

        String dependencies = allSetters.stream()
                .filter(treatAsDependencySetter)
                .map(populatePropertiesWithSuggestedComponents)
                .sorted()
                .reduce((a, b) -> a + "\n" + b).orElse("");

        String variables = allSetters.stream()
                .filter(treatAsDependencySetter.negate())
                .map(populatePropertiesWithSuggestedComponents)
                .sorted()
                .reduce((a, b) -> a + "\n" + b).orElse("");

        Properties defaultProperties = FileTemplateManager.getInstance(project).getDefaultProperties();
        Properties properties = new Properties(defaultProperties);
        properties.setProperty(FileTemplate.ATTRIBUTE_CLASS_NAME, srcClass.getQualifiedName());
        properties.setProperty("DEPENDENCIES", "".equals(dependencies) ? "" : "\n" + dependencies + "\n");
        properties.setProperty("VARS", variables);

        PsiElement psiElement = FileTemplateUtil.createFromTemplate(fileTemplate, srcClass.getName(), properties, targetDirectory);
        if (psiElement instanceof PsiFile) {
            return (PsiFile) psiElement;
        }
        return null;
    }

}