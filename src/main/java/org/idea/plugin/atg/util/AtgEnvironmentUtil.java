package org.idea.plugin.atg.util;

import com.intellij.facet.FacetManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
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
import org.idea.plugin.atg.module.AtgModuleFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.lang.manifest.psi.Header;
import org.jetbrains.lang.manifest.psi.ManifestFile;

import java.util.*;
import java.util.stream.Collectors;

public class AtgEnvironmentUtil {
    private static final Logger LOG = Logger.getInstance(AtgEnvironmentUtil.class);

    public static final String ATG_CONFIG_PATH = "ATG-Config-Path";
    public static final String ATG_REQUIRED = "ATG-Required";
    public static final String ATG_VERSION = "ATG-Version";

    private static final List<String> moduleRoots = Arrays
            .asList(System.getenv("ATG_HOME"), System.getenv("ATG_HOME") + "/CSC10.1.2/",
                    System.getenv("ATG_HOME") + "/CAF10.1.2/", System.getenv("ATG_HOME") + "/CSC-UI10.1.2/",
                    System.getenv("ATG_HOME") + "/Search10.1.2/",
                    System.getenv("ATG_HOME") + "/Service10.1.2/",
                    System.getenv("ATG_HOME") + "/Service-UI10.1.2/",
                    System.getenv("ATG_HOME") + "/CommerceReferenceStore/",
                    "/home/atg/Git/KITS-App_ATG-Dev/");

    private AtgEnvironmentUtil() {
    }

    @NotNull
    public static Optional<ManifestFile> suggestManifestFileForModule(@NotNull final String atgModuleName, @NotNull final Project project) {
        String atgHome = System.getenv("ATG_HOME");
        VirtualFile atgHomeVirtualDir = StandardFileSystems.local().findFileByPath(atgHome);
        if (atgHomeVirtualDir != null && atgHomeVirtualDir.isDirectory()) {
            VirtualFile manifestFile = VfsUtilCore.findRelativeFile(atgModuleName.replace('.', '/') + "/META-INF/MANIFEST.MF", atgHomeVirtualDir);
            PsiFile manifestPsiFile = manifestFile != null ? PsiManager.getInstance(project).findFile(manifestFile) : null;
            if (manifestPsiFile instanceof ManifestFile) return Optional.of((ManifestFile) manifestPsiFile);
        }

        return Optional.empty();
    }

    @NotNull
    public static Optional<ManifestFile> suggestManifestFileForModule(@NotNull final Module module) {
        AtgModuleFacet atgModuleFacet = FacetManager.getInstance(module).getFacetByType(AtgModuleFacet.FACET_TYPE_ID);
        if (atgModuleFacet != null) {
            String atgHome = System.getenv("ATG_HOME");
            VirtualFile atgHomeVirtualDir = StandardFileSystems.local().findFileByPath(atgHome);
            if (atgHomeVirtualDir != null && atgHomeVirtualDir.isDirectory()) {

                String moduleName = suggestAtgModuleName(module);

                VirtualFile manifestFile = VfsUtilCore.findRelativeFile(moduleName + "/META-INF/MANIFEST.MF", atgHomeVirtualDir);
                PsiFile manifestPsiFile = manifestFile != null ? PsiManager.getInstance(module.getProject()).findFile(manifestFile) : null;
                if (manifestPsiFile instanceof ManifestFile) return Optional.of((ManifestFile) manifestPsiFile);
            }


        }
        return Optional.empty();
    }

    @NotNull
    public static String suggestAtgModuleName(@NotNull Module module) {
        VirtualFile projectRoot = module.getProject().getBaseDir();
        String moduleName = projectRoot.getName();
        ContentEntry[] moduleContentEntries = ModuleRootManager.getInstance(module).getContentEntries();
        if (moduleContentEntries.length > 0) {
            VirtualFile contentEntryRoot = moduleContentEntries[0].getFile();
            if (contentEntryRoot != null && !projectRoot.equals(contentEntryRoot)) {
                moduleName = VfsUtilCore.getRelativePath(contentEntryRoot, projectRoot);
            }
        }
        return moduleName != null ? moduleName.replace('/', '.').replace('\\', '.') : "";
    }

    @NotNull
    public static List<String> getRequiredModules(@NotNull final String atgModuleName, @NotNull final Project project) {
        Optional<ManifestFile> manifestFile = suggestManifestFileForModule(atgModuleName, project);
        if (manifestFile.isPresent()) {
            Header requiredHeader = manifestFile.get().getHeader(ATG_REQUIRED);
            if (requiredHeader != null && requiredHeader.getHeaderValue() != null) {
                return Arrays.stream(requiredHeader.getHeaderValue().getUnwrappedText().split("\\s"))
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    public static List<String> getRequiredModules(@NotNull final Project project, @NotNull String... loadingModules) {
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
    public static List<String> getRequiredModules(@NotNull final Module module) {
        return getRequiredModules(module.getProject(), suggestAtgModuleName(module));
    }

    @NotNull
    public static List<VirtualFile> getConfigs(@NotNull final String atgModuleName, @NotNull final Project project) {
        Optional<ManifestFile> manifestFile = suggestManifestFileForModule(atgModuleName, project);
        if (manifestFile.isPresent()) {
            Header configPathHeader = manifestFile.get().getHeader(ATG_CONFIG_PATH);
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

    @NotNull
    public static List<VirtualFile> getConfigs(@NotNull final Module module) {
        return getConfigs(suggestAtgModuleName(module), module.getProject());
    }

    public static void addDependantConfigs(@NotNull final Module module) {
        String atgModuleName = suggestAtgModuleName(module);
        List<String> requiredModules = getRequiredModules(module);
        requiredModules.remove(atgModuleName);

        ApplicationManager.getApplication().runWriteAction(() ->
                requiredModules.forEach(m -> {
                    VirtualFile[] moduleConfigs = getConfigs(m, module.getProject()).toArray(new VirtualFile[0]);
                    if (moduleConfigs.length > 0) addDependantConfigToModule(module, m, moduleConfigs);
                }));
    }

    public static void addDependantConfigToModule(@NotNull Module module, @NotNull String atgModuleName, @NotNull VirtualFile... configs) {
        LibraryTable projectLibraryTable = ProjectLibraryTable.getInstance(module.getProject());
        ModifiableRootModel moduleModifiableModel = ModuleRootManager.getInstance(module).getModifiableModel();
        LibraryTable.ModifiableModel libraryTableModel = projectLibraryTable.getModifiableModel();

        String libraryName = Constants.ATG_CONFIG_LIBRARY_PREFIX + atgModuleName;
        Library library = libraryTableModel.getLibraryByName(libraryName);
        if (library == null) {
            library = libraryTableModel.createLibrary(libraryName);
            Library.ModifiableModel libraryModel = library.getModifiableModel();

            for (VirtualFile config : configs) {
                if (!config.isDirectory()) {
                    VirtualFile jarConfig = JarFileSystem.getInstance().getRootByLocal(config);
                    if (jarConfig != null) {
                        config = jarConfig;
                    } else {
                        LOG.warn("Wrong config root:" + config.getName() + " found in MANIFEST.MF of:" + atgModuleName);
                        continue;
                    }
                }
                libraryModel.addRoot(config, OrderRootType.CLASSES);
            }

            libraryModel.commit();
            libraryTableModel.commit();

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
    }
}