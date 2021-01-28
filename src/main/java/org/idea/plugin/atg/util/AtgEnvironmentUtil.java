package org.idea.plugin.atg.util;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathMacros;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.StandardFileSystems;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.apache.commons.lang.StringUtils;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.Constants.Keywords.Manifest;
import org.idea.plugin.atg.module.AtgModuleFacet;
import org.idea.plugin.atg.module.AtgModuleFacetConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.lang.manifest.parser.ManifestParserDefinition;
import org.jetbrains.lang.manifest.psi.Header;
import org.jetbrains.lang.manifest.psi.HeaderValue;
import org.jetbrains.lang.manifest.psi.ManifestFile;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class AtgEnvironmentUtil {
    private static final Logger LOG = Logger.getInstance(AtgEnvironmentUtil.class);
    private static Map<String, String> moduleAliases = new HashMap<>();

    private AtgEnvironmentUtil() {
    }

    public static void parseAtgHome(@NotNull final Project project) {
        moduleAliases = new HashMap<>();
        String atgHome = PathMacros.getInstance().getValue(Constants.ATG_HOME);
        if (atgHome == null) return;
        VirtualFile atgHomeVirtualDir = StandardFileSystems.local().findFileByPath(atgHome);
        if (atgHomeVirtualDir == null) return;

        for (VirtualFile rootChild : atgHomeVirtualDir.getChildren()) {
            if (rootChild.isDirectory()) {
                String relativeModulePath = VfsUtilCore.getRelativeLocation(rootChild, atgHomeVirtualDir);
                VirtualFile manifestFile = VfsUtilCore.findRelativeFile("META-INF/MANIFEST.MF", rootChild);
                if (manifestFile != null && relativeModulePath != null) {
                    ManifestFile manifestPsiFile = getPsiForManifestFile(project, manifestFile);
                    if (manifestPsiFile != null) {
                        manifestPsiFile.getHeaders().stream()
                                .filter(h -> Manifest.ATG_INSTALL_UNIT.equals(h.getName()))
                                .map(Header::getHeaderValue)
                                .filter(Objects::nonNull)
                                .map(HeaderValue::getUnwrappedText)
                                .filter(StringUtils::isNotBlank)
                                .findAny()
                                .ifPresent(atgInstallUnitStr -> moduleAliases.put(atgInstallUnitStr, relativeModulePath + "/"));
                    }
                }
            }
        }
    }

    @Nullable
    static ManifestFile suggestManifestFileForModule(@NotNull final String atgModuleName,
                                                     @NotNull final Project project) {
        String atgHome = PathMacros.getInstance().getValue(Constants.ATG_HOME);
        if (atgHome == null) return null;
        VirtualFile atgHomeVirtualDir = StandardFileSystems.local().findFileByPath(atgHome);
        if (atgHomeVirtualDir == null) return null;

        String atgModuleMostParentModule = atgModuleName.contains(".") ?
                atgModuleName.substring(0, atgModuleName.indexOf('.')) :
                atgModuleName;

        String alias = moduleAliases.entrySet().stream()
                .filter(a -> atgModuleMostParentModule.equals(a.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("");
        String manifestRelativePath = alias + atgModuleName.replace('.', '/') + "/META-INF/MANIFEST.MF";

        VirtualFile vFile = VfsUtilCore.findRelativeFile(manifestRelativePath, atgHomeVirtualDir);
        return getPsiForManifestFile(project, vFile);
    }

    @Nullable
    private static ManifestFile getPsiForManifestFile(@NotNull Project project, @Nullable VirtualFile vFile) {
        if (vFile == null || !vFile.isValid()) {
            return null;
        }

        PsiManager psiManager = PsiManager.getInstance(project);
        PsiFile manifestPsiFile = psiManager.findFile(vFile);
        if (manifestPsiFile instanceof ManifestFile) return (ManifestFile) manifestPsiFile;

        FileViewProvider viewProvider = psiManager.findViewProvider(vFile);
        if (viewProvider == null) {
            return null;
        }
        return (ManifestFile) new ManifestParserDefinition().createFile(viewProvider);
    }

    @NotNull
    public static List<String> getRequiredModulesFromManifest(@NotNull final ManifestFile manifestFile) {
        //TODO Require-if-present
        Header requiredHeader = manifestFile.getHeader(Manifest.ATG_REQUIRED);
        if (requiredHeader != null && requiredHeader.getHeaderValue() != null) {
            return Arrays.stream(requiredHeader.getHeaderValue()
                    .getUnwrappedText()
                    .split("\\s"))
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();

    }

    @NotNull
    public static List<String> getAllRequiredModules(@NotNull final Project project,
                                                     @NotNull final List<String> modulesWithoutManifests,
                                                     @NotNull final String... loadingModules) {
        List<String> requiredList = new ArrayList<>();
        Deque<String> resolvingQueue = new LinkedList<>(Arrays.asList(loadingModules));

        while (!resolvingQueue.isEmpty()) {
            String nextName = resolvingQueue.removeFirst();
            if (!requiredList.contains(nextName)) {
                ManifestFile manifestFile = suggestManifestFileForModule(nextName, project);
                if (manifestFile == null) {
                    modulesWithoutManifests.add(nextName);
                } else {
                    List<String> requiredModulesForNextModule = getRequiredModulesFromManifest(manifestFile);
                    if (requiredList.containsAll(requiredModulesForNextModule)) {
                        requiredList.add(nextName);
                    } else {
                        resolvingQueue.addFirst(nextName);
                        for (String dependency : requiredModulesForNextModule) {
                            resolvingQueue.addFirst(dependency);
                        }
                    }
                }
            }
        }

        return requiredList;
    }

    @NotNull
    public static List<VirtualFile> getJarsForHeader(@NotNull final String header,
                                                     @NotNull final ManifestFile manifestFile) {
        VirtualFile moduleRoot = manifestFile.getVirtualFile().getParent().getParent();
        String[] configs = manifestFile.getHeaders()
                .stream()
                .filter(h -> header.equals(h.getName()))
                .map(Header::getHeaderValue)
                .filter(Objects::nonNull)
                .findAny()
                .map(h -> h.getUnwrappedText().split("\\s"))
                .orElse(new String[0]);

        return Arrays.stream(configs)
                .filter(StringUtils::isNotBlank)
                .map(c -> VfsUtilCore.findRelativeFile(c, moduleRoot))
                .filter(Objects::nonNull)
                .filter(VirtualFile::exists)
                .collect(Collectors.toList());
    }

    public static void addDependenciesToModule(@NotNull final Module module,
                                               @NotNull final LibraryTable.ModifiableModel projectLibraryModifiableModel,
                                               boolean attachConfigsOfAtgDependencies,
                                               boolean attachClassPathOfAtgDependencies,
                                               @NotNull final List<String> modulesWithoutManifests) {
        AtgModuleFacet atgModuleFacet = FacetManager.getInstance(module).getFacetByType(Constants.FACET_TYPE_ID);
        if (atgModuleFacet != null) {
            String atgModuleName = atgModuleFacet.getConfiguration().getAtgModuleName();
            if (StringUtils.isNotBlank(atgModuleName)) {
                Project project = module.getProject();
                ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(module).getModifiableModel();

                Set<String> presentModulesInProject = ProjectFacetManager.getInstance(project)
                        .getFacets(Constants.FACET_TYPE_ID).stream().map(Facet::getConfiguration)
                        .map(AtgModuleFacetConfiguration::getAtgModuleName).filter(StringUtils::isNotBlank)
                        .collect(Collectors.toSet());

                List<String> requiredModules = getAllRequiredModules(project, modulesWithoutManifests, atgModuleName);
                requiredModules.removeAll(presentModulesInProject);

                for (String requiredAtgModule : requiredModules) {
                    ManifestFile manifestFile = suggestManifestFileForModule(requiredAtgModule, project);
                    if (manifestFile != null) {
                        if (attachConfigsOfAtgDependencies) {
                            //TODO Add configLayers from jar as well as as configs
                            List<VirtualFile> configJars = getJarsForHeader(Manifest.ATG_CONFIG_PATH, manifestFile);
                            addDependenciesToModule(requiredAtgModule, configJars, modifiableModel,
                                    projectLibraryModifiableModel, Constants.ATG_CONFIG_LIBRARY_PREFIX);
                        }
                        if (attachClassPathOfAtgDependencies) {
                            List<VirtualFile> configJars = getJarsForHeader(Manifest.ATG_CLASS_PATH, manifestFile);
                            addDependenciesToModule(requiredAtgModule, configJars, modifiableModel,
                                    projectLibraryModifiableModel, Constants.ATG_CLASSES_LIBRARY_PREFIX);

                        }
                    } else {
                        modulesWithoutManifests.add(requiredAtgModule);
                    }
                }
                runWriteAction(modifiableModel::commit);
            }
        } else {
            LOG.info("Module " + module.getName()
                    + " hasn't configured AtgModuleName. Couldn't identify Manifest for module. Skipping addition of dependencies");
        }
    }


    static void addDependenciesToModule(@NotNull final String atgModuleName,
                                        @NotNull final List<VirtualFile> jarFiles,
                                        @NotNull final ModifiableRootModel modifiableModel,
                                        @NotNull final LibraryTable.ModifiableModel projectLibraryModifiableModel,
                                        @NotNull final String prefix) {
        String libraryName = prefix + atgModuleName;
        Library existingLibrary = projectLibraryModifiableModel.getLibraryByName(libraryName);

        Library atgDependencyLibrary = existingLibrary != null ? existingLibrary :
                createLibraryWithJars(libraryName, atgModuleName, jarFiles, projectLibraryModifiableModel);

        boolean dependencyAlreadyInModule = Arrays.stream(modifiableModel.getOrderEntries())
                .filter(LibraryOrderEntry.class::isInstance)
                .map(f -> ((LibraryOrderEntry) f).getLibrary())
                .anyMatch(atgDependencyLibrary::equals);
        if (!dependencyAlreadyInModule) {
            runWriteAction(() -> modifiableModel.addLibraryEntry(atgDependencyLibrary));
        }
    }

    @NotNull
    static Library createLibraryWithJars(@NotNull final String libraryName,
                                         @NotNull final String atgModuleName,
                                         @NotNull final List<VirtualFile> jarFiles,
                                         @NotNull final LibraryTable.ModifiableModel projectLibraryModifiableModel) {
        Library library = projectLibraryModifiableModel.createLibrary(libraryName);
        Library.ModifiableModel libraryModel = library.getModifiableModel();

        jarFiles.stream()
                .map(jar -> new GetRootForLibrary().apply(jar, atgModuleName))
                .filter(Objects::nonNull)
                .forEach(jar -> libraryModel.addRoot(jar, OrderRootType.CLASSES));

        runWriteAction(libraryModel::commit);
        return library;
    }

    public static void addAtgDependenciesForAllModules(@NotNull final Project project,
                                                       @NotNull final ProgressIndicator indicator,
                                                       boolean attachConfigsOfAtgDependencies,
                                                       boolean attachClassPathOfAtgDependencies,
                                                       @NotNull final List<String> modulesWithoutManifests) {
        indicator.setText(AtgToolkitBundle.message("action.update.dependencies.parsingDynamoRoot.text"));
        parseAtgHome(project);
        LibraryTable.ModifiableModel projectLibraryModifiableModel = LibraryTablesRegistrar.getInstance().getLibraryTable(project)
                .getModifiableModel();
        Module[] allModules = ModuleManager.getInstance(project).getModules();
        indicator.setText(AtgToolkitBundle.message("action.update.dependencies.attach.text"));
        int currentModuleNumber = 0;
        for (Module module : allModules) {
            indicator.setFraction(currentModuleNumber++ / (double) allModules.length);
            indicator.setText2(AtgToolkitBundle.message("action.update.dependencies.attach.text2", module.getName()));
            addDependenciesToModule(module, projectLibraryModifiableModel, attachConfigsOfAtgDependencies,
                    attachClassPathOfAtgDependencies, modulesWithoutManifests);
        }
        runWriteAction(projectLibraryModifiableModel::commit);
    }

    public static void removeAtgDependenciesForAllModules(@NotNull final Project project,
                                                          @NotNull final ProgressIndicator indicator) {
        LibraryTable.ModifiableModel projectLibraryModifiableModel = LibraryTablesRegistrar.getInstance().getLibraryTable(project).getModifiableModel();
        indicator.setText(AtgToolkitBundle.message("action.update.dependencies.removeConfigs.text"));
        removeAtgDependenciesForAllModules(project, projectLibraryModifiableModel, indicator,
                Constants.ATG_CONFIG_LIBRARY_PREFIX);
        indicator.setText(AtgToolkitBundle.message("action.update.dependencies.removeClasses.text"));
        removeAtgDependenciesForAllModules(project, projectLibraryModifiableModel, indicator,
                Constants.ATG_CLASSES_LIBRARY_PREFIX);
        runWriteAction(projectLibraryModifiableModel::commit);
    }

    static void removeAtgDependenciesForAllModules(@NotNull final Project project,
                                                   @NotNull final LibraryTable.ModifiableModel projectLibraryModifiableModel,
                                                   @NotNull final ProgressIndicator indicator,
                                                   @NotNull final String libraryPrefix) {
        Module[] allModules = ModuleManager.getInstance(project).getModules();
        int currentModuleNumber = 0;
        for (Module module : allModules) {
            indicator.setFraction(currentModuleNumber++ / (double) allModules.length);
            indicator.setText2(AtgToolkitBundle.message("action.update.dependencies.attach.text2", module.getName()));
            ModifiableRootModel moduleModifiableModel = ModuleRootManager.getInstance(module).getModifiableModel();
            Set<LibraryOrderEntry> atgLibraryEntries = Arrays.stream(moduleModifiableModel.getOrderEntries())
                    .filter(LibraryOrderEntry.class::isInstance)
                    .map(f -> (LibraryOrderEntry) f)
                    .filter(l -> l.getLibraryName() != null && l.getLibraryName().startsWith(libraryPrefix))
                    .collect(Collectors.toSet());
            if (!atgLibraryEntries.isEmpty()) {
                runWriteAction(() -> {
                    atgLibraryEntries.forEach(moduleModifiableModel::removeOrderEntry);
                    moduleModifiableModel.commit();
                });
            } else {
                moduleModifiableModel.dispose();
            }
        }

        Set<Library> projectEntries = Arrays.stream(projectLibraryModifiableModel.getLibraries())
                .filter(l -> l.getName() != null && l.getName().startsWith(libraryPrefix))
                .collect(Collectors.toSet());
        if (!projectEntries.isEmpty()) {
            runWriteAction(() -> projectEntries.forEach(projectLibraryModifiableModel::removeLibrary));
        }
    }

    public static void runWriteAction(@NotNull final Runnable runnable) {
        Application application = ApplicationManager.getApplication();
        if (application.isDispatchThread()) {
            application.runWriteAction(runnable);
        } else {
            application.invokeLater(() -> application.runWriteAction(runnable));
        }
    }

private static class GetRootForLibrary implements BiFunction<VirtualFile, String, VirtualFile> {

    @Override
    @Nullable
    public VirtualFile apply(@NotNull final VirtualFile jarReference,
                             @NotNull final String atgModuleName) {
        if (jarReference.isDirectory()) return jarReference;

        String jarReferenceName = jarReference.getName();
        VirtualFile jarConfig = JarFileSystem.getInstance().getRootByLocal(jarReference);
        if (jarConfig == null) {
            LOG.warn("Wrong config root:" + jarReferenceName + " found in MANIFEST.MF of:" + atgModuleName);
        }
        return jarConfig;
    }
}

}