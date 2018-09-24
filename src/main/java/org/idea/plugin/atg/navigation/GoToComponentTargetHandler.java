package org.idea.plugin.atg.navigation;

import com.intellij.codeInsight.navigation.GotoTargetHandler;
import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.icons.AllIcons;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.pom.Navigatable;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.xml.XmlFileImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.psi.xml.XmlFile;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.PropertiesGenerator;
import org.idea.plugin.atg.config.AtgToolkitConfig;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;

public class GoToComponentTargetHandler extends GotoTargetHandler {

    @Override
    protected String getFeatureUsedKey() {
        return "navigation.goto.component";
    }

    @Override
    @Nullable
    protected GotoData getSourceAndTargetElements(final Editor editor, final PsiFile file) {
        if (file instanceof PsiJavaFile) {
            Optional<PsiClass> srcClass = Arrays.stream(((PsiJavaFile) file).getClasses())
                    .filter(AtgComponentUtil::isApplicableToHaveComponents)
                    .findFirst();
            if (srcClass.isPresent()) {
                List<PsiElement> candidates = new ArrayList<>(AtgComponentUtil.suggestComponentsByClass(srcClass.get()));
                List<AdditionalAction> actions = Collections.singletonList(new AdditionalAction() {
                    @NotNull
                    @Override
                    public String getText() {
                        return AtgToolkitBundle.message("intentions.create.component");
                    }

                    @Override
                    public Icon getIcon() {
                        return AllIcons.Actions.IntentionBulb;
                    }

                    @Override
                    public void execute() {
                        PropertiesGenerator.generatePropertiesFile(srcClass.get());
                    }
                });
                return new GotoData(srcClass.get(), PsiUtilCore.toPsiElementArray(candidates), actions);
            }
        } else if (file instanceof PropertiesFileImpl) {
            Optional<String> componentName = AtgComponentUtil.getComponentCanonicalName((PropertiesFile) file);
            if (componentName.isPresent()) {
                List<PsiElement> candidates = new ArrayList<>();
                if (AtgToolkitConfig.getInstance(file.getProject()).showReferencesOnComponentInGoTo) {
                    GlobalSearchScope scope = GlobalSearchScope.everythingScope(file.getProject());
                    candidates.addAll(ReferencesSearch.search(file, scope, true).findAll().stream().map(PsiReference::getElement).collect(Collectors.toList()));
                }
                if (AtgToolkitConfig.getInstance(file.getProject()).showOverridesOfComponentInGoTo) {
                    Collection<PropertiesFileImpl> componentsWithSameName = AtgComponentUtil.getApplicableComponentsByName(componentName.get(), null, file.getProject());
                    componentsWithSameName.remove(file);
                    candidates.addAll(componentsWithSameName);
                }
                return new GotoData(file, PsiUtilCore.toPsiElementArray(candidates), Collections.emptyList());
            }
        } else if (file instanceof XmlFileImpl) {
            Optional<String> xmlName = AtgComponentUtil.getXmlRelativePath((XmlFileImpl) file);
            if (xmlName.isPresent()) {
                List<PsiElement> candidates = new ArrayList<>();
                if (AtgToolkitConfig.getInstance(file.getProject()).showReferencesOnComponentInGoTo) {
                    GlobalSearchScope scope = GlobalSearchScope.everythingScope(file.getProject());
                    candidates.addAll(ReferencesSearch.search(file, scope, true).findAll().stream().map(PsiReference::getElement).collect(Collectors.toList()));
                }
                if (AtgToolkitConfig.getInstance(file.getProject()).showOverridesOfComponentInGoTo) {
                    candidates.addAll(AtgComponentUtil.getApplicableXmlsByName(xmlName.get(), file.getProject()));
                    candidates.remove(file);
                }
                return new GotoData(file, PsiUtilCore.toPsiElementArray(candidates), Collections.emptyList());
            }

        }

        return null;
    }

