package org.idea.plugin.atg.framework;

import com.google.common.collect.Sets;
import com.intellij.framework.FrameworkType;
import com.intellij.framework.detection.FacetBasedFrameworkDetector;
import com.intellij.framework.detection.FileContentPattern;
import com.intellij.lang.properties.PropertiesFileType;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ContentEntryEditor;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PatternCondition;
import com.intellij.util.ProcessingContext;
import com.intellij.util.indexing.FileContent;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.config.AtgConfigHelper;
import org.idea.plugin.atg.config.AtgToolkitConfig;
import org.idea.plugin.atg.index.AtgComponentsService;
import org.idea.plugin.atg.module.AtgModuleFacet;
import org.idea.plugin.atg.module.AtgModuleFacetConfiguration;
import org.idea.plugin.atg.module.AtgModuleFacetType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Pattern;

public class AtgFrameworkDetector extends FacetBasedFrameworkDetector<AtgModuleFacet, AtgModuleFacetConfiguration> {
    private static final String DETECTOR_ID = "ATG Detector";
    final LanguageFileType MANIFEST = (LanguageFileType) FileTypeManager.getInstance().getStdFileType("Manifest");

    public AtgFrameworkDetector() {
        super(DETECTOR_ID);
    }

    @NotNull
    @Override
    public AtgModuleFacetType getFacetType() {
        return AtgModuleFacetType.getInstance();
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return PropertiesFileType.INSTANCE;
    }

    @NotNull
    @Override
    public ElementPattern<FileContent> createSuitableFilePattern() {
        return FileContentPattern.fileContent().
                with(new PatternCondition<FileContent>("withContent") {
                    @Override
                    public boolean accepts(@NotNull FileContent fileContent, ProcessingContext context) {
                        return (new String(fileContent.getContent()).startsWith(Constants.Keywords.Properties.CLASS_PROPERTY + "="));
                    }
                });
    }


    @Override
    public FrameworkType getFrameworkType() {
        return AtgFrameworkType.getInstance();
    }

    @Override
    public void setupFacet(@NotNull AtgModuleFacet facet, ModifiableRootModel model) {
        AtgModuleFacetConfiguration configuration = facet.getConfiguration();
        configuration.getWebRoots().addAll(AtgConfigHelper.detectWebRootsForModule(model).keySet());

        Set<VirtualFile> excludeConfigRoots = new HashSet<>();
        Set<VirtualFile> excludeConfigLayerRoots = new HashSet<>();
        Set<VirtualFile> excludeWebRoots = new HashSet<>();

        configuration.getConfigRoots().forEach(root -> Arrays.stream(model.getContentEntries())
                .filter(contentEntry -> ContentEntryEditor.isExcludedOrUnderExcludedDirectory(model.getProject(), contentEntry, root))
                .forEach(c -> excludeConfigRoots.add(root)));
        configuration.getConfigLayerRoots().keySet().forEach(root -> Arrays.stream(model.getContentEntries())
                .filter(contentEntry -> ContentEntryEditor.isExcludedOrUnderExcludedDirectory(model.getProject(), contentEntry, root))
                .forEach(c -> excludeConfigLayerRoots.add(root)));
        configuration.getWebRoots().forEach(root -> Arrays.stream(model.getContentEntries())
                .filter(contentEntry -> ContentEntryEditor.isExcludedOrUnderExcludedDirectory(model.getProject(), contentEntry, root))
                .forEach(c -> excludeWebRoots.add(root)));


        configuration.getConfigRoots().removeAll(excludeConfigRoots);
        configuration.getConfigLayerRoots().keySet().removeAll(excludeConfigLayerRoots);
        configuration.getWebRoots().removeAll(excludeWebRoots);

        AtgComponentsService atgComponentsService = ServiceManager.getService(model.getProject(), AtgComponentsService.class);
        atgComponentsService.notifyConfigRootsChanged(configuration.getConfigRoots());
        atgComponentsService.notifyConfigRootsChanged(configuration.getConfigLayerRoots().keySet());
    }

    @NotNull
    @Override
    public AtgModuleFacetConfiguration createConfiguration(@NotNull Collection<VirtualFile> files) {
        AtgModuleFacetConfiguration atgModuleFacetConfiguration = AtgModuleFacetType.getInstance().createDefaultConfiguration();

        Optional<Project> supposedCurrentProject = Optional.empty();
        if (!files.isEmpty()) {
            VirtualFile firstFile = files.iterator().next();
            supposedCurrentProject = Arrays.stream(ProjectManager.getInstance().getOpenProjects())
                    .filter(p -> ModuleUtilCore.findModuleForFile(firstFile, p) != null)
                    .findAny();
        }
        AtgToolkitConfig atgToolkitConfig = supposedCurrentProject.map(AtgToolkitConfig::getInstance).orElseGet(AtgToolkitConfig::getInstance);

        String configRootsPatternsStr = atgToolkitConfig.getConfigRootsPatterns();
        List<Pattern> configRootsPatterns = AtgConfigHelper.convertToPatternList(configRootsPatternsStr);

        String configLayerRootsPatternsStr = atgToolkitConfig.getConfigLayerRootsPatterns();
        List<Pattern> configLayerRootsPatterns = AtgConfigHelper.convertToPatternList(configLayerRootsPatternsStr);


        for (VirtualFile file : files) {
            if (VfsUtilCore.isUnder(file, Sets.newHashSet(atgModuleFacetConfiguration.getConfigRoots()))) continue;
            if (VfsUtilCore.isUnder(file, atgModuleFacetConfiguration.getConfigLayerRoots().keySet())) continue;

            VirtualFile parent = file;
            while (parent != null) {
                final VirtualFile currentFile = parent;
                if (currentFile.getCanonicalPath() != null) {
                    if (configRootsPatterns.stream().anyMatch(p -> p.matcher(currentFile.getCanonicalPath()).find())) {
                        atgModuleFacetConfiguration.getConfigRoots().add(parent);
                        break;
                    }
                    if (configLayerRootsPatterns.stream().anyMatch(p -> p.matcher(currentFile.getCanonicalPath()).find())) {
                        atgModuleFacetConfiguration.getConfigLayerRoots().put(parent, "");
                        break;
                    }
                }
                parent = parent.getParent();
            }
        }

        return atgModuleFacetConfiguration;
    }

}
