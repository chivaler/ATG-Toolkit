package org.idea.plugin.atg.config;

import com.intellij.facet.Facet;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathMacros;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbModeTask;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.StandardFileSystems;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang.StringUtils;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.module.AtgModuleFacetConfiguration;
import org.idea.plugin.atg.util.AtgEnvironmentUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AttachAtgDependenciesAction extends AnAction {
    private static final String DEPENDENCIES_TASK = "DEPENDENCIES_TASK";

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project != null) {
            AtgToolkitConfig atgToolkitConfig = AtgToolkitConfig.getInstance(project);
            runProjectUpdate(project, atgToolkitConfig.isAttachClassPathOfAtgDependencies(),
                    atgToolkitConfig.isAttachConfigsOfAtgDependencies());
        }
    }

    void runProjectUpdate(@NotNull final Project project,
                          final boolean attachClassPathOfAtgDependencies,
                          final boolean attachConfigsOfAtgDependencies) {
        ProjectFacetManager projectFacetManager = ProjectFacetManager.getInstance(project);
        boolean modulesNotConfigured = projectFacetManager.getFacets(Constants.FACET_TYPE_ID).stream()
                .map(Facet::getConfiguration)
                .map(AtgModuleFacetConfiguration::getAtgModuleName)
                .noneMatch(StringUtils::isNotBlank);
        if (modulesNotConfigured) {
            new Notification(Constants.NOTIFICATION_GROUP_ID,
                    AtgToolkitBundle.message("detection.atg.title"),
                    AtgToolkitBundle.message("detection.atg.modules.not.set"),
                    NotificationType.WARNING)
                    .notify(project);
            return;
        }

        String atgHome = PathMacros.getInstance().getValue(Constants.ATG_HOME);
        if (StringUtils.isBlank(atgHome)) {
            new Notification(Constants.NOTIFICATION_GROUP_ID,
                    AtgToolkitBundle.message("detection.atg.title"),
                    AtgToolkitBundle.message("detection.atg.atgHome.notSet.text"),
                    NotificationType.ERROR)
                    .notify(project);
            return;
        }

        VirtualFile atgHomeVirtualDir = StandardFileSystems.local().findFileByPath(atgHome);
        if (atgHomeVirtualDir == null) {
            new Notification(Constants.NOTIFICATION_GROUP_ID,
                    AtgToolkitBundle.message("detection.atg.title"),
                    AtgToolkitBundle.message("detection.atg.atgHome.wrong.text", atgHome),
                    NotificationType.ERROR)
                    .notify(project);
            return;
        }

        DumbService.getInstance(project).queueTask(new DumbModeTask(DEPENDENCIES_TASK) {
            @Override
            public void performInDumbMode(@NotNull final ProgressIndicator indicator) {
                List<String> modulesWithoutManifests = new ArrayList<>();
                ApplicationManager.getApplication().runReadAction(() -> {
                    AtgEnvironmentUtil.removeAtgDependenciesForAllModules(project, indicator);
                    if (attachConfigsOfAtgDependencies || attachClassPathOfAtgDependencies) {
                        AtgEnvironmentUtil.addAtgDependenciesForAllModules(project, indicator, attachConfigsOfAtgDependencies,
                                attachClassPathOfAtgDependencies, modulesWithoutManifests);
                    }
                    indicator.setText(AtgToolkitBundle.message("action.update.dependencies.indexing.text"));
                    indicator.setText2(null);
                    List<String> notFoundPaths = modulesWithoutManifests.stream()
                            .map(s -> FileUtil.join(atgHome, s, "META-INF/MANIFEST.MF").replace('/', File.separatorChar))
                            .collect(Collectors.toList());
                    if (!notFoundPaths.isEmpty()) {
                        new Notification(Constants.NOTIFICATION_GROUP_ID,
                                AtgToolkitBundle.message("intentions.create.component"),
                                AtgToolkitBundle.message("detection.atg.atgHome.manifestsAbsent", notFoundPaths.toString()),
                                NotificationType.WARNING)
                                .notify(project);
                    }
                });
            }
        });
    }
}
