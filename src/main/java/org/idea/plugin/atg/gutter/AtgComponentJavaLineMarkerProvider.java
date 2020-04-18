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
import java.util.Collections;

public class AtgComponentJavaLineMarkerProvider extends RelatedItemLineMarkerProvider {
    //TODO show gutter icons in proper page in settings
    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo> result) {
        if (element instanceof PsiIdentifier && element.getParent() instanceof PsiClass) {
            PsiClass srcClass = (PsiClass) element.getParent();
            if (AtgComponentUtil.isApplicableToHaveComponents((PsiClass) element.getParent())) {
                String className = srcClass.getQualifiedName();
                Collection<PropertiesFileImpl> applicableComponents = AtgComponentUtil.suggestComponentsByClasses(Collections.singleton(srcClass), srcClass.getProject());
                if (!applicableComponents.isEmpty()) {
                    NavigationGutterIconBuilder<PsiElement> builder =
                            NavigationGutterIconBuilder.create(Constants.Icons.COMPONENT_ICON).
                                    setTargets(applicableComponents).
                                    setTooltipText(AtgToolkitBundle.message("goto.component.from.class.description", className));
                    result.add(builder.createLineMarkerInfo(element));
                }
            }
        }
    }
}
