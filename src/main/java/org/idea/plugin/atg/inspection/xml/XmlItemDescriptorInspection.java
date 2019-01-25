package org.idea.plugin.atg.inspection.xml;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.XmlAttributeValuePattern;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.Constants.Keywords.Repository;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static org.idea.plugin.atg.Constants.Keywords.Repository.PROPERTY_TYPE;

public class XmlItemDescriptorInspection extends LocalInspectionTool {

    private XmlAttributeValuePattern itemDescriptorAttributeValuePattern = XmlPatterns.xmlAttributeValue()
            .withParent(XmlPatterns.xmlAttribute().withName(Repository.NAME)
                    .withParent(XmlPatterns.xmlTag().withName(Repository.ATTRIBUTE)
                            .withParent(XmlPatterns.xmlTag().withName(Repository.PROPERTY)
                                    .andOr(XmlPatterns.xmlTag().withParent(XmlPatterns.xmlTag().withName(Repository.ITEM_DESCRIPTOR)),
                                            XmlPatterns.xmlTag().withSuperParent(2, XmlPatterns.xmlTag().withName(Repository.ITEM_DESCRIPTOR))))));

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        PsiFile psiFile = holder.getFile();
        if (psiFile instanceof XmlFile) {
            Optional<String> xmlRelativePath = AtgComponentUtil.getXmlRelativePath((XmlFile) psiFile);
            if (xmlRelativePath.isPresent()) return new PsiElementVisitor() {
                @Override
                public void visitElement(PsiElement element) {
                    if (element instanceof XmlAttributeValue) {
                        XmlAttributeValue valueElement = (XmlAttributeValue) element;
                        String keyName = valueElement.getValue();
                        if (keyName != null && !Constants.IGNORED_ATTRIBUTES_NAMES_FOR_DESCRIPTOR.contains(keyName)) {
                            if (itemDescriptorAttributeValuePattern.accepts(valueElement)) {
                                XmlTag propertyTag = (XmlTag) element.getParent().getParent().getParent();
                                if (propertyTag != null) {
                                    String propertyTagAttributeValue = propertyTag.getAttributeValue(PROPERTY_TYPE);
                                    String propertyDescriptorClass = propertyTagAttributeValue != null
                                            ? propertyTagAttributeValue
                                            : Constants.DEFAULT_ITEM_DESCRIPTOR_CLASS;
                                    GlobalSearchScope scope = GlobalSearchScope.allScope(element.getProject());
                                    PsiClass psiClass = JavaPsiFacade.getInstance(element.getProject()).findClass(propertyDescriptorClass, scope);
                                    if (psiClass != null) {
                                        Optional<PsiMethod> setter = AtgComponentUtil.getSetterByFieldName(psiClass, keyName);
                                        if (!setter.isPresent()) {
                                            String setterName = AtgComponentUtil.convertPropertyNameToSetter(keyName);
                                            holder.registerProblem(valueElement, TextRange.allOf(keyName).shiftRight(1),
                                                    AtgToolkitBundle.message("inspection.notAvailableMethod.text", setterName, propertyDescriptorClass));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            };
        }
        return PsiElementVisitor.EMPTY_VISITOR;
    }
}

