package org.idea.plugin.atg.psi;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiReferenceBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;

public class JavaPropertyReference extends PsiReferenceBase<PsiElement> {
    private String propertyName;
    private PsiClass srcClass;

    public JavaPropertyReference(@NotNull PsiElement element, @NotNull PsiClass srcClass, TextRange textRange) {
        super(element, textRange);
        propertyName = element.getText().substring(textRange.getStartOffset(), textRange.getEndOffset());
        if (propertyName.endsWith("^")) propertyName = propertyName.substring(0, propertyName.length() - 1);
        this.srcClass = srcClass;
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        Optional<PsiField> foundField = Arrays.stream(srcClass.getAllFields())
                .filter(f -> f.getName() != null && f.getName().endsWith(propertyName.substring(1)))
                .findFirst();

        return foundField.orElse(null);

    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return new String[0];
    }

}
