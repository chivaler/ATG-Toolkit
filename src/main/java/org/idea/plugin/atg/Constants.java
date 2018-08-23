package org.idea.plugin.atg;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.ScalableIcon;
import com.intellij.ui.LayeredIcon;
import com.intellij.util.PlatformIcons;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class Constants {

    public static final String PLUGIN_ID = "atg-toolkit";
    public static final String ATG_TOOLKIT_CONFIGURABLE_ID = "atg-toolkit";
    public static final LayeredIcon CONFIG_ROOT_ICON;
    public static final LayeredIcon CONFIG_LAYER_ROOT_ICON;
    public static final LayeredIcon WEB_ROOT_ICON;
    public static final Pattern SUSPECTED_COMPONENT_NAME_REGEX = Pattern.compile("/[^,=]*");
    public static final String DEFAULT_ITEM_DESCRIPTOR_CLASS = "atg.adapter.gsa.GSAPropertyDescriptor";
    public static final List<String> IGNORED_ATTRIBUTES_NAMES_FOR_DESCRIPTOR = Arrays.asList("uiwritable", "uiqueryable", "resourceBundle", "deployable", "propertySortPriority", "references");
    public static final String WEB_HELP_URL = "https://github.com/chivaler/ATG-Toolkit/wiki/";
    public static final String ATG_CONFIG_LIBRARY_PREFIX = "ATGConfig|";
    public static final String ATG_CLASSES_LIBRARY_PREFIX = "ATGClasses|";

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

    public interface Keywords {
        String BASED_ON_PROPERTY = "$basedOn";
        String CLASS_PROPERTY = "$class";
        String SCOPE_PROPERTY = "$scope";

        String INCLUDE_TAG = "include";
        String DROPLET_TAG = "droplet";
        String IMPORT_BEAN_TAG = "importbean";
        String IMG_TAG = "img";
        String SCRIPT_TAG = "script";

        String PAGE_ATTRIBUTE = "page";
        String NAME_ATTRIBUTE = "name";
        String BEAN_VALUE_ATTRIBUTE = "beanvalue";
        String BEAN_ATTRIBUTE = "bean";
        String SRC_ATTRIBUTE = "src";
    }

    public interface HelpTopics {
        String MODULE_FACET_EDITOR = PLUGIN_ID + ".facet";
        String PLUGIN_CONFIGURABLE_EDITOR = PLUGIN_ID + ".settings";
    }

}
