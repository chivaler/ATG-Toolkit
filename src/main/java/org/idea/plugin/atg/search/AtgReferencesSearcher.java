package org.idea.plugin.atg.search;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.intellij.psi.util.PsiUtilCore.getProjectInReadAction;
import static org.idea.plugin.atg.util.AtgComponentUtil.suggestComponentNamesByClassWithInheritors;

/**
 * Extends references search of java fields/methods by seeking of
 * Example mOrderTools field is known to Nucleus as orderTools, so given token should be used
 */
public class AtgReferencesSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {
    public AtgReferencesSearcher() {
        super(true);
    }

    @Override
    public void processQuery(@NotNull final ReferencesSearch.SearchParameters p, @NotNull final Processor<? super PsiReference> consumer) {
        final PsiElement element = p.getElementToSearch();
        if (!element.isValid()) return;

        final SearchScope scope = p.getEffectiveSearchScope();
        if (!(scope instanceof GlobalSearchScope)) return;
        //TODO intersect with ATG Configs only

        //Skip given search extension for those classes which has no defined Nucleus components, and particularly all non-ATG projects
        if (ReadAction.compute(() ->
                suggestComponentNamesByClassWithInheritors(PsiTreeUtil.getTopmostParentOfType(element, PsiClass.class))
        ).isEmpty()) return;

        DumbService.getInstance(getProjectInReadAction(element)).runReadActionInSmartMode(() ->
                suggestAliasesForAtgNamedPsiElement(element).forEach(name -> {
                    ProgressManager.checkCanceled();
                    p.getOptimizer().searchWord(name, scope, UsageSearchContext.IN_FOREIGN_LANGUAGES, true, element);
                }));
    }

    /**
     * @param element seeking PsiField(ex: mOrderTools) or PsiMethod(ex: getOrderTools)
     * @return alias, how this field known to Nucleus, ex: orderTools
     */
    @NotNull
    private static Set<String> suggestAliasesForAtgNamedPsiElement(PsiElement element) {
        Set<String> aliases = new HashSet<>();
        if (element instanceof PsiField) {
            PsiClass psiClass = PsiTreeUtil.getTopmostParentOfType(element, PsiClass.class);
            if (psiClass != null) {
                Arrays.stream(psiClass.getAllMethods())
                        .filter(m -> PropertyUtilBase.isSimplePropertyGetter(m) || PropertyUtilBase.isSimplePropertySetter(m))
                        .filter(m -> element.equals(PropertyUtilBase.findPropertyFieldByMember(m)))
                        .map(PropertyUtilBase::getPropertyName)
                        .filter(Objects::nonNull)
                        //Assume that if property has compliant naming, search by such name is already provided
                        .filter(alias -> !alias.equals(((PsiField) element).getName()))
                        .forEach(aliases::add);
            }
        } else if (element instanceof PsiMethod) {
            //TODO probably need to cover preXXX/postXXX/handleXXX method names for FormHandlers as well
            String propertyNameFromMethodName = PropertyUtilBase.getPropertyName((PsiMember) element);
            aliases.add(propertyNameFromMethodName);
        }
        return aliases;
    }
}
