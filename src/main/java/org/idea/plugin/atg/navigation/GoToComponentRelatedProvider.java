package org.idea.plugin.atg.navigation;

import com.intellij.navigation.GotoRelatedItem;
import com.intellij.navigation.GotoRelatedProvider;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GoToComponentRelatedProvider extends GotoRelatedProvider {
    @NotNull
    @Override
    public List<? extends GotoRelatedItem> getItems(@NotNull DataContext context) {
        PsiFile file = CommonDataKeys.PSI_FILE.getData(context);
        if (!(file instanceof PsiClass)) return Collections.emptyList();

        return AtgComponentUtil.suggestComponentsByClassWithInheritors((PsiClass) file).stream()
                .map(c -> new GotoRelatedItem(c, "Components"))
                .collect(Collectors.toList());
    }
}
