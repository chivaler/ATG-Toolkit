package org.idea.plugin.atg.psi.reference;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlText;
import org.apache.commons.lang.StringUtils;
import org.fest.util.Lists;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PatchBayDestinationReference extends PsiPolyVariantReferenceBase<XmlText> {

    public PatchBayDestinationReference(@NotNull XmlText element) {
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

        String destinationName = getElement().getText().trim();
        if (StringUtils.isBlank(destinationName)) return ResolveResult.EMPTY_ARRAY;

        Set<XmlFile> xmlFilesWithSamePath = new HashSet<>();
        xmlFilesWithSamePath.add((XmlFile) containingFile);
        Optional<String> xmlRelativePath = AtgComponentUtil.getXmlRelativePath((XmlFile) containingFile);
        xmlRelativePath.ifPresent(s -> xmlFilesWithSamePath.addAll(AtgComponentUtil.getApplicableXmlsByName(s, containingFile.getProject())));
        return xmlFilesWithSamePath.stream()
                .map(f -> findDestinations(destinationName, f))
                .flatMap(Collection::stream)
                .map(xmlFile -> new PsiElementResolveResult(xmlFile, true))
                .toArray(ResolveResult[]::new);
    }

    @NotNull
    private List<XmlText> findDestinations(@NotNull final String destinationName, @NotNull final XmlFile xmlFile) {
        XmlTag rootTag = xmlFile.getRootTag();
        Project project = xmlFile.getProject();
        PsiManager psiManager = PsiManager.getInstance(project);
        List<XmlText> results = new ArrayList<>();
        if (rootTag != null) {
            for (XmlTag patchBay : rootTag.findSubTags("patchbay")) {
                List<XmlTag> messageTags = Lists.newArrayList(patchBay.findSubTags("message-source"));
                messageTags.addAll(Arrays.asList(patchBay.findSubTags("message-sink")));
                messageTags.addAll(Arrays.asList(patchBay.findSubTags("message-filter")));
                for (XmlTag messageTag : messageTags) {
                    List<XmlTag> searchingPorts = Lists.newArrayList(messageTag.findSubTags("output-port"));
                    searchingPorts.addAll(Arrays.asList(messageTag.findSubTags("input-port")));
                    searchingPorts.addAll(Arrays.asList(messageTag.findSubTags("redelivery-port")));
                    for (XmlTag port : searchingPorts) {
                        List<XmlTag> searchingDestinations = Lists.newArrayList(port.findSubTags("output-destination"));
                        searchingDestinations.addAll(Arrays.asList(port.findSubTags("input-destination")));
                        for (XmlTag destination : searchingDestinations) {
                            Arrays.stream(destination.findSubTags("destination-name"))
                                    .map(PsiElement::getChildren)
                                    .flatMap(Arrays::stream)
                                    .filter(f -> !psiManager.areElementsEquivalent(f, getElement()))
                                    .filter(XmlText.class::isInstance)
                                    .map(f -> (XmlText)f)
                                    .filter(f -> f.getText().trim().equals(destinationName))
                                    .forEach(results::add);
                        }
                    }
                }
            }
        }
        return results;
    }
}