package org.idea.plugin.atg.util;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathMacros;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
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

    private AtgEnvironmentUtil() {
    }

    @NotNull
    public static Optional<ManifestFile> suggestManifestFileForModule(@NotNull final String atgModuleName, @NotNull final Project project) {
        String macroAtgHome = PathMacros.getInstance().getValue(Constants.ATG_HOME);
        String atgHome = macroAtgHome != null ? macroAtgHome : System.getenv(Constants.ATG_HOME);
        VirtualFile atgHomeVirtualDir = StandardFileSystems.local().findFileByPath(atgHome);
        if (atgHomeVirtualDir != null && atgHomeVirtualDir.isDirectory()) {
            VirtualFile manifestFile = VfsUtilCore.findRelativeFile(atgModuleName.replace('.', '/') + "/META-INF/MANIFEST.MF", atgHomeVirtualDir);
            PsiFile manifestPsiFile = manifestFile != null ? PsiManager.getInstance(project).findFile(manifestFile) : null;
            if (manifestPsiFile instanceof ManifestFile) return Optional.of((ManifestFile) manifestPsiFile);
        }

        return Optional.empty();
    }

    @NotNull
    public static List<String> getRequiredModules(@NotNull final String atgModuleName, @NotNull final Project project) {
        Optional<ManifestFile> manifestFile = suggestManifestFileForModule(atgModuleName, project);
        if (manifestFile.isPresent()) {
            //TODO Require-if-present
            Header requiredHeader = manifestFile.get().getHeader(Constants.Keywords.Manifest.ATG_REQUIRED);
            if (requiredHeader != null && requiredHeader.getHeaderValue() != null) {
                return Arrays.stream(requiredHeader.getHeaderValue().getUnwrappedText().split("\\s"))
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    public static List<String> getAllRequiredModules(@NotNull final Project project, @NotNull String... loadingModules) {
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
    public static List<VirtualFile> getJarsForHeader(@NotNull final String atgModuleName, @NotNull final Project project, @NotNull String header) {
        Optional<ManifestFile> manifestFile = suggestManifestFileForModule(atgModuleName, project);
        if (manifestFile.isPresent()) {
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
                                .map(f -> (LibraryOrderEntry)f)
                                .filter(f -> f.getLibraryName() != null && f.getLibraryName().startsWith(prefix))
                                .filter(f -> !requiredAtgModules.contains(f.getLibraryName().replace(prefix, "")))
                                .peek(f -> LOG.debug("Removing " + f.getLibraryName() + " from module " + module.getName() + " as it's not present in ATG-Required in Manifest"))
                                .forEach(modifiableModel::removeOrderEntry);
                    }
                    modifiableModel.commit();
                });

                ApplicationManager.getApplication().runWriteAction(() ->
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
                        }));
            } else {
                LOG.info("Module " + module.getName() + " hasn't configured AtgModuleName. Couldn't identify Manifest for module. Skipping addition of dependencies");
            }
        } else {
            LOG.info("Module " + module.getName() + " hasn't configured ATGFacet. Skipping addition of dependencies");
        }
    }

    public static void addDependenciesToModule(@NotNull Module module, @NotNull String atgModuleName, @NotNull List<VirtualFile> jarFiles, @NotNull String prefix) {
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
            moduleModifiableModel.commit();
        }

    }

    public static void addAtgDependenciesForAllModules(@NotNull Project project) {
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            addDependenciesToModule(module);
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