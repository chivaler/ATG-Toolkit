package org.idea.plugin.atg.util;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
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
import java.util.stream.Collectors;

public class AtgEnvironmentUtil {
    private static final Logger LOG = Logger.getInstance(AtgEnvironmentUtil.class);
    private static Map<String, String> moduleAliases = new HashMap<>();

    private AtgEnvironmentUtil() {
    }

    public static void parseAtgHome(@NotNull final Project project) {
        moduleAliases = new HashMap<>();
        String macroAtgHome = PathMacros.getInstance().getValue(Constants.ATG_HOME);
        String atgHome = macroAtgHome != null ? macroAtgHome : System.getenv(Constants.ATG_HOME);
        if (StringUtils.isNotBlank(atgHome)) {
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
                                    ManifestFile manifestPsiFileImpl = (ManifestFile) manifestPsiFile;
                                    Header atgInstallUnit = manifestPsiFileImpl.getHeader(Constants.Keywords.Manifest.ATG_INSTALL_UNIT);
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
                new Notification(Constants.NOTIFICATION_GROUP_ID,
                        AtgToolkitBundle.message("inspection.atgHome.title"),
                        AtgToolkitBundle.message("inspection.atgHome.wrong.text", atgHome),
                        NotificationType.ERROR).notify(project);
            }
        } else {
            LOG.warn("ATG_HOME path variable isn't set");
            new Notification(Constants.NOTIFICATION_GROUP_ID,
                    AtgToolkitBundle.message("inspection.atgHome.title"),
                    AtgToolkitBundle.message("inspection.atgHome.notSet.text"),
                    NotificationType.ERROR).notify(project);
        }
    }

    @NotNull
    public static Optional<ManifestFile> suggestManifestFileForModule(@NotNull final String atgModuleName, @NotNull final Project project) {
        String macroAtgHome = PathMacros.getInstance().getValue(Constants.ATG_HOME);
        String atgHome = macroAtgHome != null ? macroAtgHome : System.getenv(Constants.ATG_HOME);

        String atgModuleRelativePath = atgModuleName.replace('.', '/');
        String atgModuleMostParent = atgModuleName.contains(".") ? atgModuleName.substring(0, atgModuleName.indexOf('.')) : atgModuleName;
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
                LOG.debug("Parsing Manifest of " + atgModuleRelativePath);
            }
            if (manifestPsiFile instanceof ManifestFile) return Optional.of((ManifestFile) manifestPsiFile);
        }

        return Optional.empty();
    }

    @NotNull
    public static List<String> getRequiredModules(@NotNull final String atgModuleName, @NotNull final Project project) {
        Optional<ManifestFile> manifestFile = suggestManifestFileForModule(atgModuleName, project);
        if (manifestFile.isPresent()) {
            LOG.debug("Reading Manifest for " + atgModuleName);
            //TODO Require-if-present
            Header requiredHeader = manifestFile.get().getHeader(Constants.Keywords.Manifest.ATG_REQUIRED);
            if (requiredHeader != null && requiredHeader.getHeaderValue() != null) {
                return Arrays.stream(requiredHeader.getHeaderValue().getUnwrappedText().split("\\s"))
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        } else {
            LOG.warn("Manifest for module " + atgModuleName + " wasn't found");
        }

        return Collections.emptyList();

    }

    @NotNull
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
            LOG.debug("Resolving jars for " + atgModuleName);
            String[] configs = new String[0];
            Header configPathHeader = manifestFile.get().getHeader(header);
            if (configPathHeader != null && configPathHeader.getHeaderValue() != null) {
                configs = configPathHeader.getHeaderValue().getUnwrappedText().split("\\s");
            }

            VirtualFile moduleRoot = manifestFile.get().getVirtualFile().getParent().getParent();
            return Arrays.stream(configs)
                    .filter(StringUtils::isNotBlank)
                    .map(c -> VfsUtilCore.findRelativeFile(c, moduleRoot))
                    .filter(Objects::nonNull)
                    .filter(VirtualFile::exists)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public static void addDependenciesToModule(@NotNull final Module module, LibraryTable.ModifiableModel projectLibraryModifiableModel) {
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

                for (String prefix : new String[]{Constants.ATG_CONFIG_LIBRARY_PREFIX, Constants.ATG_CLASSES_LIBRARY_PREFIX}) {
                    Arrays.stream(modifiableModel.getOrderEntries())
                            .filter(LibraryOrderEntry.class::isInstance)
                            .map(f -> (LibraryOrderEntry) f)
                            .filter(f -> f.getLibraryName() != null && f.getLibraryName().startsWith(prefix))
                            .filter(f -> !requiredAtgModules.contains(f.getLibraryName().replace(prefix, "")))
                            .peek(f -> LOG.debug("Removing " + f.getLibraryName() + " from module " + module.getName() + " as it's not present in ATG-Required in Manifest"))
                            .forEach(f -> runWriteAction(() -> modifiableModel.removeOrderEntry(f)));
                }

                requiredAtgModules.forEach(m -> {
                    if (!presentModulesInProject.contains(m)) {
                        if (AtgToolkitConfig.getInstance(project).isAttachConfigsOfAtgDependencies()) {
                            List<VirtualFile> configJars = getJarsForHeader(m, project, Constants.Keywords.Manifest.ATG_CONFIG_PATH);
                            addDependenciesToModule(module, m, configJars, modifiableModel, projectLibraryModifiableModel, Constants.ATG_CONFIG_LIBRARY_PREFIX);
                        }
                        if (AtgToolkitConfig.getInstance(project).isAttachClassPathOfAtgDependencies()) {
                            List<VirtualFile> classPathJars = getJarsForHeader(m, project, Constants.Keywords.Manifest.ATG_CLASS_PATH);
                            addDependenciesToModule(module, m, classPathJars, modifiableModel, projectLibraryModifiableModel, Constants.ATG_CLASSES_LIBRARY_PREFIX);
                        }
                    }
                });

                runWriteAction(modifiableModel::commit);
            }
        } else {
            LOG.info("Module " + module.getName() + " hasn't configured AtgModuleName. Couldn't identify Manifest for module. Skipping addition of dependencies");
        }
    }

    public static void addDependenciesToModule(@NotNull Module module,
                                               @NotNull String atgModuleName,
                                               @NotNull List<VirtualFile> jarFiles,
                                               @NotNull ModifiableRootModel modifiableModel,
                                               @NotNull LibraryTable.ModifiableModel projectLibraryModifiableModel,
                                               @NotNull String prefix) {
        LOG.debug("Adding dependencies to " + module.getName());
        Library atgDependencyLibrary = getOrCreateLibrary(atgModuleName, jarFiles, projectLibraryModifiableModel, prefix);

        boolean dependencyAlredyInModule = Arrays.stream(modifiableModel.getOrderEntries())
                .filter(LibraryOrderEntry.class::isInstance)
                .map(f -> ((LibraryOrderEntry) f).getLibrary())
                .anyMatch(atgDependencyLibrary::equals);
        if (!dependencyAlredyInModule) {
            runWriteAction(() -> modifiableModel.addLibraryEntry(atgDependencyLibrary));
        }
    }

    @NotNull
    public static Library getOrCreateLibrary(@NotNull String atgModuleName, @NotNull List<VirtualFile> jarFiles, @NotNull LibraryTable.ModifiableModel projectLibraryModifiableModel, @NotNull String prefix) {
        String libraryName = prefix + atgModuleName;
        Library existingLibrary = projectLibraryModifiableModel.getLibraryByName(libraryName);

        if (existingLibrary == null) {
            Library library = projectLibraryModifiableModel.createLibrary(libraryName);
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

            runWriteAction(libraryModel::commit);
            return library;
        } else {
            return existingLibrary;
        }
    }

    public static void addAtgDependenciesForAllModules(@NotNull Project project, ProgressIndicator indicator) {
        indicator.setText(AtgToolkitBundle.message("update.dependencies.parsingDynamoRoot.text"));
        parseAtgHome(project);
        Module[] allModules = ModuleManager.getInstance(project).getModules();
        indicator.setText(AtgToolkitBundle.message("update.dependencies.attach.text"));
        int currentModuleNumber = 0;
        LibraryTable.ModifiableModel projectLibraryModifiableModel = ProjectLibraryTable.getInstance(project).getModifiableModel();
        for (Module module : allModules) {
            indicator.setFraction(currentModuleNumber++ / (double) allModules.length);
            indicator.setText2(AtgToolkitBundle.message("update.dependencies.attach.text2", module.getName()));
            addDependenciesToModule(module, projectLibraryModifiableModel);
        }
        runWriteAction(projectLibraryModifiableModel::commit);
    }

    public static void removeAtgDependenciesForAllModules(@NotNull Project project, ProgressIndicator indicator) {
        LibraryTable.ModifiableModel projectLibraryModifiableModel = ProjectLibraryTable.getInstance(project).getModifiableModel();
        if (!AtgToolkitConfig.getInstance(project).isAttachConfigsOfAtgDependencies()) {
            indicator.setText(AtgToolkitBundle.message("update.dependencies.removeConfigs.text"));
            removeAtgDependenciesForAllModules(project, projectLibraryModifiableModel, indicator, Constants.ATG_CONFIG_LIBRARY_PREFIX);
        }
        if (!AtgToolkitConfig.getInstance(project).isAttachClassPathOfAtgDependencies()) {
            indicator.setText(AtgToolkitBundle.message("update.dependencies.removeClasses.text"));
            removeAtgDependenciesForAllModules(project, projectLibraryModifiableModel, indicator, Constants.ATG_CLASSES_LIBRARY_PREFIX);
        }
        runWriteAction(projectLibraryModifiableModel::commit);
    }

    public static void removeAtgDependenciesForAllModules(@NotNull Project project, @NotNull LibraryTable.ModifiableModel projectLibraryModifiableModel, ProgressIndicator indicator, @NotNull String libraryPrefix) {
        Module[] allModules = ModuleManager.getInstance(project).getModules();
        int currentModuleNumber = 0;
        for (Module module : allModules) {
            indicator.setFraction(currentModuleNumber++ / (double) allModules.length);
            indicator.setText2(AtgToolkitBundle.message("update.dependencies.attach.text2", module.getName()));
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

}