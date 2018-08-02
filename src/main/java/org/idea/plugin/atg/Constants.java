package org.idea.plugin.atg;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.ScalableIcon;
import com.intellij.ui.LayeredIcon;
import com.intellij.util.PlatformIcons;

import javax.swing.*;

public class Constants {

    public static final LayeredIcon CONFIG_FOLDER_ICON;
    public static final LayeredIcon CONFIG_LAYER_FOLDER_ICON;

    static {
        CONFIG_FOLDER_ICON = new LayeredIcon(2);
        CONFIG_FOLDER_ICON.setIcon(PlatformIcons.DIRECTORY_CLOSED_ICON, 0);
        CONFIG_FOLDER_ICON.setIcon(((ScalableIcon) AllIcons.Providers.Oracle).scale(0.5F), 1, SwingConstants.SOUTH_EAST);

        CONFIG_LAYER_FOLDER_ICON = new LayeredIcon(2);
        CONFIG_LAYER_FOLDER_ICON.setIcon(PlatformIcons.DIRECTORY_CLOSED_ICON, 0);
        CONFIG_LAYER_FOLDER_ICON.setIcon(((ScalableIcon) AllIcons.Providers.Oracle).scale(0.5F), 1, SwingConstants.SOUTH_EAST);
    }

    private Constants() {
    }

}
