package org.idea.plugin.atg.util;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DependenciesUpdateTask extends Task.Backgroundable {
    private Project project;

    public DependenciesUpdateTask(@Nullable Project project, @Nls(capitalization = Nls.Capitalization.Title) @NotNull String title, boolean canBeCancelled) {
        super(project, title, canBeCancelled, ALWAYS_BACKGROUND);
        this.project = project;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        AtgEnvironmentUtil.removeAtgDependenciesForAllModules(project);
        AtgEnvironmentUtil.addAtgDependenciesForAllModules(project);
    }


}
