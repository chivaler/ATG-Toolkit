package org.idea.plugin.atg.util;

import com.intellij.facet.ProjectFacetManager;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.lang.jvm.types.JvmType;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.PropertiesImplUtil;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.xml.XmlFileImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.xml.XmlFile;
import org.apache.commons.lang.StringUtils;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.config.AtgConfigHelper;
import org.idea.plugin.atg.config.AtgToolkitConfig;
import org.idea.plugin.atg.module.AtgModuleFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AtgComponentUtil {

    private AtgComponentUtil() {
    }

    @NotNull
    public static Optional<String> getComponentClassStr(@Nullable PsiFile propertyFile) {
        if (propertyFile instanceof PropertiesFile) {
            PropertiesFile componentFile = (PropertiesFile) propertyFile;
            IProperty propertyClassName = componentFile.findPropertyByKey(Constants.Keywords.CLASS_PROPERTY);
            if (propertyClassName == null || StringUtils.isBlank(propertyClassName.getValue())) {
                IProperty basedOnComponent = componentFile.findPropertyByKey(Constants.Keywords.BASED_ON_PROPERTY);
                if (basedOnComponent != null && StringUtils.isBlank(basedOnComponent.getValue())) {
                    Module module = ModuleUtilCore.findModuleForPsiElement(propertyFile);
                    Collection<PropertiesFileImpl> applicableParents = getApplicableComponentsByName(basedOnComponent.getValue(), module, propertyFile.getProject());
                    for (PropertiesFileImpl parent : applicableParents) {
                        //TODO after layer sequence is done choose appropriate component, or combined
                        Optional<String> parentComponentClass = getComponentClassStr(parent);
                        if (parentComponentClass.isPresent()) return parentComponentClass;
                    }
                }
            } else {
                return Optional.of(propertyClassName.getValue());
            }
        }
        return Optional.empty();
    }

    @NotNull
    public static Optional<PsiClass> getComponentClass(@Nullable PsiFile propertyFile) {
        if (!(propertyFile instanceof PropertiesFile)) {
            return Optional.empty();
        }
        Optional<String> className = getComponentClassStr(propertyFile);
        if (!className.isPresent()) return Optional.empty();

        Project project = propertyFile.getProject();
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        return Optional.ofNullable(JavaPsiFacade.getInstance(project)
                .findClass(className.get(), scope));
    }

    @NotNull
    public static Collection<VirtualFile> getApplicableConfigRoots(@Nullable Module module,
                                                                   @NotNull Project project) {
        List<Library> libraries;
        if (module == null) {
            libraries = Arrays.stream(ProjectLibraryTable.getInstance(project).getLibraries())
                    .collect(Collectors.toList());
        } else {
            ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(module).getModifiableModel();
            libraries = Arrays.stream(modifiableModel.getOrderEntries())
                    .filter(LibraryOrderEntry.class::isInstance)
                    .map(f -> ((LibraryOrderEntry) f).getLibrary())
                    .collect(Collectors.toList());
        }

        List<VirtualFile> sourceConfigRoots = ProjectFacetManager.getInstance(project).getFacets(AtgModuleFacet.FACET_TYPE_ID).stream()
                .map(f -> f.getConfiguration().getConfigRoots())
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<VirtualFile> libraryVirtualFiles = libraries.stream()
                .filter(l -> l.getName() != null && l.getName().startsWith(Constants.ATG_CONFIG_LIBRARY_PREFIX))
                .map(l -> l.getFiles(OrderRootType.CLASSES))
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());

        sourceConfigRoots.addAll(libraryVirtualFiles);
        return sourceConfigRoots;
    }

    @NotNull
    public static Collection<PropertiesFileImpl> getApplicableComponentsByName(@NotNull String componentName,
                                                                               @Nullable Module module,
                                                                               @NotNull Project project) {
        return getApplicableConfigRoots(module, project).stream()
                .filter(Objects::nonNull)
                .filter(VirtualFile::isDirectory)
                .map(root -> VfsUtilCore.findRelativeFile(componentName + ".properties", root))
                .filter(Objects::nonNull)
                .filter(VirtualFile::exists)
                .map(virtualFile -> PsiManager.getInstance(project)
                        .findFile(virtualFile))
                .filter(PropertiesFileImpl.class::isInstance)
                .map(f -> (PropertiesFileImpl) f)
                .collect(Collectors.toList());
    }

    @NotNull
    public static Collection<XmlFileImpl> getApplicableXmlsByName(@NotNull String xmlRelativePath,
                                                                  @NotNull Project project) {
        return ProjectFacetManager.getInstance(project).getFacets(AtgModuleFacet.FACET_TYPE_ID).stream().map(f -> f.getConfiguration().getConfigRoots())
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .filter(VirtualFile::isDirectory)
                .map(root -> VfsUtilCore.findRelativeFile(xmlRelativePath + ".xml", root))
                .filter(Objects::nonNull)
                .filter(VirtualFile::exists)
                .map(virtualFile -> PsiManager.getInstance(project)
                        .findFile(virtualFile))
                .filter(XmlFileImpl.class::isInstance)
                .map(f -> (XmlFileImpl) f)
                .collect(Collectors.toList());
    }

    @NotNull
    public static Optional<String> getComponentCanonicalName(@NotNull PropertiesFile file) {
        return getComponentCanonicalName(file.getVirtualFile(), file.getProject());
    }

    @NotNull
    public static Optional<String> getXmlRelativePath(@NotNull XmlFile file) {
        return ProjectFacetManager.getInstance(file.getProject()).getFacets(AtgModuleFacet.FACET_TYPE_ID).stream().map(f -> f.getConfiguration().getConfigRoots())
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .filter(VirtualFile::isDirectory)
                .filter(r -> VfsUtilCore.isAncestor(r, file.getVirtualFile(), true))
                .map(r -> "/" + VfsUtilCore.getRelativeLocation(file.getVirtualFile(), r).replace(".xml", ""))
                .findFirst();
    }

    @NotNull
    public static Optional<String> getComponentCanonicalName(@NotNull VirtualFile file, @NotNull Project project) {
        return ProjectFacetManager.getInstance(project).getFacets(AtgModuleFacet.FACET_TYPE_ID).stream().map(f -> f.getConfiguration().getConfigRoots())
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .filter(VirtualFile::isDirectory)
                .filter(r -> VfsUtilCore.isAncestor(r, file, true))
                .map(r -> "/" + VfsUtilCore.getRelativeLocation(file, r).replace(".properties", ""))
                .findFirst();
    }

    @Nullable
    public static JvmType getJvmTypeForComponentDependency(@NotNull PropertyImpl property) {
        Optional<PsiMethod> setterForKey = getSetterForProperty(property);
        return setterForKey.map(AtgComponentUtil::getJvmTypeForSetterMethod).orElse(null);
    }

    @Nullable
    public static PsiClass getClassForComponentDependency(@NotNull PropertyImpl property) {
        Optional<PsiMethod> setterForKey = getSetterForProperty(property);
        return setterForKey.map(AtgComponentUtil::getPsiClassForSetterMethod).orElse(null);
    }

    @NotNull
    private static Optional<PsiMethod> getSetterForProperty(@NotNull PropertyImpl property) {
        PsiFile propertyFile = property.getContainingFile();
        String key = property.getKey();
        if (StringUtils.isBlank(key) || key.startsWith("$")) {
            return Optional.empty();
        }
        if (key.endsWith("^")) {
            key = key.substring(0, key.length() - 1);
        }
        final String searchProperty = key;
        Optional<PsiClass> srcClass = getComponentClass(propertyFile);
        if (!srcClass.isPresent()) {
            return Optional.empty();
        }
        return Arrays.stream(srcClass.get().getAllMethods())
                .filter(m -> m.getName()
                        .equals(AtgComponentUtil.convertVariableToSetter(searchProperty)))
                .findAny();
    }

    @Nullable
    public static PsiClass getPsiClassForSetterMethod(@NotNull PsiMethod setter) {
        JvmType keyType = getJvmTypeForSetterMethod(setter);
        if (keyType instanceof PsiClassType) {
            return ((PsiClassType) keyType).resolve();
        }
        return null;
    }

    @Nullable
    public static JvmType getJvmTypeForSetterMethod(@NotNull PsiMethod setter) {
        JvmParameter[] parametersList = setter.getParameters();
        return parametersList.length > 0 ? parametersList[0].getType() : null;
    }


    @NotNull
    public static List<String> suggestComponentsNamesByClass(@Nullable PsiClass psiClass) {
        if (psiClass == null) {
            return Collections.emptyList();
        }
        return suggestComponentsByClass(psiClass).stream()
                .map(AtgComponentUtil::getComponentCanonicalName)
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                .distinct()
                .collect(Collectors.toList());
    }

    @NotNull
    public static List<PropertiesFileImpl> suggestComponentsByClassWithInheritors(@Nullable PsiClass srcClass) {
        if (srcClass == null) {
            return Collections.emptyList();
        }
        GlobalSearchScope scope = GlobalSearchScope.allScope(srcClass.getProject());
        Collection<PsiClass> classAndInheritors = ClassInheritorsSearch.search(srcClass, scope, true, true, false)
                .findAll();
        classAndInheritors.add(srcClass);
        return classAndInheritors
                .stream()
                .map(AtgComponentUtil::suggestComponentsByClass)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
    }

    @NotNull
    public static List<PropertiesFileImpl> suggestComponentsByClass(@Nullable PsiClass srcClass) {
        if (srcClass == null) {
            return Collections.emptyList();
        }
        Project project = srcClass.getProject();
        String className = srcClass.getQualifiedName();
        if (className == null || StringUtils.isBlank(className)) {
            return Collections.emptyList();
        }

        Stream<PropertiesFileImpl> originalComponentsFiles = PropertiesImplUtil.findPropertiesByKey(project, Constants.Keywords.CLASS_PROPERTY)
                .stream()
                .filter(Objects::nonNull)
                .filter(p -> className.equals(p.getValue()))
                .map(IProperty::getPropertiesFile)
                .filter(PropertiesFileImpl.class::isInstance)
                .map(p -> (PropertiesFileImpl) p);

        List<IProperty> allBasedOnProperties = PropertiesImplUtil.findPropertiesByKey(project, Constants.Keywords.BASED_ON_PROPERTY);
        Stream<PropertiesFileImpl> basedOnPropertiesFiles = allBasedOnProperties.stream()
                .filter(Objects::nonNull)
                .map(IProperty::getPropertiesFile)
                .filter(f -> f.findPropertyByKey(Constants.Keywords.CLASS_PROPERTY) == null)
                .filter(PropertiesFileImpl.class::isInstance)
                .map(p -> (PropertiesFileImpl) p)
                .filter(f -> className.equals(getComponentClassStr(f).orElse(null)));
        return Stream.concat(originalComponentsFiles, basedOnPropertiesFiles)
                .distinct()
                .collect(Collectors.toList());
    }

    @NotNull
    public static List<PropertiesFile> getAllComponents(@NotNull Project project) {
        return PropertiesImplUtil.findPropertiesByKey(project, Constants.Keywords.CLASS_PROPERTY)
                .stream()
                .filter(Objects::nonNull)
                .map(IProperty::getPropertiesFile)
                .distinct()
                .collect(Collectors.toList());
    }

    @NotNull
    public static List<PsiMethod> getSettersOfClass(@NotNull PsiClass psiClass) {
        return Arrays.stream(psiClass.getAllMethods())
                .filter(method -> method.getName()
                        .startsWith("set"))
                .filter(m -> m.hasModifier(JvmModifier.PUBLIC))
                .filter(m -> !m.hasModifier(JvmModifier.ABSTRACT))
                .filter(m -> m.getParameters().length == 1)
                .filter(new IsMethodIgnored())
                .collect(Collectors.toList());
    }

    @NotNull
    public static String convertVariableToSetter(@NotNull String var) {
        return "set" + var.substring(0, 1)
                .toUpperCase() + var.substring(1);
    }

    @NotNull
    public static String convertSetterToVariableName(@NotNull PsiMethod setterMethod) {
        return convertSetterNameToVariableName(setterMethod.getName());
    }

    @NotNull
    public static String convertSetterNameToVariableName(@NotNull String methodName) {
        return methodName.substring(3, 4)
                .toLowerCase() + methodName.substring(4);
    }

    public static boolean isApplicableToHaveComponents(@Nullable PsiClass psiClass) {
        if (psiClass == null) return false;
        if (psiClass.isAnnotationType() || psiClass.isInterface() || psiClass instanceof PsiAnonymousClass)
            return true;
        PsiModifierList modifierList = psiClass.getModifierList();
        return modifierList != null && !modifierList.hasModifierProperty(PsiModifier.ABSTRACT) && modifierList.hasModifierProperty(PsiModifier.PUBLIC);
    }

    static class IsMethodIgnored implements Predicate<PsiMethod> {
        private final List<Pattern> ignoredClassPatterns;

        IsMethodIgnored() {
            AtgToolkitConfig atgToolkitConfig = org.idea.plugin.atg.config.AtgToolkitConfig.getInstance();
            String ignoredClassesForSetters = atgToolkitConfig.getIgnoredClassesForSetters();
            ignoredClassPatterns = AtgConfigHelper.convertToPatternList(ignoredClassesForSetters);
        }

        @Override
        public boolean test(PsiMethod psiMethod) {
            for (Pattern classNamePattern : ignoredClassPatterns) {
                PsiClass containingClass = psiMethod.getContainingClass();
                if (containingClass == null || containingClass.isInterface()) return false;
                String className = containingClass.getQualifiedName();
                if (StringUtils.isNotBlank(className) && classNamePattern.matcher(className)
                        .matches()) {
                    return false;
                }
            }
            return true;
        }
    }

}
