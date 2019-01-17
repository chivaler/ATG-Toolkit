package org.idea.plugin.atg.gutter;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.xml.XmlFileImpl;
import com.intellij.psi.xml.XmlTag;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.render.AtgPropertiesFileRenderer;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class AtgXmlLayersLineMarkerProvider extends RelatedItemLineMarkerProvider {

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo> result) {
        if (element instanceof XmlTag && !(element.getParent() instanceof XmlTag)) {
            PsiFile psiFile = element.getContainingFile();
            if (psiFile instanceof XmlFileImpl) {
                Optional<String> xmlName = AtgComponentUtil.getXmlRelativePath((XmlFileImpl) psiFile);
                if (xmlName.isPresent()) {
                    List<XmlFileImpl> xmlFilesWithSameName = AtgComponentUtil.getApplicableXmlsByName(xmlName.get(), element.getProject());
                    xmlFilesWithSameName.remove(psiFile);
                    if (!xmlFilesWithSameName.isEmpty()) {
                        NavigationGutterIconBuilder<PsiElement> builder =
                                NavigationGutterIconBuilder.create(Constants.Icons.COMPONENT_ICON).
                                        setTargets(xmlFilesWithSameName).
                                        setTooltipText(AtgToolkitBundle.message("goto.component.layers.description", xmlName.get())).
                                        setCellRenderer(AtgPropertiesFileRenderer.INSTANCE);
                        result.add(builder.createLineMarkerInfo(element));
                    }
                }

            }
        }
    }
}