    @Override
    protected boolean shouldSortTargets() {
        return true;
    }

    @NotNull
    @Override
    protected String getChooserTitle(@NotNull PsiElement sourceElement, String name, int length, boolean finished) {
        String suffix = finished ? "" : " so far";
        if (sourceElement instanceof PropertiesFile) {
            String componentName = AtgComponentUtil.getComponentCanonicalName((PropertiesFile) sourceElement).orElse(name);
            return AtgToolkitBundle.message("goto.component.chooserTitle.from.component.subject", componentName, length, suffix);
        } else if (sourceElement instanceof XmlFile) {
            String componentName = AtgComponentUtil.getXmlRelativePath((XmlFile) sourceElement).orElse(name);
            return AtgToolkitBundle.message("goto.component.chooserTitle.from.component.subject", componentName, length, suffix);
        }
        return AtgToolkitBundle.message("goto.component.chooserTitle.from.class.subject", name, length, suffix);
    }

    @NotNull
    @Override
    protected String getFindUsagesTitle(@NotNull PsiElement sourceElement, String name, int length) {
        return AtgToolkitBundle.message("goto.component.findUsages", name);
    }

    @NotNull
    @Override
    protected String getNotFoundMessage(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return AtgToolkitBundle.message("goto.component.notFound");
    }

    @Nullable
    @Override
    protected String getAdText(PsiElement source, int length) {
        return null;
    }

    @Override
    protected void navigateToElement(@NotNull Navigatable element) {
        if (element instanceof PsiElement) {
            NavigationUtil.activateFileWithPsiElement((PsiElement) element, true);
        } else {
            element.navigate(true);
        }
    }

    @NotNull
    @Override
    protected Comparator<PsiElement> createComparator(@NotNull GotoData gotoData) {
        return new Comparator<PsiElement>() {
            private final String sourceComponentName = (gotoData.source instanceof PropertiesFile) ? AtgComponentUtil.getComponentCanonicalName((PropertiesFile) gotoData.source).orElse(null) : null;

            @Override
            public int compare(PsiElement firstElement, PsiElement secondElement) {
                PropertiesFile first = PsiTreeUtil.getParentOfType(firstElement, PropertiesFileImpl.class, false);
                PropertiesFile second = PsiTreeUtil.getParentOfType(secondElement, PropertiesFileImpl.class, false);

                if (first == null && second == null) return 0;
                if (first == null && second != null) return 1;
                if (first != null && second == null) return -1;

                if (sourceComponentName != null) {
                    boolean isFirstNameSameForSource = sourceComponentName.equals(AtgComponentUtil.getComponentCanonicalName(first).orElse(null));
                    boolean isSecondNameSameForSource = sourceComponentName.equals(AtgComponentUtil.getComponentCanonicalName(second).orElse(null));

                    if (isFirstNameSameForSource && !isSecondNameSameForSource) return -1;
                    if (!isFirstNameSameForSource && isSecondNameSameForSource) return 1;
                }

                boolean isFirstInLibrary = first.getContainingFile().getVirtualFile().getFileSystem() instanceof JarFileSystem;
                boolean isSecondInLibrary = second.getContainingFile().getVirtualFile().getFileSystem() instanceof JarFileSystem;
                if (isFirstInLibrary && !isSecondInLibrary) return 1;
                if (!isFirstInLibrary && isSecondInLibrary) return -1;

                boolean isFirstHaveClassProperty = first.findPropertyByKey(Constants.Keywords.Properties.CLASS_PROPERTY) != null;
                boolean isSecondHaveClassProperty = second.findPropertyByKey(Constants.Keywords.Properties.CLASS_PROPERTY) != null;
                if (isFirstHaveClassProperty && !isSecondHaveClassProperty) return -1;
                if (!isFirstHaveClassProperty && isSecondHaveClassProperty) return 1;

                return 0;
            }
        };

    }

}
