package org.idea.plugin.atg;

import com.intellij.facet.FacetManager;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.ide.util.EditorHelper;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PostprocessReformattingAspect;
import com.intellij.util.IncorrectOperationException;
import org.idea.plugin.atg.config.AtgConfigHelper;
import org.idea.plugin.atg.module.AtgModuleFacet;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PropertiesGenerator {

    private PropertiesGenerator() {
    }

    public static void generatePropertiesFile(@NotNull final PsiClass srcClass) {
        Project project = srcClass.getProject();
        PsiDirectory srcDir = srcClass.getContainingFile().getContainingDirectory();
        PsiPackage srcPackage = JavaDirectoryService.getInstance().getPackage(srcDir);
        Module module = ModuleUtilCore.findModuleForFile(srcClass.getContainingFile());

        if (module == null) {
            new Notification(Constants.NOTIFICATION_GROUP_ID,
                    AtgToolkitBundle.message("intentions.create.component"),
                    AtgToolkitBundle.message("intentions.create.component.moduleNotFound"),
                    NotificationType.WARNING).notify(project);
            return;
        }

        if (srcPackage == null) {
            new Notification(Constants.NOTIFICATION_GROUP_ID,
                    AtgToolkitBundle.message("intentions.create.component"),
                    AtgToolkitBundle.message("intentions.create.component.packageNotFound"),
                    NotificationType.WARNING).notify(project);
            return;
        }

        AtgModuleFacet atgFacet = FacetManager.getInstance(module).getFacetByType(Constants.FACET_TYPE_ID);
        if (atgFacet == null) {
            new Notification(Constants.NOTIFICATION_GROUP_ID,
                    AtgToolkitBundle.message("intentions.create.component"),
                    AtgToolkitBundle.message("intentions.create.component.moduleNotAtg", module.getName()),
                    NotificationType.WARNING).notify(project);
            return;
        }

        PsiDirectory targetDirectory = AtgConfigHelper.getComponentConfigPsiDirectory(atgFacet, srcPackage);
        if (targetDirectory == null) {
            new Notification(Constants.NOTIFICATION_GROUP_ID,
                    AtgToolkitBundle.message("intentions.create.component"),
                    AtgToolkitBundle.message("intentions.create.component.moduleHasNoRoots", module.getName()),
                    NotificationType.WARNING).notify(project);
            return;
        }

        CommandProcessor.getInstance().executeCommand(project, () -> DumbService.getInstance(project).withAlternativeResolveEnabled(() ->
                PropertiesGenerator.generatePropertiesFile(project, targetDirectory, srcClass)), AtgToolkitBundle.message("intentions.create.component"), Constants.NOTIFICATION_GROUP_ID);

    }

    private static void generatePropertiesFile(@NotNull final Project project,
                                              @NotNull final PsiDirectory targetDirectory,
                                              @NotNull final PsiClass srcClass) {
        PostprocessReformattingAspect.getInstance(project).postponeFormattingInside(
                () -> ApplicationManager.getApplication().runWriteAction(() -> {
                    try {
                        FileTemplateDescriptor fileTemplateDescriptor = new FileTemplateDescriptor("ATG Properties.properties");
                        PsiFile targetClass = createPropertyFileFromTemplate(fileTemplateDescriptor, targetDirectory, project, srcClass);

                        if (targetClass != null) {
                            IdeDocumentHistory.getInstance(project).includeCurrentPlaceAsChangePlace();
                            EditorHelper.openInEditor(targetClass, false);
                        }
                    } catch (Exception e) {
                        new Notification(Constants.NOTIFICATION_GROUP_ID, AtgToolkitBundle.message("intentions.create.component.error"),
                                e.getMessage(), NotificationType.ERROR).notify(project);
                    }
                }));
    }

    @Nullable
    private static PsiFile createPropertyFileFromTemplate(@NotNull final FileTemplateDescriptor fileTemplateDescriptor,
                                                          @NotNull final PsiDirectory targetDirectory,
                                                          @NotNull final Project project,
                                                          @NotNull final PsiClass srcClass) throws Exception {
        String templateName = fileTemplateDescriptor.getFileName();
        FileTemplate fileTemplate = FileTemplateManager.getInstance(project).getCodeTemplate(templateName);
        fileTemplate.setReformatCode(false);


        Function<PsiMethod, String> populatePropertiesWithSuggestedComponents = psiMethod -> {
            String variableName = AtgComponentUtil.convertSetterToVariableName(psiMethod);
            PsiClass dependencyClass = AtgComponentUtil.getPsiClassForSetterMethod(psiMethod);
            Collection<String> possibleComponents = AtgComponentUtil.   suggestComponentsByClassWithInheritors(dependencyClass).stream()
                    .map(AtgComponentUtil::getComponentCanonicalName)
                    .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                    .distinct()
                    .collect(Collectors.toList());
            if (possibleComponents.size() == 1) {
                return variableName + "=" + possibleComponents.iterator().next();
            }
            return variableName + "=";
        };

        List<PsiMethod> allSetters = AtgComponentUtil.getSettersOfClass(srcClass);

        String dependencies = allSetters.stream()
                .filter(AtgComponentUtil::treatAsDependencySetter)
                .map(populatePropertiesWithSuggestedComponents)
                .sorted()
                .reduce((a, b) -> a + "\n" + b).orElse("");

        String variables = allSetters.stream()
                .filter(p -> !AtgComponentUtil.treatAsDependencySetter(p))
                .map(populatePropertiesWithSuggestedComponents)
                .sorted()
                .reduce((a, b) -> a + "\n" + b).orElse("");

        Properties defaultProperties = FileTemplateManager.getInstance(project).getDefaultProperties();
        Properties properties = new Properties(defaultProperties);
        properties.setProperty(FileTemplate.ATTRIBUTE_CLASS_NAME, srcClass.getQualifiedName());
        properties.setProperty("DEPENDENCIES", "".equals(dependencies) ? "" : "\n" + dependencies + "\n");
        properties.setProperty("VARS", variables);

        PsiElement psiElement;
        try {
            psiElement = FileTemplateUtil.createFromTemplate(fileTemplate, srcClass.getName(), properties, targetDirectory);
        } catch (IncorrectOperationException e) {
            new Notification(Constants.NOTIFICATION_GROUP_ID, AtgToolkitBundle.message("intentions.create.component.exist"),
                    e.getMessage(), NotificationType.INFORMATION).notify(project);

            VirtualFile existVirtualFile = srcClass.getName() != null ? targetDirectory.getVirtualFile().findFileByRelativePath(srcClass.getName() + ".properties") : null;
            psiElement = existVirtualFile != null ? PsiManager.getInstance(project).findFile(existVirtualFile) : null;
        }

        if (psiElement instanceof PsiFile) {
            return (PsiFile) psiElement;
        }
        return null;
    }

}