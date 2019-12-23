package org.idea.plugin.atg.search;

import com.intellij.codeInsight.highlighting.ReadWriteAccessDetector.Access;
import com.intellij.find.findUsages.CustomUsageSearcher;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.find.findUsages.JavaMethodFindUsagesOptions;
import com.intellij.find.findUsages.JavaVariableFindUsagesOptions;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.ReadWriteAccessUsageInfo2UsageAdapter;
import com.intellij.usages.Usage;
import com.intellij.util.Processor;
import org.idea.plugin.atg.psi.reference.AccessDefinedJavaFieldPsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.idea.plugin.atg.util.AtgComponentUtil.suggestComponentNamesByClassWithInheritors;

/**
 * Extends FindUsages behaviour with seeking of Nucleus access to field via setter/getter
 */
public class FindAtgUsagesSearcher extends CustomUsageSearcher {

    @Override
    public void processElementUsages(@NotNull PsiElement element,
                                     @NotNull Processor<Usage> processor,
                                     @NotNull FindUsagesOptions options) {
        //Skip given search extension for those classes which has no defined Nucleus components, and particularly all non-ATG projects
        if (ReadAction.compute(() ->
                suggestComponentNamesByClassWithInheritors(PsiTreeUtil.getTopmostParentOfType(element, PsiClass.class))
        ).isEmpty()) return;

        Project project = PsiUtilCore.getProjectInReadAction(element);
        if (options instanceof JavaVariableFindUsagesOptions && element instanceof PsiField) {
            searchByReferencesToJavaField(processor, options, (PsiField) element, project, null);
        } else if (options instanceof JavaMethodFindUsagesOptions && element instanceof PsiMethod) {
            PsiMethod seekingMethod = (PsiMethod) element;
            Access skippedAccessType = ReadAction.compute(() -> {
                if (PropertyUtilBase.isSimplePropertySetter(seekingMethod)) return Access.Read;
                if (PropertyUtilBase.isSimplePropertyGetter(seekingMethod)) return Access.Write;
                return null;
            });
            PsiField accessingField = ReadAction.compute(() -> PropertyUtilBase.findPropertyFieldByMember(seekingMethod));
            if (accessingField != null) {
                searchByReferencesToJavaField(processor, options, accessingField, project, skippedAccessType);
            }
        }

    }

    /**
     * @param seekingField                   If search is against setter/getter - appropriate field search should be invoked instead
     * @param accessTypeOfReferencesToIgnore If search is against setter/getter - JavaReferences of opposite accesstype should be skipped
     */
    private void searchByReferencesToJavaField(@NotNull Processor<Usage> processor,
                                               @NotNull FindUsagesOptions options,
                                               @NotNull PsiField seekingField,
                                               @NotNull Project project,
                                               @Nullable Access accessTypeOfReferencesToIgnore) {
        ReferencesSearch.search(seekingField, options.searchScope, true).forEach(ref -> {
            if (ref instanceof AccessDefinedJavaFieldPsiReference) {
                Access refAccessType = ((AccessDefinedJavaFieldPsiReference) ref).getAccessType();
                if (refAccessType != accessTypeOfReferencesToIgnore) {
                    DumbService.getInstance(project).runReadActionInSmartMode(() -> {
                        //TODO Separated UsageView to distinguish Nucleus findings. see com.intellij.usages.impl.UsageViewFactory
                        UsageInfo usageInfo = new UsageInfo(ref.getElement(), ref.getRangeInElement().getStartOffset(), ref.getRangeInElement().getEndOffset(), false);
                        processor.process(new ReadWriteAccessUsageInfo2UsageAdapter(usageInfo, refAccessType != Access.Write, refAccessType != Access.Read));
                    });
                }
            }
        });
    }
}

