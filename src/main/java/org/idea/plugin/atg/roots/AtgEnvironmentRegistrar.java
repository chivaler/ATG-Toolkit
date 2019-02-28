package org.idea.plugin.atg.roots;

import com.intellij.openapi.application.PathMacros;
import com.intellij.openapi.components.ProjectComponent;
import org.apache.commons.lang.StringUtils;
import org.idea.plugin.atg.Constants;

public class AtgEnvironmentRegistrar implements ProjectComponent {

    @Override
    public void projectOpened() {
        String systemAtgHome = System.getenv(Constants.ATG_HOME);
        String systemDynamoRoot = System.getenv(Constants.DYNAMO_ROOT);
        String systemDynamoHome = System.getenv(Constants.DYNAMO_HOME);

        PathMacros macros = PathMacros.getInstance();
        String atgHomeMacroValue = macros.getValue(Constants.ATG_HOME);
        if (atgHomeMacroValue == null) {
            if (StringUtils.isNotBlank(systemDynamoRoot)) {
                macros.setMacro(Constants.ATG_HOME, systemDynamoRoot);
            } else if (StringUtils.isNotBlank(systemAtgHome)) {
                macros.setMacro(Constants.ATG_HOME, systemAtgHome);
            } else if (StringUtils.isNotBlank(systemDynamoHome) && systemDynamoHome.endsWith("home")) {
                String dynamoRoot = systemDynamoHome.replaceAll(".home$", "");
                macros.setMacro(Constants.ATG_HOME, dynamoRoot);
            }
        }
    }
}
