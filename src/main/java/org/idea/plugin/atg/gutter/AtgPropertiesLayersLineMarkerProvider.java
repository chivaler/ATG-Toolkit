package org.idea.plugin.atg.gutter;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.lang.properties.psi.PropertiesList;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;

public class AtgPropertiesLayersLineMarkerProvider extends RelatedItemLineMarkerProvider {

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo> result) {
        if (element instanceof PropertiesList) {
            PsiFile psiFile = element.getContainingFile();
            if (psiFile instanceof PropertiesFileImpl) {
                Optional<String> componentName = AtgComponentUtil.getComponentCanonicalName((PropertiesFileImpl) psiFile);
                if (componentName.isPresent()) {
                    Collection<PropertiesFileImpl> componentsWithSameName = AtgComponentUtil.getApplicableComponentsByName(componentName.get(), element.getProject());
                    componentsWithSameName.remove(psiFile);
                    if (!componentsWithSameName.isEmpty()) {
                        NavigationGutterIconBuilder<PsiElement> builder =
                                NavigationGutterIconBuilder.create(Constants.Icons.COMPONENT_ICON).
                                        setTargets(componentsWithSameName).
                                        setTooltipText(AtgToolkitBundle.message("goto.component.layers.description", componentName.get()));
                        result.add(builder.createLineMarkerInfo(element));
                    }
                }

            }
        }
    }
}
