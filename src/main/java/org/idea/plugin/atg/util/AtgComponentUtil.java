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

    @Nullable
    public static PsiClass getComponentClass(@Nullable PsiFile propertyFile) {
        if (!(propertyFile instanceof PropertiesFile)) return null;
        IProperty propertyClassName = ((PropertiesFile) propertyFile).findPropertyByKey("$class");
        if (propertyClassName == null || StringUtils.isBlank(propertyClassName.getValue())) return null;
        String className = propertyClassName.getValue();
        Module module = ModuleUtilCore.findModuleForFile(propertyFile.getOriginalFile());
        Project project = propertyFile.getProject();
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        if (module != null) scope = GlobalSearchScope.moduleWithDependenciesScope(module);
        return JavaPsiFacade.getInstance(project).findClass(className, scope);
    }


    @NotNull
    public static Collection<PsiElement> getApplicableComponentsByName(@NotNull String componentName, @NotNull Project project) {
        return Arrays.stream(ModuleManager.getInstance(project).getModules())
                .map(AtgConfigHelper::getConfigRoot)
                .map(path -> LocalFileSystem.getInstance().findFileByPath(path))
                .filter(Objects::nonNull)
                .filter(VirtualFile::isDirectory)
                .map(root -> VfsUtilCore.findRelativeFile(componentName + ".properties", root))
                .filter(Objects::nonNull)
                .filter(VirtualFile::exists)
                .map(virtualFile -> PsiManager.getInstance(project).findFile(virtualFile))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Nullable
    public static String getComponentCanonicalName(@NotNull VirtualFile file, @NotNull Project project) {
        Module module = ModuleUtilCore.findModuleForFile(file, project);
        if (module == null) return null;
        String configRoot = AtgConfigHelper.getConfigRoot(module);
        VirtualFile configRootFile = LocalFileSystem.getInstance().findFileByPath(configRoot);
        if (configRootFile == null) return null;
        if (!VfsUtilCore.isAncestor(configRootFile, file, true)) return null;
        String relativePath = VfsUtilCore.getRelativeLocation(file, configRootFile);
        if (relativePath == null) return null;
        return "/" + relativePath.replace(".properties", "");
    }

    @Nullable
    @SuppressWarnings("OptionalIsPresent")
    public static String getClassNameForComponentDependency(@NotNull PropertyImpl property) {
        PsiFile propertyFile = property.getContainingFile();
        String key = property.getKey();
        if (StringUtils.isBlank(key) || key.startsWith("$")) return null;
        Project project = propertyFile.getProject();
        IProperty componentClassName = ((PropertiesFile) propertyFile).findPropertyByKey("$class");
        if (componentClassName == null || StringUtils.isBlank(componentClassName.getValue())) return null;
        Module module = ModuleUtilCore.findModuleForFile(propertyFile.getOriginalFile());
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        if (module != null) scope = GlobalSearchScope.moduleWithDependenciesScope(module);
        PsiClass srcClass = JavaPsiFacade.getInstance(project).findClass(componentClassName.getValue(), scope);
        if (srcClass == null) return null;
        Optional<PsiMethod> setterForKey = Arrays.stream(srcClass.getAllMethods())
                .filter(m -> m.getName().equals(AtgComponentUtil.convertVariableToSetter(key)))
                .findAny();
        if (!setterForKey.isPresent()) return null;
        return getClassNameForSetterMethod(setterForKey.get());
    }

    @Nullable
    public static String getClassNameForSetterMethod(@NotNull PsiMethod setter) {
        JvmParameter[] parametersList = setter.getParameters();
        JvmType keyType = parametersList.length > 0 ? parametersList[0].getType() : null;

        if (!(keyType instanceof PsiClassType)) return null;
        return ((PsiClassType) keyType).getCanonicalText();
    }

    @NotNull
    public static List<String> suggestComponentsByClassName(@Nullable String className, @NotNull Project project) {
        return PropertiesImplUtil.findPropertiesByKey(project, "$class").stream()
                .filter(Objects::nonNull)
                .filter(p -> p.getValue() != null && p.getValue().equals(className))
                .map(IProperty::getPropertiesFile)
                .map(PropertiesFile::getVirtualFile)
                .map(file -> getComponentCanonicalName(file, project))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    @NotNull
    public static List<PsiFile> suggestComponentsByClass(@NotNull PsiClass srcClass) {
        Project project = srcClass.getProject();
        String className = srcClass.getQualifiedName();
        return PropertiesImplUtil.findPropertiesByKey(project, "$class").stream()
                .filter(Objects::nonNull)
                .filter(p -> p.getValue() != null && p.getValue().equals(className))
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
                .filter(method -> method.getName().startsWith("set"))
                .filter(m -> m.getParameters().length == 1)
                .filter(new IsMethodIgnored(psiClass.getProject()))
                .collect(Collectors.toList());
    }

    @NotNull
    public static String convertVariableToSetter(@NotNull String var) {
        return "set" + var.substring(0, 1).toUpperCase() + var.substring(1);
    }

    @NotNull
    public static String convertSetterToVariableName(@NotNull PsiMethod setterMethod) {
        return convertSetterNameToVariableName(setterMethod.getName());
    }

    @NotNull
    public static String convertSetterNameToVariableName(@NotNull String methodName) {
        return methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
    }

    static class IsMethodIgnored implements Predicate<PsiMethod> {
        private final List<Pattern> ignoredClassPatterns;

        IsMethodIgnored(Project project) {
            AtgToolkitConfig atgToolkitConfig = org.idea.plugin.atg.config.AtgToolkitConfig.getInstance(project);
            String ignoredClassesForSetters = atgToolkitConfig != null ? atgToolkitConfig.getIgnoredClassesForSetters() : "";
            String[] ignoredClassesForSettersArray = ignoredClassesForSetters
                    .replace(".", "\\.")
                    .replace("?", ".?")
                    .replace("*", ".*?")
                    .split("[,;]");
            ignoredClassPatterns = Stream.of(ignoredClassesForSettersArray).map(Pattern::compile).collect(Collectors.toList());
        }

        @Override
        public boolean test(PsiMethod psiMethod) {
            for (Pattern classNamePattern : ignoredClassPatterns) {
                PsiClass containingClass = psiMethod.getContainingClass();
                String className = containingClass != null ? containingClass.getQualifiedName() : "";
                if (StringUtils.isNotBlank(className) && classNamePattern.matcher(className).matches()) return false;
            }
            return true;
        }
    }

}
