package org.idea.plugin.atg.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.concurrency.JobLauncher;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressWrapper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.index.AtgIndexService;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class IdenticalPropertyValuesInspection extends LocalInspectionTool {

    private static final String INSPECTION_DUPLICATED_PROPERTY_KEY = "inspection.identicallyProperty.text";

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {

                if (element instanceof PropertiesFileImpl) {
                    PropertiesFileImpl propertiesFile = (PropertiesFileImpl) element;
                    final List<IProperty> properties = propertiesFile.getProperties();
                    List<PropertiesFileImpl> propertiesFiles = getComponentPropertiesFiles(propertiesFile, propertiesFile.getProject());
                    Module currentModule = ModuleUtilCore.findModuleForPsiElement(propertiesFile);

                    final Map<String, Set<PsiFile>> processedKeyToFiles = Collections.synchronizedMap(new HashMap<>());
                    final ProgressIndicator original = ProgressManager.getInstance().getProgressIndicator();
                    final ProgressIndicator progress = ProgressWrapper.wrap(original);
                    ProgressManager.getInstance().runProcess(() -> {
                        if (!JobLauncher.getInstance().invokeConcurrentlyUnderProgress(properties, progress, property -> {
                            processKeyUsages(property, processedKeyToFiles, propertiesFiles, currentModule);
                            return true;
                        })) throw new ProcessCanceledException();
                    }, progress);

                    processedKeyToFiles.keySet().stream()
                            .map(key -> (PropertyImpl)propertiesFile.findPropertyByKey(key))
                            .filter(Objects::nonNull)
                            .forEach(property -> holder.registerProblem(property, AtgToolkitBundle.message(INSPECTION_DUPLICATED_PROPERTY_KEY, property.getText()),
                                        ProblemHighlightType.WEAK_WARNING, TextRange.allOf(property.getText()))
                            );
                }
            }
        };
    }

    private void processKeyUsages(final IProperty property,
                                  final Map<String, Set<PsiFile>> processedKeyToFiles,
                                  final List<PropertiesFileImpl> propertiesFiles,
                                  Module currentModule) {
        String propertyKey = property.getKey();
        String propertyValue = property.getValue();
        if (!processedKeyToFiles.containsKey(propertyKey) && StringUtils.isNotBlank(propertyKey) && StringUtils.isNotBlank(propertyValue)) {
            final Set<PsiFile> resultFiles = propertiesFiles.stream()
                    .filter(propertiesFile -> hasEqualKeyValue(propertyKey, propertyValue, propertiesFile) && isDependsOnModule(currentModule, propertiesFile))
                    .collect(Collectors.toSet());
            if (CollectionUtils.isNotEmpty(resultFiles)) {
                processedKeyToFiles.put(propertyKey, resultFiles);
            }
        }
    }

    @NotNull
    private List<PropertiesFileImpl> getComponentPropertiesFiles(@NotNull PropertiesFileImpl propertiesFile,
                                                                 @NotNull Project project) {
        AtgIndexService indexService = ServiceManager.getService(project, AtgIndexService.class);
        Optional<String> componentName = AtgComponentUtil.getComponentCanonicalName(propertiesFile);
        List<PropertiesFileImpl> propertiesFiles = componentName
                .map(indexService::getComponentsByName)
                .orElse(Collections.emptyList());
        propertiesFiles.remove(propertiesFile);
        return propertiesFiles;
    }

    private boolean isDependsOnModule(Module currentModule, PropertiesFileImpl propertiesFile) {
        Module module = ModuleUtilCore.findModuleForPsiElement(propertiesFile);
        return module != null && !ModuleRootManager.getInstance(module).isDependsOn(currentModule);
    }

    private boolean hasEqualKeyValue(String propertyKey, String propertyValue, PropertiesFileImpl propertiesFile) {
        IProperty fileProperty = propertiesFile.findPropertyByKey(propertyKey);
        return fileProperty != null && propertyValue.equals(fileProperty.getValue());
    }
}