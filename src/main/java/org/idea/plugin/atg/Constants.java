package org.idea.plugin.atg;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.ScalableIcon;
import com.intellij.ui.LayeredIcon;
import com.intellij.util.PlatformIcons;

import javax.swing.*;

public class Constants {

    public static final LayeredIcon CONFIG_ROOT_ICON;
    public static final LayeredIcon CONFIG_LAYER_ROOT_ICON;
    public static final LayeredIcon WEB_ROOT_ICON;

    private static final float LAYERED_ICON_SCALE_FACTOR = 0.75F;

    static {
        CONFIG_ROOT_ICON = new LayeredIcon(2);
        CONFIG_ROOT_ICON.setIcon(PlatformIcons.DIRECTORY_CLOSED_ICON, 0);
        CONFIG_ROOT_ICON.setIcon(((ScalableIcon) AllIcons.Nodes.Artifact).scale(LAYERED_ICON_SCALE_FACTOR), 1, SwingConstants.SOUTH_EAST);

        CONFIG_LAYER_ROOT_ICON = new LayeredIcon(3);
        CONFIG_LAYER_ROOT_ICON.setIcon(PlatformIcons.DIRECTORY_CLOSED_ICON, 0);
        CONFIG_LAYER_ROOT_ICON.setIcon(((ScalableIcon) AllIcons.Nodes.Artifact).scale(LAYERED_ICON_SCALE_FACTOR), 1, SwingConstants.SOUTH_EAST);
        CONFIG_LAYER_ROOT_ICON.setIcon(((ScalableIcon) AllIcons.Welcome.CreateNewProject).scale(0.5f), 2, SwingConstants.NORTH_WEST);

        WEB_ROOT_ICON = new LayeredIcon(2);
        WEB_ROOT_ICON.setIcon(PlatformIcons.DIRECTORY_CLOSED_ICON, 0);
        WEB_ROOT_ICON.setIcon(((ScalableIcon) AllIcons.General.Web).scale(LAYERED_ICON_SCALE_FACTOR), 1, SwingConstants.SOUTH_EAST);
    }

    private Constants() {
    }

}
