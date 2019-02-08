package org.idea.plugin.atg.psi.reference;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.apache.commons.lang.StringUtils;
import org.idea.plugin.atg.index.AtgIndexService;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class ItemDescriptorReference extends PsiPolyVariantReferenceBase<XmlAttributeValue> {

    public ItemDescriptorReference(@NotNull XmlAttributeValue element) {
        super(element);
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return new String[0];
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        PsiFile containingFile = getElement().getContainingFile();
        if (!(containingFile instanceof XmlFile)) return ResolveResult.EMPTY_ARRAY;

        String seekingItemDescriptorName = getElement().getValue();
        if (StringUtils.isBlank(seekingItemDescriptorName)) return ResolveResult.EMPTY_ARRAY;

        Set<XmlFile> xmlFilesWithSamePath = new HashSet<>();
        xmlFilesWithSamePath.add((XmlFile) containingFile);
        AtgIndexService componentsService = ServiceManager.getService(containingFile.getProject(), AtgIndexService.class);
        Optional<String> xmlRelativePath = AtgComponentUtil.getXmlRelativePath((XmlFile) containingFile);
        xmlRelativePath.ifPresent(s -> xmlFilesWithSamePath.addAll(componentsService.getXmlsByName(s)));
        return xmlFilesWithSamePath.stream()
                .map(f -> findItemDescriptorByName(seekingItemDescriptorName, f))
                .filter(Objects::nonNull)
                .map(attr -> new PsiElementResolveResult(attr, true))
                .toArray(ResolveResult[]::new);
    }

    private XmlAttributeValue findItemDescriptorByName(@NotNull final String seekingItemDescriptorName, @NotNull final XmlFile xmlFile) {
        XmlTag rootTag = xmlFile.getRootTag();
        if (rootTag != null) {
            for (XmlTag itemDescriptorTag : rootTag.findSubTags("item-descriptor")) {
                XmlAttribute nameAttribute = itemDescriptorTag.getAttribute("name");
                if (nameAttribute != null && seekingItemDescriptorName.equals(itemDescriptorTag.getAttributeValue("name"))) {
                    return nameAttribute.getValueElement();
                }

            }
        }
        return null;
    }

}