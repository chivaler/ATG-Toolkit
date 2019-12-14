package org.idea.plugin.atg.psi.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPlainTextFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.xml.XmlAttributeValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AtgComponentReferenceCreator {

    public static List<PsiReference> createReferences(@NotNull XmlAttributeValue element,
                                                      @Nullable PsiFile originalFile,
                                                      @NotNull Map<String, String> importedClasses) {
        List<PsiReference> result = new ArrayList<>();
        String fullText = element.getValue();

        String shortBeanName = fullText;
        String firstField = "";
        if (fullText.contains(".")) {
            shortBeanName = fullText.substring(0, fullText.indexOf('.'));
            firstField = fullText.substring(fullText.indexOf('.') + 1);
            if (firstField.contains(".")) {
                firstField = firstField.substring(0, firstField.indexOf('.'));
            }
        }

        String foundInImports = importedClasses.get(shortBeanName);
        String beanName = foundInImports != null ? foundInImports : shortBeanName;

        TextRange fullAttributeRange = element.getValueTextRange();
        if (originalFile instanceof PsiPlainTextFile) {
            TextRange beanTextRange = new TextRange(fullAttributeRange.getStartOffset(), fullAttributeRange.getStartOffset() + shortBeanName.length());
            result.add(new AtgComponentReference(beanName, beanTextRange, originalFile));
        } else {
            TextRange beanTextRange = TextRange.from(1, shortBeanName.length());
            result.add(new AtgComponentReference(beanName, beanTextRange, element));
        }

        if (firstField.length() > 0) {
            if (originalFile instanceof PsiPlainTextFile) {
                TextRange fieldTextRange = new TextRange(fullAttributeRange.getStartOffset() + shortBeanName.length() + 1, fullAttributeRange.getStartOffset() + shortBeanName.length() + firstField.length() + 1);
                result.add(new AtgComponentFieldReference(originalFile, fieldTextRange, beanName, firstField));
            } else {
                TextRange fieldTextRange = TextRange.from(shortBeanName.length() + 2, firstField.length());
                result.add(new AtgComponentFieldReference(element, fieldTextRange, beanName, firstField));
            }
        }

        return result;

    }
}
