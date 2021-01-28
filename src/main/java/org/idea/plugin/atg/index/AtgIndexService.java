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
import com.intellij.psi.impl.source.xml.XmlFileImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AtgIndexService {
    @NotNull
    private final Project project;

    public AtgIndexService(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    //TODO Different scopes for layers and
    public List<PropertiesFileImpl> getComponentsByName(@NotNull String componentName) {
        return FileBasedIndex.getInstance().getContainingFiles(AtgComponentsIndexExtension.NAME,
                componentName, GlobalSearchScope.allScope(project))
                .stream()
                .map(this::getPsiFileSafely)
                .filter(PropertiesFileImpl.class::isInstance)
                .map(PropertiesFileImpl.class::cast)
                .collect(Collectors.toList());
    }

    @NotNull
    //TODO Different scopes for layers and
    public List<XmlFileImpl> getXmlsByName(@NotNull String componentName) {
        return FileBasedIndex.getInstance().getContainingFiles(AtgXmlsIndexExtension.NAME,
                componentName, GlobalSearchScope.allScope(project))
                .stream()
                .map(this::getPsiFileSafely)
                .filter(XmlFileImpl.class::isInstance)
                .map(XmlFileImpl.class::cast)
                .collect(Collectors.toList());
    }

    @NotNull
    public Set<String> getComponentDerivedPropertyWithBasedOns(@NotNull String bean, @NotNull Function<ComponentWrapper, String> propertyExtractor) {
        Set<String> resolvedValues = RecursionManager.doPreventingRecursion(bean, true, () -> {
            ProgressManager.checkCanceled();
            Set<String> propertyValues = getComponentIndexedProperty(bean, propertyExtractor);
            if (!propertyValues.isEmpty()) return propertyValues;
            return getComponentIndexedProperty(bean, ComponentWrapper::getBasedOn).
                    stream().
                    map(b -> getComponentDerivedPropertyWithBasedOns(b, propertyExtractor)).
                    flatMap(Collection::stream).
                    collect(Collectors.toSet());
        });
        return resolvedValues != null ? resolvedValues : new HashSet<>();
    }

    @NotNull
    private Set<String> getComponentAndLayerBasedOns(@NotNull String bean) {
        return FileBasedIndex.getInstance().getValues(AtgComponentsIndexExtension.NAME, bean, GlobalSearchScope.allScope(project))
                .stream()
                .map(ComponentWrapper::getBasedOn)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @NotNull
    private Set<String> getComponentIndexedProperty(@NotNull String bean, @NotNull Function<ComponentWrapper, String> propertyExtractor) {
        return FileBasedIndex.getInstance().getValues(AtgComponentsIndexExtension.NAME, bean, GlobalSearchScope.allScope(project))
                .stream()
                .map(propertyExtractor)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @NotNull
    private Set<String> getComponentAndLayerClasses(@NotNull String bean) {
        return FileBasedIndex.getInstance().getValues(AtgComponentsIndexExtension.NAME, bean, GlobalSearchScope.allScope(project))
                .stream()
                .map(ComponentWrapper::getJavaClass)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @NotNull
    private Set<String> getComponentAndLayerScopes(@NotNull String bean) {
        return FileBasedIndex.getInstance().getValues(AtgComponentsIndexExtension.NAME, bean, GlobalSearchScope.allScope(project))
                .stream()
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
        return getComponentsByName(componentName)
                .stream()
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
    public Set<String> suggestComponentNamesByClass(@NotNull Collection<PsiClass> srcClasses) {
        if (srcClasses.isEmpty()) {
            return Collections.emptySet();
        }
        Collection<String> srcClassesStr = srcClasses.stream()
                .map(PsiClass::getQualifiedName)
                .collect(Collectors.toSet());
        FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);

        Set<String> result = Sets.newHashSet();
        Collection<String> allComponentsNames = fileBasedIndex.getAllKeys(AtgComponentsIndexExtension.NAME, project);
        for (String componentName : allComponentsNames) {
            fileBasedIndex.processValues(AtgComponentsIndexExtension.NAME, componentName, null, (file, componentWrapper) -> {
                if (srcClassesStr.contains(componentWrapper.getJavaClass())) {
                    result.add(componentName);
                }
                return true;
            }, scope);
        }

        return result;
    }


    @Nullable
    private PsiFile getPsiFileSafely(@Nullable final VirtualFile file) {
        if (project.isDisposed() || file == null) {
            return null;
        }
        return ApplicationManager.getApplication().runReadAction((Computable<PsiFile>) () ->
                file.isValid() ? PsiManager.getInstance(project).findFile(file) : null);
    }

}
