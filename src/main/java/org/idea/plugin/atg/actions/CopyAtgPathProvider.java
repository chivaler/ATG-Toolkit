package org.idea.plugin.atg.actions;

import com.intellij.facet.ProjectFacetManager;
import com.intellij.ide.actions.CopyPathProvider;
import com.intellij.lang.properties.PropertiesFileType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.idea.plugin.atg.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.idea.plugin.atg.util.AtgComponentUtil.getAtgRelativeName;

public class CopyAtgPathProvider extends CopyPathProvider {
    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        Project project = e.getProject();
        if (project == null || !ProjectFacetManager.getInstance(project).hasFacets(Constants.FACET_TYPE_ID)) {
            e.getPresentation().setEnabledAndVisible(false);
        }
    }

    @Nullable
    @Override
    public String getPathToElement(@NotNull Project project, @Nullable VirtualFile virtualFile, @Nullable Editor editor) {
        if (virtualFile == null || virtualFile.getExtension() == null) return null;
        if (virtualFile.getExtension().equalsIgnoreCase(PropertiesFileType.DEFAULT_EXTENSION)) {
            return getAtgRelativeName(virtualFile, project, PropertiesFileType.DOT_DEFAULT_EXTENSION).orElse(null);
        }
        return super.getPathToElement(project, virtualFile, editor);
    }
}
