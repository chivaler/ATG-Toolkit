package org.idea.plugin.atg.config;

import com.intellij.ide.util.DirectoryUtil;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.module.AtgModuleFacet;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AtgConfigHelper {

    private AtgConfigHelper() {
    }

    public static PsiDirectory getComponentConfigPsiDirectory(AtgModuleFacet atgModuleFacet, PsiPackage srcPackage) {
        PsiManager psiManager = srcPackage.getManager();
        Iterator<VirtualFile> iterator = atgModuleFacet.getConfiguration().getConfigRoots().iterator();
        if (iterator.hasNext()) {
            String targetDirStr = iterator.next().getCanonicalPath();

            return targetDirStr == null ? null : WriteCommandAction.writeCommandAction(srcPackage.getProject())
                    .withName(AtgToolkitBundle.message("create.directory.command"))
                    .compute(() -> DirectoryUtil.mkdirs(psiManager, targetDirStr));
        }

        return null;
    }

    public static List<Pattern> convertToPatternList(@NotNull String str) {
        String[] patternStrArray = str.replace(".", "\\.")
                .replace("?", ".?")
                .replace("*", "[\\w-]+")
                .split("[,;]");
        return Stream.of(patternStrArray)
                .map(s -> s + "$")
                .map(Pattern::compile)
                .collect(Collectors.toList());
    }

    @NotNull
    public static List<VirtualFile> detectWebRootsForModule(ModifiableRootModel model) {
        return Arrays.stream(model.getContentEntries())
                .map(ContentEntry::getFile)
                .map(AtgConfigHelper::collectWebRoots)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private static List<VirtualFile> collectWebRoots(final VirtualFile contentEntryRoot) {
        List<VirtualFile> result = new ArrayList<>();
        VfsUtilCore.visitChildrenRecursively(contentEntryRoot, new VirtualFileVisitor() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                if (file.isDirectory()) {
                    VirtualFile webInfFolder = file.findChild("WEB-INF");
                    if (webInfFolder != null && webInfFolder.findChild("web.xml") != null) {
                        result.add(file);
                        return false;
                    }
                }
                return true;
            }
        });
        return result;
    }

    private static List<VirtualFile> collectRootsMatchedPatterns(final VirtualFile contentEntryRoot, final List<Pattern> patterns) {
        List<VirtualFile> result = new ArrayList<>();
        VfsUtilCore.visitChildrenRecursively(contentEntryRoot, new VirtualFileVisitor() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                if (file.isDirectory() && file.getCanonicalPath() != null) {
                    if (patterns.stream().anyMatch(p -> p.matcher(file.getCanonicalPath()).find())) {
                        result.add(file);
                        return false;
                    }
                }
                return true;
            }
        });
        return result;
    }

}
