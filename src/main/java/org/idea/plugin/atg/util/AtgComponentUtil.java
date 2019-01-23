package org.idea.plugin.atg.util;

import com.intellij.facet.FacetManager;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.lang.jvm.types.JvmType;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.xml.XmlFileImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.xml.XmlFile;
import com.intellij.testFramework.LightVirtualFile;
import org.apache.commons.lang.StringUtils;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.config.AtgConfigHelper;
import org.idea.plugin.atg.config.AtgToolkitConfig;
import org.idea.plugin.atg.index.AtgComponentsService;
import org.idea.plugin.atg.module.AtgModuleFacet;
import org.idea.plugin.atg.module.AtgModuleFacetConfiguration;
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
    public static Optional<String> getDerivedProperty(@NotNull PropertiesFileImpl propertyFile,
                                                      @NotNull String propertyName) {
        return getDerivedProperty(propertyFile, propertyName, true, new ArrayList<>());
    }

    @NotNull
    public static Optional<String> getDerivedProperty(@NotNull PropertiesFileImpl propertyFile,
                                                      @NotNull String propertyName,
                                                      boolean searchInAllLayers,
                                                      @NotNull List<PropertiesFileImpl> resolvedFiles) {
        //TODO after layer sequence is done choose appropriate component, or combined
        //TODO use RecursionManager.doPreventingRecursion(method, true, () -> {});
        resolvedFiles.add(propertyFile);
        Optional<String> componentName = getComponentCanonicalName(propertyFile);
        if (componentName.isPresent()) {
            IProperty propertyClassName = propertyFile.findPropertyByKey(propertyName);
            if (propertyClassName != null && StringUtils.isNotBlank(propertyClassName.getValue())) {
                return Optional.of(propertyClassName.getValue());
            } else {
                Project project = propertyFile.getProject();
                AtgComponentsService componentsService = ServiceManager.getService(project, AtgComponentsService.class);
                Collection<PropertiesFileImpl> applicableLayers = searchInAllLayers
                        ? componentsService.getComponentsByName(componentName.get())
                        : Collections.emptyList();

                Optional<String> otherLayerDefined = applicableLayers.stream()
                        .filter(p -> !resolvedFiles.contains(p))
                        .map(p -> p.findPropertyByKey(propertyName))
                        .filter(Objects::nonNull)
                        .map(IProperty::getValue)
                        .filter(StringUtils::isNotBlank)
                        .findFirst();
                if (otherLayerDefined.isPresent()) {
                    return otherLayerDefined;
                }

                Stream<PropertiesFileImpl> streamForBasedOnResolve = searchInAllLayers ? Stream.of(propertyFile) : applicableLayers.stream();

                return streamForBasedOnResolve.map(p -> p.findPropertyByKey(Constants.Keywords.Properties.BASED_ON_PROPERTY))
                        .filter(Objects::nonNull)
                        .map(IProperty::getValue)
                        .filter(StringUtils::isNotBlank)
                        .map(componentsService::getComponentsByName)
                        .flatMap(Collection::stream)
                        .filter(p -> !resolvedFiles.contains(p))
                        .map(p -> getDerivedProperty(p, propertyName, false, resolvedFiles))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .filter(StringUtils::isNotBlank)
                        .findFirst();
            }
        }

        return Optional.empty();
    }

    @NotNull
    public static String getComponentScope(@NotNull PropertiesFileImpl propertyFile) {
        return getDerivedProperty(propertyFile, Constants.Keywords.Properties.SCOPE_PROPERTY).orElse("global");
    }

    @NotNull
    public static Set<String> getComponentClassesStr(@NotNull PropertiesFileImpl propertyFile) {
        AtgComponentsService componentsService = ServiceManager.getService(propertyFile.getProject(), AtgComponentsService.class);
        Optional<String> beanName = getComponentCanonicalName(propertyFile);
        return beanName.map(componentsService::getComponentClassStrWithBasedOns).orElse(new HashSet<>());
    }

    @NotNull
    public static Optional<PsiClass> getSupposedComponentClass(@Nullable PsiFile propertyFile) {
        if (!(propertyFile instanceof PropertiesFileImpl)) {
            return Optional.empty();
        }
        Set<String> classesName = getComponentClassesStr((PropertiesFileImpl) propertyFile);

        Iterator<String> classesIterator = classesName.iterator();
        if (classesIterator.hasNext()) {
            Project project = propertyFile.getProject();
            GlobalSearchScope scope = GlobalSearchScope.allScope(project);
            return Optional.ofNullable(JavaPsiFacade.getInstance(project)
                    .findClass(classesIterator.next(), scope));
        } else return  Optional.empty();
    }

    @NotNull
    @Deprecated
    public static Collection<VirtualFile> getApplicableConfigRoots(@Nullable Module module,
                                                                   @NotNull Project project,
                                                                   boolean includeConfigLayers) {
        List<Library> libraries;
        if (module == null) {
            libraries = Arrays.stream(ProjectLibraryTable.getInstance(project).getLibraries())
                    .collect(Collectors.toList());
        } else {
            libraries = Arrays.stream(ModuleRootManager.getInstance(module).getOrderEntries())
                    .filter(LibraryOrderEntry.class::isInstance)
                    .map(f -> ((LibraryOrderEntry) f).getLibrary())
                    .collect(Collectors.toList());
        }

        List<VirtualFile> sourceConfigRoots = ProjectFacetManager.getInstance(project).getFacets(Constants.FACET_TYPE_ID).stream()
                .map(f -> f.getConfiguration().getConfigRoots())
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<VirtualFile> sourceConfigLayersRoots = new ArrayList<>();
        if (includeConfigLayers) {
            sourceConfigLayersRoots = ProjectFacetManager.getInstance(project).getFacets(Constants.FACET_TYPE_ID).stream()
                    .map(f -> f.getConfiguration().getConfigLayerRoots().keySet())
                    .flatMap(Collection::stream)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        List<VirtualFile> libraryVirtualFiles = libraries.stream()
                .filter(l -> l.getName() != null && l.getName().startsWith(Constants.ATG_CONFIG_LIBRARY_PREFIX))
                .map(l -> l.getFiles(OrderRootType.CLASSES))
                .flatMap(Arrays::stream)
                .distinct()
                .collect(Collectors.toList());

        sourceConfigRoots.addAll(sourceConfigLayersRoots);
        sourceConfigRoots.addAll(libraryVirtualFiles);
        return sourceConfigRoots;
    }

    @NotNull
    public static List<XmlFileImpl> getApplicableXmlsByName(@NotNull String xmlRelativePath,
                                                            @NotNull Project project) {
        return getApplicableConfigRoots(null, project, false).stream()
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
    public static Optional<String> getComponentCanonicalName(@NotNull PropertiesFileImpl file) {
        VirtualFile virtualFile = file.getViewProvider().getVirtualFile();
        return getComponentCanonicalName(virtualFile, file.getProject());
    }

    @NotNull
    public static Optional<String> getComponentCanonicalName(@NotNull VirtualFile supposedVirtualFile, @NotNull Project project) {
        VirtualFile virtualFile = supposedVirtualFile instanceof LightVirtualFile ?
                ((LightVirtualFile) supposedVirtualFile).getOriginalFile() :
                supposedVirtualFile;
        if (!supposedVirtualFile.isValid()) return Optional.empty();
        if (virtualFile.getFileSystem() instanceof JarFileSystem) {
            Optional<Library> libraryForFile = ProjectFileIndex.getInstance(project).getOrderEntriesForFile(virtualFile).stream()
                    .filter(LibraryOrderEntry.class::isInstance)
                    .map(f -> ((LibraryOrderEntry) f).getLibrary())
                    .filter(Objects::nonNull)
                    //TODO probably not needed
                    .filter(l -> l.getName() != null && l.getName().startsWith(Constants.ATG_CONFIG_LIBRARY_PREFIX))
                    .findAny();
            if (libraryForFile.isPresent()) {
                String path = virtualFile.getPath();
                int separatorIndex = path.indexOf("!/");
                return Optional.of(path.substring(separatorIndex + 1).replace(".properties", ""));
            }
        }

        Module module = ModuleUtilCore.findModuleForFile(virtualFile, project);
        if (module != null) {
            AtgModuleFacet atgFacet = FacetManager.getInstance(module).getFacetByType(Constants.FACET_TYPE_ID);
            if (atgFacet != null) {
                AtgModuleFacetConfiguration configuration = atgFacet.getConfiguration();
                Collection<VirtualFile> configRoots = configuration.getConfigRoots();
                Set<VirtualFile> configLayersRoots = configuration.getConfigLayerRoots().keySet();
                return Stream.concat(configRoots.stream(), configLayersRoots.stream())
                        .filter(Objects::nonNull)
                        .filter(VirtualFile::isDirectory)
                        .filter(r -> VfsUtilCore.isAncestor(r, virtualFile, true))
                        .map(r -> "/" + VfsUtilCore.getRelativeLocation(virtualFile, r).replace(".properties", ""))
                        .findFirst();
            }
        }

        return Optional.empty();
    }

    @NotNull
    public static Optional<String> getXmlRelativePath(@NotNull XmlFile file) {
        return getApplicableConfigRoots(null, file.getProject(), true).stream()
                .filter(Objects::nonNull)
                .filter(VirtualFile::isDirectory)
                .filter(r -> VfsUtilCore.isAncestor(r, file.getVirtualFile(), true))
                .map(r -> "/" + VfsUtilCore.getRelativeLocation(file.getVirtualFile(), r).replace(".xml", ""))
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
    public static Optional<PsiMethod> getSetterForProperty(@NotNull PropertyImpl property) {
        PsiFile propertyFile = property.getContainingFile();
        String key = property.getKey();
        if (StringUtils.isBlank(key) || key.startsWith("$")) {
            return Optional.empty();
        }
        Optional<PsiClass> srcClass = getSupposedComponentClass(propertyFile);
        if (!srcClass.isPresent()) {
            return Optional.empty();
        }
        return Arrays.stream(srcClass.get().getAllMethods())
                .filter(m -> m.getName()
                        .equals(AtgComponentUtil.convertPropertyNameToSetter(key)))
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
    public static PsiClass getPsiClassForGetterMethod(@NotNull PsiMethod getter) {
        JvmType keyType = getter.getReturnType();
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
    public static Collection<String> suggestComponentNamesByClassWithInheritors(@Nullable PsiClass srcClass) {
        if (srcClass == null) {
            return Collections.emptyList();
        }
        GlobalSearchScope scope = GlobalSearchScope.allScope(srcClass.getProject());
        Collection<PsiClass> classAndInheritors = ClassInheritorsSearch.search(srcClass, scope, true, true, false)
                .findAll();
        classAndInheritors.add(srcClass);
        AtgComponentsService componentsService = ServiceManager.getService(srcClass.getProject(), AtgComponentsService.class);
        return componentsService.suggestComponentNamesByClass(classAndInheritors);
    }

    @NotNull
    public static List<PropertiesFileImpl> suggestComponentsByClassWithInheritors(@Nullable PsiClass srcClass) {
        if (srcClass == null) {
            return Collections.emptyList();
        }
        AtgComponentsService componentsService = ServiceManager.getService(srcClass.getProject(), AtgComponentsService.class);
        return suggestComponentNamesByClassWithInheritors(srcClass)
                .stream()
                .map(componentsService::getComponentsByName)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @NotNull
    public static Collection<PropertiesFileImpl> suggestComponentsByClasses(@NotNull Collection<PsiClass> srcClasses, @NotNull Project project) {
        AtgComponentsService componentsService = ServiceManager.getService(project, AtgComponentsService.class);
        return componentsService.suggestComponentNamesByClass(srcClasses).stream()
                .map(componentsService::getComponentsByName)
                .flatMap(Collection::stream)
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
                .filter(new IsMethodIgnored(psiClass.getProject()))
                .collect(Collectors.toList());
    }

    @NotNull
    public static Optional<PsiMethod> getGetter(@NotNull PsiClass psiClass, @NotNull String key) {
        return Arrays.stream(psiClass.getAllMethods())
                .filter(m -> convertVariableToGetters(key).contains(m.getName()))
                .filter(m -> m.hasModifier(JvmModifier.PUBLIC))
                .filter(m -> !m.hasModifier(JvmModifier.ABSTRACT))
                .filter(m -> m.getParameters().length == 0)
                .findAny();
    }

    @NotNull
    public static String convertPropertyNameToSetter(@NotNull String var) {
        if (!"".equals(var)) {
            String propertyName = var;
            if (propertyName.endsWith("^") || propertyName.endsWith("+") || propertyName.endsWith("-")) {
                propertyName = propertyName.substring(0, propertyName.length() - 1);
            }
            return "set" + propertyName.substring(0, 1)
                    .toUpperCase() + propertyName.substring(1);
        } else return "set";
    }

    @NotNull
    public static List<String> convertVariableToGetters(@NotNull String var) {
        if (!"".equals(var)) {
            String suffix = var.substring(0, 1)
                    .toUpperCase() + var.substring(1);
            return Arrays.asList("get" + suffix, "is" + suffix);
        } else return Collections.emptyList();
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

    public static boolean treatAsDependencySetter(@NotNull PsiMethod m) {
        JvmType parameterType = m.getParameters()[0].getType();
        if (!(parameterType instanceof PsiClassType)) return false;
        String parameterClassName = ((PsiClassType) parameterType).getCanonicalText();
        if (parameterClassName.startsWith("java")) return false;
        if (parameterClassName.equals("atg.xml.XMLFile")) return false;
        if (parameterClassName.equals("atg.repository.rql.RqlStatement")) return false;
        if (parameterClassName.equals("atg.nucleus.ResolvingMap")) return false;
        return !parameterClassName.equals("atg.nucleus.ServiceMap");
    }

    static class IsMethodIgnored implements Predicate<PsiMethod> {
        private final List<Pattern> ignoredClassPatterns;

        IsMethodIgnored(@NotNull Project project) {
            AtgToolkitConfig atgToolkitConfig = AtgToolkitConfig.getInstance(project);
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
