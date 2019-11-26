package org.idea.plugin.atg.visitor;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.xml.XmlTag;

import java.util.List;

public class XmlPsiRecursiveElementVisitor extends PsiRecursiveElementVisitor {

    private final String xmlTag;
    private final List<PsiElement> psiElements;

    public XmlPsiRecursiveElementVisitor(String xmlTag, List<PsiElement> psiElements) {
        this.xmlTag = xmlTag;
        this.psiElements = psiElements;
    }

    @Override
    public void visitElement(PsiElement element) {
        super.visitElement(element);
        if(element instanceof XmlTag && ((XmlTag) element).getName().equals(xmlTag)) {
            psiElements.add(element);
        }
    }
}
