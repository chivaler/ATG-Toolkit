package org.idea.plugin.atg.roots;

import com.intellij.ide.util.projectWizard.importSources.DetectedProjectRoot;
import com.intellij.ide.util.projectWizard.importSources.ProjectStructureDetector;
import com.intellij.openapi.util.io.FileUtil;
import org.idea.plugin.atg.AtgConfigHelper;
import org.idea.plugin.atg.config.AtgToolkitConfig;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author nik
 */
public class AtgRootsStructureDetector extends ProjectStructureDetector {
    @NotNull
    @Override
    public DirectoryProcessingResult detectRoots(@NotNull File dir,
                                                 @NotNull File[] children,
                                                 @NotNull File base,
                                                 @NotNull List<DetectedProjectRoot> result) {
        AtgToolkitConfig atgToolkitConfig = AtgToolkitConfig.getInstance();
        List<Pattern> configRootsPatters = AtgConfigHelper.convertToPatternList(atgToolkitConfig.getConfigRootsPatterns());
        String relativePath = FileUtil.getRelativePath(base, dir);
        for (Pattern configRootPattern : configRootsPatters) {
            Matcher matcher = configRootPattern.matcher(relativePath);
            if (matcher.matches()) {
//                AtgConfigurationSourceRoot sourceRoot = new JavaModuleSourceRoot(root.getFirst(), root.getSecond(), getLanguageName());
//                    result.add(sourceRoot);
            }
        }


        return DirectoryProcessingResult.PROCESS_CHILDREN;

    }


}
