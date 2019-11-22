package org.idea.plugin.atg;

import com.google.common.collect.ImmutableList;
import com.intellij.facet.FacetTypeId;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.ScalableIcon;
import com.intellij.ui.LayeredIcon;
import com.intellij.util.PlatformIcons;
import org.idea.plugin.atg.module.AtgModuleFacet;

import javax.swing.*;
import java.util.List;
import java.util.regex.Pattern;

@SuppressWarnings({"WeakerAccess", "squid:S1118"})
public class Constants {

    public static final String PLUGIN_ID = "atg-toolkit"; //don't change this in future releases
    public static final String NOTIFICATION_GROUP_ID = PLUGIN_ID; //don't change this in future releases
    public static final String ATG_TOOLKIT_CONFIGURABLE_ID = PLUGIN_ID;

    public static final FacetTypeId<AtgModuleFacet> FACET_TYPE_ID = new FacetTypeId<>("config");
    public static final String FACET_STRING_ID = "AtgModuleConfiguration"; //don't change this in future releases
    public static final String FACET_PRESENTABLE_NAME = "ATG Facet"; //don't change this in future releases

    public static final Pattern SUSPECTED_COMPONENT_NAME_REGEX = Pattern.compile("(?<=(^|\\s|=|,))/[^,=\\\\\\s]+");
    public static final Pattern SUSPECTED_REFERENCE_NAME_REGEX = Pattern.compile("(?<=(^|\\s|=|,))/[^,=\\\\\\s]+");
    public static final String DEFAULT_ITEM_DESCRIPTOR_CLASS = "atg.adapter.gsa.GSAPropertyDescriptor";
    public static final List<String> IGNORED_ATTRIBUTES_NAMES_FOR_DESCRIPTOR = ImmutableList.of("uiwritable", "uiqueryable", "resourceBundle", "deployable", "propertySortPriority", "references", "serialize", "useCodeForValue", "stringEnumProvider", "unique");
    public static final String WEB_HELP_URL = "https://github.com/chivaler/ATG-Toolkit/wiki/";
    public static final String ATG_LIBRARY_SEPARATOR = ":";
    public static final String ATG_CONFIG_LIBRARY_PREFIX = "ATGConfig" + ATG_LIBRARY_SEPARATOR;
    public static final String ATG_CLASSES_LIBRARY_PREFIX = "ATG" + ATG_LIBRARY_SEPARATOR;
    public static final String ATG_HOME = "ATG_HOME";
    public static final String DYNAMO_ROOT = "DYNAMO_ROOT";
    public static final String DYNAMO_HOME = "DYNAMO_HOME";

    public static class Icons {
        public static final Icon FACET_ICON = AllIcons.General.GearPlain;
        public static final Icon COMPONENT_ICON = AllIcons.General.GearPlain;
        public static final LayeredIcon CONFIG_ROOT_ICON;
        public static final LayeredIcon CONFIG_LAYER_ROOT_ICON;
        public static final LayeredIcon WEB_ROOT_ICON;

        private static final float LAYERED_ICON_SCALE_FACTOR = 0.75F;

        static {
            CONFIG_ROOT_ICON = new LayeredIcon(2);
            CONFIG_ROOT_ICON.setIcon(PlatformIcons.FOLDER_ICON, 0);
            CONFIG_ROOT_ICON.setIcon(((ScalableIcon) AllIcons.Nodes.Artifact).scale(LAYERED_ICON_SCALE_FACTOR), 1, SwingConstants.SOUTH_EAST);

            CONFIG_LAYER_ROOT_ICON = new LayeredIcon(3);
            CONFIG_LAYER_ROOT_ICON.setIcon(PlatformIcons.FOLDER_ICON, 0);
            CONFIG_LAYER_ROOT_ICON.setIcon(((ScalableIcon) AllIcons.Nodes.Artifact).scale(LAYERED_ICON_SCALE_FACTOR), 1, SwingConstants.SOUTH_EAST);
            CONFIG_LAYER_ROOT_ICON.setIcon(((ScalableIcon) AllIcons.Welcome.CreateNewProject).scale(0.5f), 2, SwingConstants.NORTH_WEST);

            WEB_ROOT_ICON = new LayeredIcon(2);
            WEB_ROOT_ICON.setIcon(PlatformIcons.FOLDER_ICON, 0);
            WEB_ROOT_ICON.setIcon(((ScalableIcon) AllIcons.General.Web).scale(LAYERED_ICON_SCALE_FACTOR), 1, SwingConstants.SOUTH_EAST);
        }
    }

    public static class Keywords {

        public static class Actor {
            public static final String NAME = "name";
            public static final String ACTOR = "actor";
            public static final String COMPONENT = "component";
            public static final String DROPLET = "droplet";
            public static final String FORM = "form";
        }

        public static class Manifest {
            public static final String ATG_CONFIG_PATH = "ATG-Config-Path";
            public static final String ATG_CLASS_PATH = "ATG-Class-Path";
            public static final String ATG_REQUIRED = "ATG-Required";
            public static final String ATG_VERSION = "ATG-Version";
            public static final String ATG_INSTALL_UNIT = "ATG-Install-Unit";
        }

        public static class Repository {
            public static final String ITEM_DESCRIPTOR = "item-descriptor";
            public static final String NAME = "name";
            public static final String PROPERTY = "property";
            public static final String SUPER_TYPE = "super-type";
            public static final String ATTRIBUTE = "attribute";
            public static final String VALUE = "value";
            public static final String BEAN = "bean";
            public static final String PROPERTY_TYPE = "property-type";
            public static final String COMPONENT_ITEM_TYPE = "component-item-type";
            public static final String EDITOR_CLASS = "editor-class";
            public static final String REPOSITORY = "repository";
        }

        public static class Properties {
            public static final List<String> AVAILABLE_SCOPES = ImmutableList.of(
                    Scope.GLOBAL, Scope.SESSION, Scope.REQUEST, Scope.WINDOW, Scope.PROTOTYPE);
            public static final String BASED_ON_PROPERTY = "$basedOn";
            public static final String CLASS_PROPERTY = "$class";
            public static final String SCOPE_PROPERTY = "$scope";
        }

        public static class Java {
            public static final List<String> NUCLEUS_REFERENCES = ImmutableList.of("atg.nucleus.GenericReference", "atg.nucleus.ContextualSuffixGenericReference", "atg.nucleus.StaticMethodReference", "atg.nucleus.JNDIReference");
        }

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
    }

    public static class HelpTopics {
        public static final String MODULE_FACET_EDITOR = PLUGIN_ID + ".Facet-Settings";
        public static final String PLUGIN_CONFIGURABLE_EDITOR = PLUGIN_ID + ".Project-Settings";
    }

    public static class Scope {
        public static final String GLOBAL = "global";
        public static final String SESSION = "session";
        public static final String WINDOW = "window";
        public static final String REQUEST = "request";
        public static final String PROTOTYPE = "prototype";
    }

}
