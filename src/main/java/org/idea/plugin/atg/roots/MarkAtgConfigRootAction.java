package org.idea.plugin.atg.roots;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.actions.MarkRootActionBase;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ui.configuration.ModuleSourceRootEditHandler;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class MarkAtgConfigRootAction extends MarkRootActionBase {
  public MarkAtgConfigRootAction() {
    Presentation presentation = getTemplatePresentation();
    presentation.setIcon(AllIcons.Providers.Oracle);

    ModuleSourceRootEditHandler<AtgConfigurationRootProperties> handler = ModuleSourceRootEditHandler.getEditHandler(AtgConfigurationRootType.ATG_CONFIGURATION);
    if (handler == null) return;
    
    String typeName = handler.getFullRootTypeName();
    presentation.setText(typeName);
    presentation.setDescription("Mark directory as a " + typeName.toLowerCase(Locale.getDefault()));
  }

  @Override
  protected boolean isEnabled(@NotNull RootsSelection selection, @NotNull Module module) {
    return !selection.mySelectedDirectories.isEmpty();

  }

  @Override
  protected void modifyRoots(@NotNull VirtualFile vFile, @NotNull ContentEntry entry) {
    AtgConfigurationRootProperties properties = AtgConfigurationRootType.ATG_CONFIGURATION.createDefaultProperties();
    entry.addSourceFolder(vFile, AtgConfigurationRootType.ATG_CONFIGURATION, properties);
  }
}
