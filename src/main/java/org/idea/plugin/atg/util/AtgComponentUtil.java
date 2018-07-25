package org.idea.plugin.atg.util;

import com.intellij.lang.jvm.JvmModifier;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.lang.jvm.types.JvmType;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.PropertiesImplUtil;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.apache.commons.lang.StringUtils;
import org.idea.plugin.atg.AtgConfigHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

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
    public static List<PsiMethod> getSettersOfClass(@NotNull PsiClass psiClass) {
        return Arrays.stream(psiClass.getAllMethods())
                .filter(m -> m.hasModifier(JvmModifier.PUBLIC))
                .filter(method -> method.getName().startsWith("set"))
                .filter(m -> m.getParameters().length == 1)
                .collect(Collectors.toList());
    }

    @NotNull
    public static String convertVariableToSetter(@NotNull String var) {
        return "set" + var.substring(0, 1).toUpperCase() + var.substring(1);
    }

    @NotNull
    public static String convertSetterNameToVariableName(@NotNull PsiMethod setterMethod) {
        return convertSetterNameToVariableName(setterMethod.getName());
    }

    @NotNull
    public static String convertSetterNameToVariableName(@NotNull String methodName) {
        return methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
    }

}
