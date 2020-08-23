package org.idea.plugin.atg.gutter;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.xml.XmlFileImpl;
import com.intellij.psi.xml.XmlTag;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.index.AtgIndexService;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class AtgXmlLayersLineMarkerProvider extends RelatedItemLineMarkerProvider {

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        if (element instanceof XmlTag && !(element.getParent() instanceof XmlTag)) {
            PsiFile psiFile = element.getContainingFile();
            if (psiFile instanceof XmlFileImpl) {
                Optional<String> xmlName = AtgComponentUtil.getXmlRelativePath((XmlFileImpl) psiFile);
                if (xmlName.isPresent()) {
                    AtgIndexService componentsService = ServiceManager.getService(element.getProject(), AtgIndexService.class);
                    List<XmlFileImpl> xmlFilesWithSameName = componentsService.getXmlsByName(xmlName.get());
                    xmlFilesWithSameName.remove(psiFile);
                    if (!xmlFilesWithSameName.isEmpty()) {
                        PsiElement elementToMark = element.getFirstChild() != null ? element .getFirstChild() : element;
                        NavigationGutterIconBuilder<PsiElement> builder =
                                NavigationGutterIconBuilder.create(Constants.Icons.COMPONENT_ICON).
                                        setTargets(xmlFilesWithSameName).
                                        setTooltipText(AtgToolkitBundle.message("goto.component.layers.description", xmlName.get()));
                        result.add(builder.createLineMarkerInfo(elementToMark));
                    }
                }
            }
        }
    }
}
