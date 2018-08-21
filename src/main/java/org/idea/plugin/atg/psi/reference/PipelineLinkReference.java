package org.idea.plugin.atg.psi.reference;

import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.jgoodies.common.base.Strings;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PipelineLinkReference extends PsiPolyVariantReferenceBase<XmlAttributeValue> {

    public PipelineLinkReference(@NotNull XmlAttributeValue element) {
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
        List<ResolveResult> result = new ArrayList<>();
        if (Strings.isNotBlank(getElement().getValue())) {
            XmlTag rootTag = PsiTreeUtil.getTopmostParentOfType(getElement(), XmlTag.class);
            if (rootTag != null) {
                for (XmlTag pipelineChain : rootTag.findSubTags("pipelinechain")) {
                    for (XmlTag pipelineLink : pipelineChain.findSubTags("pipelinelink")) {
                        XmlAttribute nameAttribute = pipelineLink.getAttribute("name");
                        if (nameAttribute != null) {
                            XmlAttributeValue nameAttributeValueElement = nameAttribute.getValueElement();
                            String nameAttributeValue = nameAttribute.getValue();
                            if (nameAttributeValueElement != null && getElement().getValue().equals(nameAttributeValue)) {
                                result.add(new PsiElementResolveResult(nameAttributeValueElement, true));
                            }
                        }
                    }
                }
            }
        }
        return result.toArray(new ResolveResult[0]);
    }
}