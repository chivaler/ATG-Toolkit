package org.idea.plugin.atg.roots;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.PathMacros;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang.StringUtils;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.Constants;

public class AtgEnvironmentRegistrar implements ProjectComponent {
    private final Project project;

    public AtgEnvironmentRegistrar(Project project) {
        this.project = project;
    }

    @Override
    public void projectOpened() {
        String systemAtgHome = System.getenv(Constants.ATG_HOME);
        PathMacros macros = PathMacros.getInstance();
        String atgHomeMacroValue = macros.getValue(Constants.ATG_HOME);
        if (atgHomeMacroValue == null) {
            if (StringUtils.isNotBlank(systemAtgHome)) {
                macros.setMacro(Constants.ATG_HOME, systemAtgHome);
            } else {
                new Notification(Constants.NOTIFICATION_GROUP_ID,
                        AtgToolkitBundle.message("inspection.atgHome.title"),
                        AtgToolkitBundle.message("inspection.atgHome.notPresent.text"),
                        NotificationType.INFORMATION).notify(project);
            }
        }
    }
}
