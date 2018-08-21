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

    public static final String ATG_TOOLKIT_CONFIGURABLE_ID = "atg-toolkit";
    public static final LayeredIcon CONFIG_ROOT_ICON;
    public static final LayeredIcon CONFIG_LAYER_ROOT_ICON;
    public static final LayeredIcon WEB_ROOT_ICON;
    public static final Pattern SUSPECTED_COMPONENT_NAME_REGEX = Pattern.compile("/[^,=]*");
    public static final String DEFAULT_ITEM_DESCRIPTOR_CLASS = "atg.adapter.gsa.GSAPropertyDescriptor";
    public static final List<String> IGNORED_ATTRIBUTES_NAMES_FOR_DESCRIPTOR = Arrays.asList("uiwritable", "uiqueryable", "resourceBundle", "deployable", "propertySortPriority", "references");

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

    public static class Keywords {
        public static final String BASED_ON_PROPERTY = "$basedOn";
        public static final String CLASS_PROPERTY = "$class";
        public static final String SCOPE_PROPERTY = "$scope";


        public static final String INCLUDE_TAG = "include";
        public static final String DROPLET_TAG = "droplet";
        public static final String IMPORT_BEAN_TAG = "importbean";
        public static final String IMG_TAG = "img";
        public static final String SCRIPT_TAG = "script";

        public static final String PAGE_ATTRIBUTE = "page";
        public static final String NAME_ATTRIBUTE = "name";
        public static final String BEAN_VALUE_ATTRIBUTE = "beanvalue";
        public static final String BEAN_ATTRIBUTE = "bean";
        public static final String SRC_ATTRIBUTE = "src";

        private Keywords() {
        }

    }

}
