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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
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
    public static Collection<VirtualFile> detectWebRootsForModule(ModifiableRootModel model) {
        List<VirtualFile> foundWebRoots = new ArrayList<>();
        for (ContentEntry contentEntry : model.getContentEntries()) {
            foundWebRoots.addAll(collectWebRoots(contentEntry.getFile()));
        }
        return foundWebRoots;
    }

    private static List<VirtualFile> collectWebRoots(final VirtualFile file) {
        List<VirtualFile> result = new ArrayList<>();
        VfsUtilCore.visitChildrenRecursively(file, new VirtualFileVisitor() {
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

}
