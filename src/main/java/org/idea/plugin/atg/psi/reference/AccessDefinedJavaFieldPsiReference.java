package org.idea.plugin.atg.psi.reference;

import com.intellij.codeInsight.highlighting.ReadWriteAccessDetector;
import org.jetbrains.annotations.NotNull;

public interface AccessDefinedJavaFieldPsiReference {
    @NotNull
    ReadWriteAccessDetector.Access getAccessType();
}
