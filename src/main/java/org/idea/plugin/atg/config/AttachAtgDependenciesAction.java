package org.idea.plugin.atg.config;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbModeTask;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.util.AtgEnvironmentUtil;
import org.jetbrains.annotations.NotNull;

public class AttachAtgDependenciesAction extends AnAction {
    private static final String DEPENDENCIES_TASK = "DEPENDENCIES_TASK";

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project != null) {
            AtgToolkitConfig atgToolkitConfig = AtgToolkitConfig.getInstance(project);
            runProjectUpdate(project, atgToolkitConfig.isAttachClassPathOfAtgDependencies(), atgToolkitConfig.isAttachConfigsOfAtgDependencies());
        }
    }

    void runProjectUpdate(final @NotNull Project project, final boolean attachClassPathOfAtgDependencies, final boolean attachConfigsOfAtgDependencies) {
        DumbService.getInstance(project).queueTask(new DumbModeTask(DEPENDENCIES_TASK) {
            @Override
            public void performInDumbMode(@NotNull ProgressIndicator indicator) {
                ApplicationManager.getApplication().runReadAction(() -> {
                    AtgEnvironmentUtil.removeAtgDependenciesForAllModules(project, indicator);
                    if (attachConfigsOfAtgDependencies || attachClassPathOfAtgDependencies) {
                        AtgEnvironmentUtil.addAtgDependenciesForAllModules(project, indicator);
                    }
                });
                indicator.setText(AtgToolkitBundle.message("action.update.dependencies.indexing.text"));
                indicator.setText2(null);
            }
        });
    }
}
