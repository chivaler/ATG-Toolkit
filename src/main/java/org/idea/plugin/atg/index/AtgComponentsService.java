package org.idea.plugin.atg.index;

import com.google.common.collect.Sets;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AtgComponentsService {
    @NotNull
    private Project project;

    public AtgComponentsService(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    //TODO Different scopes for layers and
    public List<PropertiesFileImpl> getComponentsByName(@NotNull String componentName) {
        return FileBasedIndex.getInstance().getContainingFiles(AtgComponentsIndexExtension.NAME,
                componentName, GlobalSearchScope.allScope(project))
                .parallelStream()
                .map(this::getPropertiesPsiFileSafely)
                .collect(Collectors.toList());
    }

    @NotNull
    public Set<String> getComponentScopesWithBasedOns(@NotNull String bean) {
        return  getComponentDerivedPropertyWithBasedOns(bean, this::getComponentAndLayerScopes);
    }

    @NotNull
    public Set<String> getComponentClassStrWithBasedOns(@NotNull String bean) {
        return  getComponentDerivedPropertyWithBasedOns(bean, this::getComponentAndLayerClasses);
    }

    @NotNull
    private Set<String> getComponentDerivedPropertyWithBasedOns(@NotNull String bean, @NotNull Function<String, Set<String>> propertyExtractor) {
        Set<String> resolvedClasses = RecursionManager.doPreventingRecursion(bean, true, () -> {
            ProgressManager.checkCanceled();
            Set<String> beanClasses = propertyExtractor.apply(bean);
            if (!beanClasses.isEmpty()) return beanClasses;
            return getComponentAndLayerBasedOns(bean).
                    stream().
                    map(b -> getComponentDerivedPropertyWithBasedOns(b, propertyExtractor)).
                    flatMap(Collection::stream).
                    collect(Collectors.toSet());
        });
        return resolvedClasses != null ? resolvedClasses : new HashSet<>();
    }

    @NotNull
    private Set<String> getComponentAndLayerBasedOns(@NotNull String bean) {
        return FileBasedIndex.getInstance().getValues(AtgComponentsIndexExtension.NAME, bean, GlobalSearchScope.allScope(project))
                .parallelStream()
                .map(ComponentWrapper::getJavaClass)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @NotNull
    private Set<String> getComponentAndLayerClasses(@NotNull String bean) {
        return FileBasedIndex.getInstance().getValues(AtgComponentsIndexExtension.NAME, bean, GlobalSearchScope.allScope(project))
                .parallelStream()
                .map(ComponentWrapper::getJavaClass)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @NotNull
    private Set<String> getComponentAndLayerScopes(@NotNull String bean) {
        return FileBasedIndex.getInstance().getValues(AtgComponentsIndexExtension.NAME, bean, GlobalSearchScope.allScope(project))
                .parallelStream()
                .map(ComponentWrapper::getJavaClass)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @NotNull
    public Collection<String> getAllComponents() {
        return FileBasedIndex.getInstance().getAllKeys(AtgComponentsIndexExtension.NAME, project);
    }

    @NotNull
    public List<String> getComponentPropertyValues(@NotNull String componentName, @NotNull String propertyName) {
        return getComponentsByName(componentName).parallelStream()
                .map(f -> f.findPropertyByKey(propertyName))
                .filter(Objects::nonNull)
                .map(IProperty::getUnescapedValue)
                .collect(Collectors.toList());
    }

    public void notifyConfigRootsChanged(@NotNull Collection<VirtualFile> virtualFiles) {
        ApplicationManager.getApplication().invokeLater(() -> virtualFiles.forEach(FileBasedIndex.getInstance()::requestReindex)
                , project.getDisposed());
    }

    @NotNull
    public Collection<String> suggestComponentNamesByClass(@NotNull Collection<PsiClass> srcClasses) {
        if (srcClasses.isEmpty()) {
            return Collections.emptyList();
        }
        Collection<String> srcClassesStr = srcClasses.stream().map(PsiClass::getQualifiedName).collect(Collectors.toList());
        FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);

        Set<String> result = Sets.newHashSet();
        fileBasedIndex.processAllKeys(AtgComponentsIndexExtension.NAME, componentName -> {
            ProgressManager.checkCanceled();
            fileBasedIndex.processValues(AtgComponentsIndexExtension.NAME, componentName, null, (file, componentWrapper) -> {
                if (srcClassesStr.contains(componentWrapper.getJavaClass())) {
                    result.add(componentName);
                }
                return true;
            }, scope);
            return true;
        }, project);

        return result;
    }


    @Nullable
    private PropertiesFileImpl getPropertiesPsiFileSafely(@Nullable final VirtualFile file) {
        if (project.isDisposed() || file == null) {
            return null;
        }
        PsiFile psiFile = ApplicationManager.getApplication().runReadAction((Computable<PsiFile>) () ->
                file.isValid() ? PsiManager.getInstance(project).findFile(file) : null);
        return psiFile instanceof PropertiesFileImpl ? (PropertiesFileImpl) psiFile : null;
    }

}
