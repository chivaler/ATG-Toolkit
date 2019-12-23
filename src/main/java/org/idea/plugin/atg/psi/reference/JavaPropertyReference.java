package org.idea.plugin.atg.psi.reference;

import com.intellij.codeInsight.highlighting.ReadWriteAccessDetector.Access;
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.xml.XmlAttributeValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Reference to Java class field from ATG/Nucleus
 */
public class JavaPropertyReference extends PsiReferenceBase<PsiElement> implements AccessDefinedJavaFieldPsiReference {
    private String fieldName;
    private PsiClass srcClass;
    private Access accessType;

    public JavaPropertyReference(@NotNull PropertyKeyImpl element,
                                 @NotNull PsiClass srcClass,
                                 @NotNull TextRange textRange,
                                 @NotNull Access accessType) {
        super(element, textRange);
        fieldName = element.getText().substring(textRange.getStartOffset(), textRange.getEndOffset());
        this.srcClass = srcClass;
        this.accessType = accessType;
    }

    public JavaPropertyReference(@NotNull XmlAttributeValue element,
                                 @NotNull PsiClass srcClass) {
        super(element);
        fieldName = element.getValue();
        this.srcClass = srcClass;
        this.accessType = Access.Write;
    }

    @Nullable
    @Override
    public PsiField resolve() {
        return getPsiField(srcClass, fieldName).orElse(null);
    }

    @NotNull
    static Optional<PsiField> getPsiField(@NotNull PsiClass clazz, @NotNull String fieldName) {
        return PropertyUtilBase.getSetters(clazz, fieldName).stream()
                .map(PropertyUtilBase::findPropertyFieldByMember)
                .filter(Objects::nonNull)
                .findAny();
    }

    @NotNull
    @Override
    public Access getAccessType() {
        return accessType;
    }
}
