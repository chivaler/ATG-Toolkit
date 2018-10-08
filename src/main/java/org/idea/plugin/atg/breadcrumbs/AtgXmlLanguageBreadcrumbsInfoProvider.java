package org.idea.plugin.atg.breadcrumbs;

import com.intellij.lang.Language;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.breadcrumbs.XmlLanguageBreadcrumbsInfoProvider;
import org.jetbrains.annotations.NotNull;

public class AtgXmlLanguageBreadcrumbsInfoProvider extends XmlLanguageBreadcrumbsInfoProvider {
    @Override
    public Language[] getLanguages() {
        return new Language[]{XMLLanguage.INSTANCE};
    }

    @NotNull
    @Override
    public String getElementInfo(@NotNull PsiElement e) {
        final XmlTag tag = (XmlTag) e;
        String tagName = tag.getName();
        String nameValue = tag.getAttributeValue("name");
        return nameValue != null ? tagName + '[' + nameValue + ']' : tagName;
    }
}
