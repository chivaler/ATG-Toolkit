package org.idea.plugin.atg.module;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by Igor_Pidgurskyi on 4/19/2017.
 */
public class AtgModuleFacet extends Facet<AtgModuleFacetConfiguration> {

    public static final FacetTypeId<AtgModuleFacet> FACET_TYPE_ID = new FacetTypeId<>("config");

    public AtgModuleFacet(@NotNull final FacetType facetType,
                          @NotNull final Module module,
                          final String name,
                          @NotNull final AtgModuleFacetConfiguration configuration,
                          final Facet underlyingFacet) {
        super(facetType, module, name, configuration, underlyingFacet);
    }

    /**
     * Gets the StrutsFacet for the given module.
     *
     * @param module Module to check.
     *
     * @return Instance or <code>null</code> if none configured.
     */
    @Nullable
    public static AtgModuleFacet getInstance(@NotNull final Module module) {
        return FacetManager.getInstance(module).getFacetByType(FACET_TYPE_ID);
    }

    /**
     * Gets the StrutsFacet for the module containing the given PsiElement.
     *
     * @param element Element to check.
     *
     * @return Instance or <code>null</code> if none configured.
     */
    @Nullable
    public static AtgModuleFacet getInstance(@NotNull final PsiElement element) {
        final Module module = ModuleUtil.findModuleForPsiElement(element);
        return module != null ? getInstance(module) : null;
    }

}
