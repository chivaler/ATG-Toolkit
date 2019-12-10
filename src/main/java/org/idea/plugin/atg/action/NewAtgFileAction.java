package org.idea.plugin.atg.action;

import com.intellij.facet.FacetManager;
import com.intellij.ide.IdeView;
import com.intellij.ide.actions.CreateFileFromTemplateAction;
import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.module.AtgModuleFacet;

import java.util.Arrays;

/**
 * @author gress on 09.12.2019
 */
public class NewAtgFileAction extends CreateFileFromTemplateAction {

    public NewAtgFileAction() {
        super(AtgToolkitBundle.message("new.atg.file.action"), AtgToolkitBundle.message("new.atg.file.action.dialog.description"), StdFileTypes.XML.getIcon());
    }

    @Override
    protected void buildDialog(Project project, PsiDirectory directory, CreateFileFromTemplateDialog.Builder builder) {
        builder.setTitle(AtgToolkitBundle.message("new.atg.file.action.dialog.title"))
                .addKind("Action file", StdFileTypes.XML.getIcon(), "Actor File.xml")
                .addKind("Repository file", StdFileTypes.XML.getIcon(), "Repository File.xml")
                .addKind("Pipeline file", StdFileTypes.XML.getIcon(), "Pipeline File.xml");
    }

    @Override
    protected String getActionName(PsiDirectory directory, String newName, String templateName) {
        return AtgToolkitBundle.message("new.atg.file.action");
    }

    @Override
    public void update(final AnActionEvent e) {
        final DataContext dataContext = e.getDataContext();
        final Presentation presentation = e.getPresentation();
        final boolean enabled = isAvailable(dataContext);
        presentation.setVisible(enabled);
        presentation.setEnabled(enabled);
    }

    @Override
    protected boolean isAvailable(final DataContext dataContext) {
        boolean available = false;

        final IdeView view = LangDataKeys.IDE_VIEW.getData(dataContext);
        final Project project = CommonDataKeys.PROJECT.getData(dataContext);
        ProjectFileIndex projectFileIndex = ProjectFileIndex.getInstance(project);

        if (project != null && view != null && view.getDirectories().length > 0) {
            available = Arrays.stream(view.getDirectories()).anyMatch(psiDirectory -> checkConfigRoot(psiDirectory.getVirtualFile(), projectFileIndex));
        }
        return available;
    }

    private boolean checkConfigRoot(VirtualFile current, ProjectFileIndex projectFileIndex) {
        Module currentModule = projectFileIndex.getModuleForFile(current);
        VirtualFile rootDir = projectFileIndex.getSourceRootForFile(current);

        AtgModuleFacet atgFacet = FacetManager.getInstance(currentModule).getFacetByType(Constants.FACET_TYPE_ID);

        boolean isConfigDir = atgFacet.getConfiguration().getConfigRoots().contains(current);
        return !isConfigDir && !current.equals(rootDir)
                ? checkConfigRoot(current.getParent(), projectFileIndex)
                : isConfigDir;
    }
}
