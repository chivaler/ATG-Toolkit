package org.idea.plugin.atg;

import com.intellij.ide.util.DirectoryUtil;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import org.idea.plugin.atg.config.AtgToolkitConfig;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AtgConfigHelper {

    private AtgConfigHelper() {
    }

    public static String getComponentConfigDirectory(Module module, PsiPackage srcPackage) {
        String configRoot = getConfigRoot(module);
        String packageDir = srcPackage.getQualifiedName().replace('.', '/');
        return configRoot + packageDir;
    }

    @NotNull
    public static String getConfigRoot(Module module) {
        VirtualFile moduleDefaultRootFile = ModuleRootManager.getInstance(module).getContentEntries()[0].getFile();
        String moduleDefaultRoot = moduleDefaultRootFile != null ? moduleDefaultRootFile.getCanonicalPath() : "";

        AtgToolkitConfig atgToolkitConfig = AtgToolkitConfig.getInstance();
        String relativeConfigPath = atgToolkitConfig.getRelativeConfigPath();
        return moduleDefaultRoot + relativeConfigPath;
    }

    public static PsiDirectory getComponentConfigPsiDirectory(Module module, PsiPackage srcPackage) {
        PsiManager psiManager = srcPackage.getManager();
        String targetDirStr = getComponentConfigDirectory(module, srcPackage);

        return WriteCommandAction.writeCommandAction(module.getProject())
                .withName(AtgToolkitBundle.message("create.directory.command"))
                .compute(() -> DirectoryUtil.mkdirs(psiManager, targetDirStr));
    }

    public static List<Pattern> convertToPatternList(@NotNull String str) {
        String[] patternStrArray = str.replace(".", "\\.")
                .replace("?", ".?")
                .replace("*", ".+")
                .split("[,;]");
        return Stream.of(patternStrArray)
                .map(Pattern::compile)
                .collect(Collectors.toList());
    }


}
