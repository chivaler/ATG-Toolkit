package org.idea.plugin.atg.gutter;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class AtgComponentLineMarkerProvider extends RelatedItemLineMarkerProvider {

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo> result) {
        if (element instanceof PsiIdentifier && element.getParent() instanceof PsiClass &&
                AtgComponentUtil.isApplicableToHaveComponents((PsiClass) element.getParent())) {
            String className = ((PsiClass) element.getParent()).getQualifiedName();
            Collection<PropertiesFileImpl> applicableComponents = AtgComponentUtil.suggestComponentsByClass((PsiClass) element.getParent());
            NavigationGutterIconBuilder<PsiElement> builder =
                    NavigationGutterIconBuilder.create(Constants.Icons.COMPONENT_ICON).
                            setTargets(applicableComponents).
                            setTooltipText(AtgToolkitBundle.message("goto.component.from.class.description", className));
            result.add(builder.createLineMarkerInfo(element));
        }
    }
}
