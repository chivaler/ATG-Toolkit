package org.idea.plugin.atg.util;

import com.intellij.facet.FacetManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
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

    @NotNull
    public static Optional<ManifestFile> suggestManifestFileForModule(@NotNull final Module module) {
        VirtualFile projectRoot = module.getProject().getBaseDir();
        AtgModuleFacet atgModuleFacet = FacetManager.getInstance(module).getFacetByType(AtgModuleFacet.FACET_TYPE_ID);
        if (atgModuleFacet != null) {
            String atgHome = System.getenv("ATG_HOME");
            VirtualFile atgHomeVirtualDir = StandardFileSystems.local().findFileByPath(atgHome);
            if (atgHomeVirtualDir != null && atgHomeVirtualDir.isDirectory()) {
                String moduleName = projectRoot.getName();

                ContentEntry[] moduleContentEntries = ModuleRootManager.getInstance(module).getContentEntries();
                if (moduleContentEntries.length > 0) {
                    VirtualFile contentEntryRoot = moduleContentEntries[0].getFile();
                    if (!projectRoot.equals(contentEntryRoot)) {
                        moduleName = VfsUtilCore.getRelativePath(contentEntryRoot, projectRoot);
                    }
                }

                VirtualFile manifestFile = VfsUtilCore.findRelativeFile(moduleName + "/META-INF/MANIFEST.MF", atgHomeVirtualDir);
                PsiFile manifestPsiFile = PsiManager.getInstance(module.getProject()).findFile(manifestFile);
                if (manifestPsiFile instanceof ManifestFile) return Optional.of((ManifestFile) manifestPsiFile);
            }


        }
        return Optional.empty();
    }

    @NotNull
    public static String[] getRequiredModules(@NotNull final Module module) {
        Optional<ManifestFile> manifestFile = suggestManifestFileForModule(module);
        if (manifestFile.isPresent()) {
            Header requiredHeader = manifestFile.get().getHeader(ATG_REQUIRED);
            if (requiredHeader != null && requiredHeader.getHeaderValue() != null) {
                return requiredHeader.getHeaderValue().getUnwrappedText().split("\\S");
            }
        }
        return new String[0];
    }

    @NotNull
    public static List<VirtualFile> getConfigs(@NotNull final Module module) {
        Optional<ManifestFile> manifestFile = suggestManifestFileForModule(module);
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

    public static void addLibraries(@NotNull final Module module, @NotNull List<VirtualFile> configs) {
        ApplicationManager.getApplication().runWriteAction(() ->
        {
            LibraryTable projectLibraryTable = ProjectLibraryTable.getInstance(module.getProject());
            ModifiableRootModel moduleModifiableModel = ModuleRootManager.getInstance(module).getModifiableModel();

            LibraryTable.ModifiableModel libraryTableModel = projectLibraryTable.getModifiableModel();
            for (VirtualFile config : configs) {
                String libraryName = Constants.ATG_CONFIG_LIBRARY_PREFIX + config.getName();
                Library library = libraryTableModel.getLibraryByName(libraryName);
                if (library == null) {
                    library = libraryTableModel.createLibrary(libraryName);
                    Library.ModifiableModel libraryModel = library.getModifiableModel();
                    if (config.isDirectory()) {
                        libraryModel.addRoot(config, OrderRootType.CLASSES);
                    } else {
                        libraryModel.addJarDirectory(config, true, OrderRootType.CLASSES);
                    }
                    libraryModel.commit();
                }
                moduleModifiableModel.addLibraryEntry(library);
            }
            libraryTableModel.commit();
            moduleModifiableModel.commit();
        });

    }
}
