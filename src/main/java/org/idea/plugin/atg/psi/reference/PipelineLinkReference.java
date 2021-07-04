package org.idea.plugin.atg.psi.reference;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.idea.plugin.atg.index.AtgIndexService;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class PipelineLinkReference extends PsiPolyVariantReferenceBase<XmlAttributeValue> {

    public PipelineLinkReference(@NotNull XmlAttributeValue element) {
        super(element);
    }

    @Override
    public Object @NotNull [] getVariants() {
        return new String[0];
    }

    @Override
    public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
        PsiFile containingFile = getElement().getContainingFile();
        if (!(containingFile instanceof XmlFile)) return ResolveResult.EMPTY_ARRAY;

        String seekingLinkName = getElement().getValue();
        PsiElement pipelineChainTag;

        PsiElement attribute = getElement().getParent();
        if (attribute instanceof XmlAttribute && "headlink".equals(((XmlAttribute) attribute).getName())) {
            pipelineChainTag = attribute.getParent();
        } else if (attribute instanceof XmlAttribute && "name".equals(((XmlAttribute) attribute).getName())) {
            PsiElement pipelineLinkTag = attribute.getParent();
            pipelineChainTag = pipelineLinkTag.getParent();
        } else {
            PsiElement transitionTag = attribute.getParent();
            PsiElement pipelineLinkTag = transitionTag.getParent();
            pipelineChainTag = pipelineLinkTag.getParent();
        }

        if (!(pipelineChainTag instanceof XmlTag)) return ResolveResult.EMPTY_ARRAY;
        String seekingChainName = ((XmlTag) pipelineChainTag).getAttributeValue("name");

        if (seekingChainName == null) return ResolveResult.EMPTY_ARRAY;

        Project project = containingFile.getProject();
        Set<XmlFile> xmlFilesWithSamePath = new HashSet<>();
        xmlFilesWithSamePath.add((XmlFile) containingFile);
        AtgIndexService componentsService = ServiceManager.getService(project, AtgIndexService.class);
        Optional<String> xmlRelativePath = AtgComponentUtil.getXmlRelativePath((XmlFile) containingFile);
        xmlRelativePath.ifPresent(s -> xmlFilesWithSamePath.addAll(componentsService.getXmlsByName(s)));
        return xmlFilesWithSamePath.stream()
                .map(f -> findPipelineLinkByName(seekingLinkName, seekingChainName, f))
                .filter(Objects::nonNull)
                .map(attr -> new PsiElementResolveResult(attr, true))
                .toArray(ResolveResult[]::new);
    }

    private XmlAttributeValue findPipelineLinkByName(@NotNull final String seekingLinkName, @NotNull final String seekingChainName, @NotNull final XmlFile xmlFile) {
        XmlTag rootTag = xmlFile.getRootTag();
        if (rootTag != null) {
            for (XmlTag pipelineChain : rootTag.findSubTags("pipelinechain")) {
                if (seekingChainName.equals(pipelineChain.getAttributeValue("name"))) {
                    for (XmlTag pipelineLink : pipelineChain.findSubTags("pipelinelink")) {
                        XmlAttribute pipelineNameAttribute = pipelineLink.getAttribute("name");
                        if (pipelineNameAttribute != null && seekingLinkName.equals(pipelineNameAttribute.getValue())) {
                            return pipelineNameAttribute.getValueElement();
                        }
                    }
                }
            }
        }
        return null;
    }
}