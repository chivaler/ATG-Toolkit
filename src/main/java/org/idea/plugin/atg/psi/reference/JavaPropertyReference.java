package org.idea.plugin.atg.psi.reference;

import com.intellij.lang.properties.psi.impl.PropertyKeyImpl;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.xml.XmlAttributeValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class JavaPropertyReference extends PsiReferenceBase<PsiElement> {
    private String propertyName;
    private PsiClass srcClass;

    public JavaPropertyReference(@NotNull PropertyKeyImpl element, @NotNull PsiClass srcClass, @NotNull TextRange textRange) {
        super(element, textRange);
        propertyName = element.getText().substring(textRange.getStartOffset(), textRange.getEndOffset());
        this.srcClass = srcClass;
    }

    public JavaPropertyReference(@NotNull XmlAttributeValue element, @NotNull PsiClass srcClass) {
        super(element);
        propertyName = element.getValue();
        this.srcClass = srcClass;
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        return Arrays.stream(srcClass.getAllFields())
                .filter(f ->(f.getName().endsWith(propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1))
                        || f.getName().endsWith(propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1)))
                        && f.getName().length() - propertyName.length() <= 1)
                .findFirst().orElse(null);
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return new String[0];
    }

}
