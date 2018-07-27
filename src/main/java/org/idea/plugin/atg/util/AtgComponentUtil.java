package org.idea.plugin.atg.util;

import com.intellij.lang.jvm.JvmModifier;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.lang.jvm.types.JvmType;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.PropertiesImplUtil;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import org.apache.commons.lang.StringUtils;
import org.idea.plugin.atg.AtgConfigHelper;
import org.idea.plugin.atg.config.AtgToolkitConfig;
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
    public static Optional<PsiClass> getComponentClass(@Nullable PsiFile propertyFile) {
        if (!(propertyFile instanceof PropertiesFile)) {
            return Optional.empty();
        }
        IProperty propertyClassName = ((PropertiesFile) propertyFile).findPropertyByKey("$class");
        if (propertyClassName == null || StringUtils.isBlank(propertyClassName.getValue())) {
            return Optional.empty();
        }
        String className = propertyClassName.getValue();
        Project project = propertyFile.getProject();
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        return Optional.ofNullable(JavaPsiFacade.getInstance(project)
                .findClass(className, scope));
    }

    @NotNull
    public static Collection<PsiElement> getApplicableComponentsByName(@NotNull String componentName,
                                                                       @NotNull Project project) {
        return Arrays.stream(ModuleManager.getInstance(project)
                .getModules())
                .map(AtgConfigHelper::getConfigRoot)
                .map(path -> LocalFileSystem.getInstance()
                        .findFileByPath(path))
                .filter(Objects::nonNull)
                .filter(VirtualFile::isDirectory)
                .map(root -> VfsUtilCore.findRelativeFile(componentName + ".properties", root))
                .filter(Objects::nonNull)
                .filter(VirtualFile::exists)
                .map(virtualFile -> PsiManager.getInstance(project)
                        .findFile(virtualFile))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @NotNull
    public static Optional<String> getComponentCanonicalName(@NotNull PropertiesFileImpl file) {
        return getComponentCanonicalName(file.getVirtualFile(), file.getProject());
    }

    @NotNull
    public static Optional<String> getComponentCanonicalName(@NotNull VirtualFile file, @NotNull Project project) {
        Module module = ModuleUtilCore.findModuleForFile(file, project);
        if (module == null) {
            return Optional.empty();
        }
        String configRoot = AtgConfigHelper.getConfigRoot(module);
        VirtualFile configRootFile = LocalFileSystem.getInstance()
                .findFileByPath(configRoot);
        if (configRootFile == null) {
            return Optional.empty();
        }
        if (!VfsUtilCore.isAncestor(configRootFile, file, true)) {
            return Optional.empty();
        }
        String relativePath = VfsUtilCore.getRelativeLocation(file, configRootFile);
        if (relativePath == null) {
            return Optional.empty();
        }
        return Optional.of("/" + relativePath.replace(".properties", ""));
    }

    @Nullable
    @SuppressWarnings("OptionalIsPresent")
    public static PsiClass getClassForComponentDependency(@NotNull PropertyImpl property) {
        PsiFile propertyFile = property.getContainingFile();
        String key = property.getKey();
        if (StringUtils.isBlank(key) || key.startsWith("$")) {
            return null;
        }
        Optional<PsiClass> srcClass = getComponentClass(propertyFile);
        if (!srcClass.isPresent()) {
            return null;
        }
        Optional<PsiMethod> setterForKey = Arrays.stream(srcClass.get().getAllMethods())
                .filter(m -> m.getName()
                        .equals(AtgComponentUtil.convertVariableToSetter(key)))
                .findAny();
        if (!setterForKey.isPresent()) {
            return null;
        }
        return getClassForSetterMethod(setterForKey.get());
    }

    @Nullable
    public static PsiClass getClassForSetterMethod(@NotNull PsiMethod setter) {
        JvmParameter[] parametersList = setter.getParameters();
        JvmType keyType = parametersList.length > 0 ? parametersList[0].getType() : null;

        if (!(keyType instanceof PsiClassType)) {
            return null;
        }
        return ((PsiClassType) keyType).resolve();
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
        return PropertiesImplUtil.findPropertiesByKey(project, "$class")
                .stream()
                .filter(Objects::nonNull)
                .filter(p -> p.getValue() != null && p.getValue()
                        .equals(className))
                .map(IProperty::getPropertiesFile)
                .filter(PropertiesFileImpl.class::isInstance)
                .map(p -> (PropertiesFileImpl) p)
                .distinct()
                .collect(Collectors.toList());
    }

    @NotNull
    public static List<PsiMethod> getSettersOfClass(@NotNull PsiClass psiClass) {
        return Arrays.stream(psiClass.getAllMethods())
                .filter(m -> m.hasModifier(JvmModifier.PUBLIC))
                .filter(method -> method.getName()
                        .startsWith("set"))
                .filter(m -> m.getParameters().length == 1)
                .filter(new IsMethodIgnored(psiClass.getProject()))
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

        IsMethodIgnored(Project project) {
            AtgToolkitConfig atgToolkitConfig = org.idea.plugin.atg.config.AtgToolkitConfig.getInstance(project);
            String ignoredClassesForSetters =
                    atgToolkitConfig != null ? atgToolkitConfig.getIgnoredClassesForSetters() : "";
            String[] ignoredClassesForSettersArray = ignoredClassesForSetters.replace(".", "\\.")
                    .replace("?", ".?")
                    .replace("*", ".*?")
                    .split("[,;]");
            ignoredClassPatterns = Stream.of(ignoredClassesForSettersArray)
                    .map(Pattern::compile)
                    .collect(Collectors.toList());
        }

        @Override
        public boolean test(PsiMethod psiMethod) {
            for (Pattern classNamePattern : ignoredClassPatterns) {
                PsiClass containingClass = psiMethod.getContainingClass();
                String className = containingClass != null ? containingClass.getQualifiedName() : "";
                if (StringUtils.isNotBlank(className) && classNamePattern.matcher(className)
                        .matches()) {
                    return false;
                }
            }
            return true;
        }
    }

}
