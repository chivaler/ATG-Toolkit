package org.idea.plugin.atg.config;

import com.intellij.ide.util.DirectoryUtil;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.module.AtgModuleFacet;
import org.jetbrains.annotations.NotNull;

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


}
