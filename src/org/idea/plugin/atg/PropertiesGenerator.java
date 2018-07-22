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

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Properties;

public class PropertiesGenerator {

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
        Properties defaultProperties = FileTemplateManager.getInstance(project).getDefaultProperties();
        Properties properties = new Properties(defaultProperties);
        properties.setProperty(FileTemplate.ATTRIBUTE_CLASS_NAME, srcClass.getQualifiedName());

        String setters = Arrays.stream(srcClass.getAllMethods())
                .filter(m -> m.hasModifier(JvmModifier.PUBLIC))
                .map(PsiMethod::getName)
                .filter(name -> name.startsWith("set"))
                .map(name -> name.substring(3, 4).toLowerCase() + name.substring(4) + "=" + LineSeparator.getSystemLineSeparator().getSeparatorString())
                .reduce((a, b) -> a + b).orElse("");
        properties.setProperty("SETTERS", setters);

        PsiElement psiElement = FileTemplateUtil.createFromTemplate(fileTemplate, srcClass.getName(), properties, targetDirectory);
        if (psiElement instanceof PsiFile) {
            return (PsiFile) psiElement;
        }
        return null;
    }

}