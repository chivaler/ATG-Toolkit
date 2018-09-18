package org.idea.plugin.atg.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathMacros;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.StandardFileSystems;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.apache.commons.lang.StringUtils;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.config.AtgToolkitConfig;
import org.idea.plugin.atg.module.AtgModuleFacet;
import org.idea.plugin.atg.module.AtgModuleFacetConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.lang.manifest.psi.Header;
import org.jetbrains.lang.manifest.psi.ManifestFile;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AtgEnvironmentUtil {
    private static final Logger LOG = Logger.getInstance(AtgEnvironmentUtil.class);
    private static Cache<String, List<String>> dependenciesForAtgModule = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();
    private static Cache<String, Optional<ManifestFile>> manifestForModule = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();
    private static Map<String, String> moduleAliases = new HashMap<>();


    private AtgEnvironmentUtil() {
    }


    @NotNull
    public static void parseAtgHome(@NotNull final Project project) {
        moduleAliases = new HashMap<>();
        String macroAtgHome = PathMacros.getInstance().getValue(Constants.ATG_HOME);
        String atgHome = macroAtgHome != null ? macroAtgHome : System.getenv(Constants.ATG_HOME);
        VirtualFile atgHomeVirtualDir = StandardFileSystems.local().findFileByPath(atgHome);
        if (atgHomeVirtualDir != null) {
            for (VirtualFile rootChild : atgHomeVirtualDir.getChildren()) {
                if (rootChild.isDirectory()) {
                    VirtualFile metaInfFolder = rootChild.findChild("META-INF");
                    if (metaInfFolder != null) {
                        VirtualFile manifestFile = metaInfFolder.findChild("MANIFEST.MF");
                        PsiFile manifestPsiFile = null;
                        if (manifestFile != null) {
                            manifestPsiFile = PsiManager.getInstance(project).findFile(manifestFile);
                        }
                        if (manifestPsiFile instanceof ManifestFile) {
                            String relativeModulePath = VfsUtilCore.getRelativeLocation(rootChild, atgHomeVirtualDir);
                            if (relativeModulePath != null) {
                                String moduleName = relativeModulePath.replace("/", ".");
                                manifestForModule.put(moduleName, Optional.of((ManifestFile) manifestPsiFile));
                                Header atgInstallUnit = ((ManifestFile) manifestPsiFile).getHeader(Constants.Keywords.Manifest.ATG_INSTALL_UNIT);
                                if (atgInstallUnit != null && atgInstallUnit.getHeaderValue() != null) {
                                    String atgInstallUnitStr = atgInstallUnit.getHeaderValue().getUnwrappedText();
                                    if (StringUtils.isNotBlank(atgInstallUnitStr)) {
                                        moduleAliases.put(atgInstallUnitStr, moduleName);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LOG.warn("ATG_HOME folder wasn't found");
        }
    }


    @NotNull
    public static Optional<ManifestFile> suggestManifestFileForModule(@NotNull final String atgModuleName, @NotNull final Project project) {
        try {
            return manifestForModule.get(atgModuleName, () -> {
                String macroAtgHome = PathMacros.getInstance().getValue(Constants.ATG_HOME);
                String atgHome = macroAtgHome != null ? macroAtgHome : System.getenv(Constants.ATG_HOME);

                String atgModuleRelativePath = atgModuleName.replace('.', '/');
                String atgModuleMostParent = atgModuleName.contains(".") ? atgModuleName.substring(0, atgModuleName.indexOf(".")) : atgModuleName;
                Optional<String> first = moduleAliases.entrySet().stream()
                        .filter(a -> atgModuleMostParent.equals(a.getKey()))
                        .map(Map.Entry::getValue)
                        .findFirst();
                atgModuleRelativePath = first.isPresent() ? first.get() + '/' + atgModuleRelativePath : atgModuleRelativePath;

                VirtualFile atgHomeVirtualDir = StandardFileSystems.local().findFileByPath(atgHome);
                if (atgHomeVirtualDir != null && atgHomeVirtualDir.isDirectory()) {
                    VirtualFile manifestFile = VfsUtilCore.findRelativeFile(atgModuleRelativePath + "/META-INF/MANIFEST.MF", atgHomeVirtualDir);
                    PsiFile manifestPsiFile = null;
                    if (manifestFile != null) {
                        manifestPsiFile = PsiManager.getInstance(project).findFile(manifestFile);
                        LOG.info("Parsing Manifest of " + atgModuleRelativePath);
                    }
                    if (manifestPsiFile instanceof ManifestFile) return Optional.of((ManifestFile) manifestPsiFile);
                }
                return Optional.empty();
            });
        } catch (ExecutionException | UncheckedExecutionException e) {
            LOG.debug("Execution stopped");
        }
        return Optional.empty();
    }

    @NotNull
    public static List<String> getRequiredModules(@NotNull final String atgModuleName, @NotNull final Project project) {
        try {
            return dependenciesForAtgModule.get(atgModuleName, () -> {
                Optional<ManifestFile> manifestFile = suggestManifestFileForModule(atgModuleName, project);
                if (manifestFile.isPresent()) {
                    LOG.info("Reading Manifest for " + atgModuleName);
                    //TODO Require-if-present
                    Header requiredHeader = manifestFile.get().getHeader(Constants.Keywords.Manifest.ATG_REQUIRED);
                    if (requiredHeader != null && requiredHeader.getHeaderValue() != null) {
                        return Arrays.stream(requiredHeader.getHeaderValue().getUnwrappedText().split("\\s"))
                                .collect(Collectors.toList());
                    }
                } else {
                    LOG.warn("Manifest for module " + atgModuleName + " wasn't found");
                }

                return Collections.emptyList();
            });
        } catch (ExecutionException | UncheckedExecutionException e) {
            return Collections.emptyList();
        }
    }

    public static List<String> getAllRequiredModules(@NotNull final Project project, @NotNull String...
            loadingModules) {
        List<String> requiredList = new ArrayList<>();
        Deque<String> resolvingQueue = new LinkedList<>(Arrays.asList(loadingModules));

        while (!resolvingQueue.isEmpty()) {
            String nextName = resolvingQueue.removeFirst();
            if (!requiredList.contains(nextName)) {
                List<String> requiredModulesForNextModule = getRequiredModules(nextName, project);
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

        return requiredList;
    }

    @NotNull
    public static List<VirtualFile> getJarsForHeader(@NotNull final String atgModuleName,
                                                     @NotNull final Project project, @NotNull String header) {
        Optional<ManifestFile> manifestFile = suggestManifestFileForModule(atgModuleName, project);
        if (manifestFile.isPresent()) {
            LOG.info("Resolving jars for " + atgModuleName);
            Header configPathHeader = manifestFile.get().getHeader(header);
            if (configPathHeader != null && configPathHeader.getHeaderValue() != null) {
                String[] configs = configPathHeader.getHeaderValue().getUnwrappedText().split("\\s");
                VirtualFile moduleRoot = manifestFile.get().getVirtualFile().getParent().getParent();
                return Arrays.stream(configs)
                        .filter(StringUtils::isNotBlank)
                        .map(c -> VfsUtilCore.findRelativeFile(c, moduleRoot))
                        .filter(Objects::nonNull)
                        .filter(VirtualFile::exists)
                        .collect(Collectors.toList());
            }

        }
        return Collections.emptyList();
    }

    public static void addDependenciesToModule(@NotNull final Module module) {
        AtgModuleFacet atgModuleFacet = FacetManager.getInstance(module).getFacetByType(Constants.FACET_TYPE_ID);
        if (atgModuleFacet != null) {
            String atgModuleName = atgModuleFacet.getConfiguration().getAtgModuleName();
            if (StringUtils.isNotBlank(atgModuleName)) {
                Project project = module.getProject();
                List<String> requiredAtgModules = getAllRequiredModules(project, atgModuleName);
                requiredAtgModules.remove(atgModuleName);
                Set<String> presentModulesInProject = ProjectFacetManager.getInstance(project).getFacets(Constants.FACET_TYPE_ID).stream()
                        .map(Facet::getConfiguration)
                        .map(AtgModuleFacetConfiguration::getAtgModuleName)
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toSet());

                ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(module).getModifiableModel();
                ApplicationManager.getApplication().runWriteAction(() -> {
                    for (String prefix : new String[]{Constants.ATG_CONFIG_LIBRARY_PREFIX, Constants.ATG_CLASSES_LIBRARY_PREFIX}) {
                        Arrays.stream(modifiableModel.getOrderEntries())
                                .filter(LibraryOrderEntry.class::isInstance)
                                .map(f -> (LibraryOrderEntry) f)
                                .filter(f -> f.getLibraryName() != null && f.getLibraryName().startsWith(prefix))
                                .filter(f -> !requiredAtgModules.contains(f.getLibraryName().replace(prefix, "")))
                                .peek(f -> LOG.info("Removing " + f.getLibraryName() + " from module " + module.getName() + " as it's not present in ATG-Required in Manifest"))
                                .forEach(modifiableModel::removeOrderEntry);
                    }
                    requiredAtgModules.forEach(m -> {
                        if (!presentModulesInProject.contains(m)) {
                            if (AtgToolkitConfig.getInstance(project).isAttachConfigsOfAtgDependencies()) {
                                List<VirtualFile> configJars = getJarsForHeader(m, project, Constants.Keywords.Manifest.ATG_CONFIG_PATH);
                                addDependenciesToModule(module, m, configJars, Constants.ATG_CONFIG_LIBRARY_PREFIX);
                            }
                            if (AtgToolkitConfig.getInstance(project).isAttachClassPathOfAtgDependencies()) {
                                List<VirtualFile> classPathJars = getJarsForHeader(m, project, Constants.Keywords.Manifest.ATG_CLASS_PATH);
                                addDependenciesToModule(module, m, classPathJars, Constants.ATG_CLASSES_LIBRARY_PREFIX);
                            }
                        }
                    });
                    modifiableModel.commit();
                });
            } else {
                LOG.info("Module " + module.getName() + " hasn't configured AtgModuleName. Couldn't identify Manifest for module. Skipping addition of dependencies");
            }
        } else {
            LOG.info("Module " + module.getName() + " hasn't configured ATGFacet. Skipping addition of dependencies");
        }
    }

    public static void addDependenciesToModule(@NotNull Module module, @NotNull String
            atgModuleName, @NotNull List<VirtualFile> jarFiles, @NotNull String prefix) {
        LOG.info("Adding dependencies to " + module.getName());
        LibraryTable projectLibraryTable = ProjectLibraryTable.getInstance(module.getProject());
        ModifiableRootModel moduleModifiableModel = ModuleRootManager.getInstance(module).getModifiableModel();
        LibraryTable.ModifiableModel libraryTableModel = projectLibraryTable.getModifiableModel();

        String libraryName = prefix + atgModuleName;
        Library library = libraryTableModel.getLibraryByName(libraryName);
        if (library == null) {
            library = libraryTableModel.createLibrary(libraryName);
            Library.ModifiableModel libraryModel = library.getModifiableModel();

            for (VirtualFile jarFile : jarFiles) {
                if (!jarFile.isDirectory()) {
                    VirtualFile jarConfig = JarFileSystem.getInstance().getRootByLocal(jarFile);
                    if (jarConfig != null) {
                        jarFile = jarConfig;
                    } else {
                        LOG.warn("Wrong config root:" + jarFile.getName() + " found in MANIFEST.MF of:" + atgModuleName);
                        continue;
                    }
                }
                libraryModel.addRoot(jarFile, OrderRootType.CLASSES);
            }

            libraryModel.commit();
            libraryTableModel.commit();
        }

        Optional<LibraryOrderEntry> moduleLibraryEntry = Arrays.stream(moduleModifiableModel.getOrderEntries())
                .filter(LibraryOrderEntry.class::isInstance)
                .map(f -> (LibraryOrderEntry) f)
                .filter(l -> libraryName.equals(l.getLibraryName()))
                .findAny();
        if (!moduleLibraryEntry.isPresent()) {
            moduleModifiableModel.addLibraryEntry(library);
        }

    }

    public static void addAtgDependenciesForAllModules(@NotNull Project project) {
        Module[] allModules = ModuleManager.getInstance(project).getModules();
        parseAtgHome(project);
        for (int i = 0; i < allModules.length; i++) {
            ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
            if (indicator != null) {
                if (indicator.isCanceled()) return;
                indicator.setText(AtgToolkitBundle.message("update.dependencies.progress.text", allModules[i].getName()));
                indicator.setFraction((double) i / allModules.length);
            }
            addDependenciesToModule(allModules[i]);
        }
    }

    public static void removeAtgDependenciesForAllModules(@NotNull Project project) {
        if (!AtgToolkitConfig.getInstance(project).isAttachConfigsOfAtgDependencies()) {
            removeAtgDependenciesForAllModules(project, Constants.ATG_CONFIG_LIBRARY_PREFIX);
        }
        if (!AtgToolkitConfig.getInstance(project).isAttachClassPathOfAtgDependencies()) {
            removeAtgDependenciesForAllModules(project, Constants.ATG_CLASSES_LIBRARY_PREFIX);
        }
    }

    public static void removeAtgDependenciesForAllModules(@NotNull Project project, @NotNull String libraryPrefix) {
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            ModifiableRootModel moduleModifiableModel = ModuleRootManager.getInstance(module).getModifiableModel();
            Set<LibraryOrderEntry> atgLibraryEntries = Arrays.stream(moduleModifiableModel.getOrderEntries())
                    .filter(LibraryOrderEntry.class::isInstance)
                    .map(f -> (LibraryOrderEntry) f)
                    .filter(l -> l.getLibraryName() != null && l.getLibraryName().startsWith(libraryPrefix))
                    .collect(Collectors.toSet());
            if (!atgLibraryEntries.isEmpty()) {
                ApplicationManager.getApplication().runWriteAction(() -> {
                    atgLibraryEntries.forEach(moduleModifiableModel::removeOrderEntry);
                    moduleModifiableModel.commit();
                });
            }
        }

        LibraryTable projectLibraryTable = ProjectLibraryTable.getInstance(project);
        Set<Library> projectEntries = Arrays.stream(projectLibraryTable.getLibraries())
                .filter(l -> l.getName() != null && l.getName().startsWith(libraryPrefix))
                .collect(Collectors.toSet());
        if (!projectEntries.isEmpty()) {
            ApplicationManager.getApplication().runWriteAction(() -> {
                projectEntries.forEach(projectLibraryTable::removeLibrary);
                projectLibraryTable.getModifiableModel().commit();
            });
        }
    }


}