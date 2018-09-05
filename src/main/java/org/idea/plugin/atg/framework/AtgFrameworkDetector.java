package org.idea.plugin.atg.framework;

import com.google.common.collect.Sets;
import com.intellij.framework.FrameworkType;
import com.intellij.framework.detection.FacetBasedFrameworkDetector;
import com.intellij.framework.detection.FileContentPattern;
import com.intellij.lang.properties.PropertiesFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.LanguageFileType;
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
import org.idea.plugin.atg.module.AtgModuleFacet;
import org.idea.plugin.atg.module.AtgModuleFacetConfiguration;
import org.idea.plugin.atg.module.AtgModuleFacetType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
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
        return FileContentPattern.fileContent()
                .and(FileContentPattern.fileContent().
                        with(new PatternCondition<FileContent>("withContent") {
                            @Override
                            public boolean accepts(@NotNull FileContent fileContent, ProcessingContext context) {
                                return (new String(fileContent.getContent()).startsWith(Constants.Keywords.Properties.CLASS_PROPERTY + "="));
                            }
                        }))
                .and(FileContentPattern.fileContent().
                        with(new PatternCondition<FileContent>("withContent") {
                            @Override
                            public boolean accepts(@NotNull FileContent fileContent, ProcessingContext context) {
                                String canonicalPath = fileContent.getFile().getCanonicalPath();
                                return (canonicalPath != null && canonicalPath.contains("/config/"));
                            }
                        })
                );
    }


    @Override
    public FrameworkType getFrameworkType() {
        return AtgFrameworkType.getInstance();
    }

    @Override
    public void setupFacet(@NotNull AtgModuleFacet facet, ModifiableRootModel model) {
        facet.getConfiguration().getWebRoots().putAll(AtgConfigHelper.detectWebRootsForModule(model));

        Set<VirtualFile> excludeConfigRoots = new HashSet<>();
        Set<VirtualFile> excludeConfigLayerRoots = new HashSet<>();
        Set<VirtualFile> excludeWebRoots = new HashSet<>();

        facet.getConfiguration().getConfigRoots().forEach(root -> Arrays.stream(model.getContentEntries())
                .filter(contentEntry -> ContentEntryEditor.isExcludedOrUnderExcludedDirectory(model.getProject(), contentEntry, root))
                .forEach(c -> excludeConfigRoots.add(root)));
        facet.getConfiguration().getConfigLayerRoots().keySet().forEach(root -> Arrays.stream(model.getContentEntries())
                .filter(contentEntry -> ContentEntryEditor.isExcludedOrUnderExcludedDirectory(model.getProject(), contentEntry, root))
                .forEach(c -> excludeConfigLayerRoots.add(root)));
        facet.getConfiguration().getWebRoots().keySet().forEach(root -> Arrays.stream(model.getContentEntries())
                .filter(contentEntry -> ContentEntryEditor.isExcludedOrUnderExcludedDirectory(model.getProject(), contentEntry, root))
                .forEach(c -> excludeWebRoots.add(root)));


        facet.getConfiguration().getConfigRoots().removeAll(excludeConfigRoots);
        facet.getConfiguration().getConfigLayerRoots().keySet().removeAll(excludeConfigLayerRoots);
        facet.getConfiguration().getWebRoots().keySet().removeAll(excludeWebRoots);


    }

    @NotNull
    @Override
    public AtgModuleFacetConfiguration createConfiguration(@NotNull Collection<VirtualFile> files) {
        AtgModuleFacetConfiguration atgModuleFacetConfiguration = AtgModuleFacetType.getInstance().createDefaultConfiguration();
        for (VirtualFile file : files) {
            String configRootsPatternsStr = AtgToolkitConfig.getInstance().getConfigRootsPatterns();
            List<Pattern> configRootsPatterns = AtgConfigHelper.convertToPatternList(configRootsPatternsStr);

            String configLayerRootsPatternsStr = AtgToolkitConfig.getInstance().getConfigLayerRootsPatterns();
            List<Pattern> configLayerRootsPatterns = AtgConfigHelper.convertToPatternList(configLayerRootsPatternsStr);


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

    protected void setupConfiguration(AtgModuleFacetConfiguration configuration, VirtualFile file) {
        Pattern patternConfig = Pattern.compile("(ATG-.*Config-Path):\\s*(.+/)?(.*)(.jar)");
        Pattern patternRequired = Pattern.compile("(ATG-Required):\\s*(.*)$");
        Pattern patternContextRoot = Pattern.compile("(ATG-Context-Root):\\s*(.*)$");

        try {
            String manifestContents = new String(file.contentsToByteArray());
            String[] manifestLines = manifestContents.split("\\r?\\n");

            if (manifestContents.contains("ATG-Context-Root")) {
                for (String line : manifestLines) {
                    Matcher matcherContext = patternContextRoot.matcher(line);
                    if (matcherContext.matches()) {
                        String contextRoot = matcherContext.group(2);
                        String path = file.getParent().getParent().getPath() + "/";
//                        configuration.getWebRoots().put(contextRoot, path);
                    }
                }
            }
            if (manifestContents.contains("ATG-Config-Path")) {
                for (String line : manifestLines) {
                    Matcher matcherConfig = patternConfig.matcher(line);
                    if (matcherConfig.matches()) {
                        String layer = matcherConfig.group(1);
                        String jarName = matcherConfig.group(3);
                        String path = file.getParent().getParent().getPath() + "/" + jarName + "/";
//                        configuration.getConfigRoots().put(layer, path);
                    }

                    Matcher matcherRequired = patternRequired.matcher(line);
                    if (matcherRequired.matches()) {
                        for (String module : matcherRequired.group(2).split(" ")) {
//                            configuration.getAllRequiredModules().put(module, "");
                        }

                    }
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
