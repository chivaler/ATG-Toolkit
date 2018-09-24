package org.idea.plugin.atg.module;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import org.idea.plugin.atg.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Created by Igor_Pidgurskyi on 4/19/2017.
 */
public class AtgModuleFacetType extends FacetType<AtgModuleFacet, AtgModuleFacetConfiguration> {
    private static final AtgModuleFacetType INSTANCE = new AtgModuleFacetType();

    public static AtgModuleFacetType getInstance() {
        return INSTANCE;
    }

    private AtgModuleFacetType() {
        super(Constants.FACET_TYPE_ID, Constants.FACET_STRING_ID, Constants.FACET_PRESENTABLE_NAME);
    }

    public AtgModuleFacetConfiguration createDefaultConfiguration() {
        return new AtgModuleFacetConfiguration();
    }

    public AtgModuleFacet createFacet(@NotNull final Module module,
                                      final String name,
                                      @NotNull final AtgModuleFacetConfiguration configuration,
                                      @Nullable final Facet underlyingFacet) {
        return new AtgModuleFacet(this, module, name, configuration, underlyingFacet);
    }

    @Override
    public boolean isSuitableModuleType(ModuleType moduleType) {
        return moduleType.equals(JavaModuleType.getModuleType());
    }

    public Icon getIcon() {
        return AllIcons.General.Gear;
    }

    @Override
    public boolean isOnlyOneFacetAllowed() {
        return true;
    }
}
