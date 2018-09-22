package org.idea.plugin.atg.util;

import com.intellij.openapi.help.WebHelpProvider;
import org.idea.plugin.atg.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HelpProvider extends WebHelpProvider {
    @Nullable
    @Override
    public String getHelpPageUrl(@NotNull String helpTopicId) {
        String suffix = helpTopicId.substring(Constants.PLUGIN_ID.length() + 1).replace('.', '/');
        return Constants.WEB_HELP_URL + suffix;
    }
}
