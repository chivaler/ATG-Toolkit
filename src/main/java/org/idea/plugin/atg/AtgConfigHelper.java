/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

        AtgToolkitConfig atgToolkitConfig = AtgToolkitConfig.getInstance(module.getProject());
        String relativeConfigPath = atgToolkitConfig != null ? atgToolkitConfig.getRelativeConfigPath() : "";
        return moduleDefaultRoot + relativeConfigPath;
    }

    public static PsiDirectory getComponentConfigPsiDirectory(Module module, PsiPackage srcPackage) {
        PsiManager psiManager = srcPackage.getManager();
        String targetDirStr = getComponentConfigDirectory(module, srcPackage);

        return WriteCommandAction.writeCommandAction(module.getProject())
                .withName(AtgToolkitBundle.message("create.directory.command"))
                .compute(() -> DirectoryUtil.mkdirs(psiManager, targetDirStr));
    }



}
