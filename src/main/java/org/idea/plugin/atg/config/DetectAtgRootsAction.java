package org.idea.plugin.atg.config;

import com.google.common.collect.Lists;
import com.intellij.facet.FacetManager;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbModeTask;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.index.AtgIndexService;
import org.idea.plugin.atg.module.AtgModuleFacet;
import org.idea.plugin.atg.module.AtgModuleFacetConfiguration;
import org.idea.plugin.atg.module.AtgModuleFacetType;
import org.idea.plugin.atg.util.AtgEnvironmentUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DetectAtgRootsAction extends AnAction {
    private static final String DETECT_ROOTS_TASK = "DETECT_ROOTS_TASK";
    private final AtgModuleFacetType atgModuleFacetType = AtgModuleFacetType.getInstance();
    private List<VirtualFile> addedRoots = Lists.newArrayList();
    private List<VirtualFile> removedRoots = Lists.newArrayList();

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project != null) {
            AtgToolkitConfig atgToolkitConfig = AtgToolkitConfig.getInstance(project);
            runDetection(project, atgToolkitConfig.getConfigRootsPatterns(), atgToolkitConfig.getConfigLayerRootsPatterns(),
                    atgToolkitConfig.isRemoveRootsNonMatchedToPatterns(), atgToolkitConfig.isRemoveFacetsIfModuleHasNoAtgRoots());
        }
    }

    void runDetection(@NotNull Project project, @NotNull String configRootsPatternsStr, @NotNull String configRootsLayerPatternsStr,
                             boolean removeRootsNonMatchedToPatterns, boolean removeFacetsIfModuleHasNoAtgRoots) {
        List<Pattern> configRootsPatterns = AtgConfigHelper.convertToPatternList(configRootsPatternsStr);
        List<Pattern> configRootsLayerPatterns = AtgConfigHelper.convertToPatternList(configRootsLayerPatternsStr);
        AtgIndexService atgIndexService = ServiceManager.getService(project, AtgIndexService.class);
        addedRoots = Lists.newArrayList();
        removedRoots = Lists.newArrayList();
        DumbService.getInstance(project).queueTask(new DumbModeTask(DETECT_ROOTS_TASK) {
            @Override
            public void performInDumbMode(@NotNull ProgressIndicator indicator) {
                ApplicationManager.getApplication().runReadAction(() -> {
                    indicator.setText(AtgToolkitBundle.message("action.detect.roots.text"));
                    int currentModuleNumber = 0;
                    Module[] allModules = ModuleManager.getInstance(project).getModules();
                    for (Module module : allModules) {
                        ProgressManager.checkCanceled();
                        indicator.setFraction(currentModuleNumber++ / (double) allModules.length);
                        indicator.setText2(AtgToolkitBundle.message("action.detect.roots.text2", module.getName()));
                        ModifiableRootModel modifiableRootModel = ModuleRootManager.getInstance(module).getModifiableModel();
                        AtgModuleFacet atgFacet = FacetManager.getInstance(module).getFacetByType(Constants.FACET_TYPE_ID);

                        List<VirtualFile> foundConfigRoots = Arrays.stream(modifiableRootModel.getContentRoots())
                                .map(r -> AtgConfigHelper.collectRootsMatchedPatterns(r, configRootsPatterns))
                                .flatMap(Collection::stream)
                                .collect(Collectors.toList());

                        List<VirtualFile> foundConfigLayerRoots = Arrays.stream(modifiableRootModel.getContentRoots())
                                .map(r -> AtgConfigHelper.collectRootsMatchedPatterns(r, configRootsLayerPatterns))
                                .flatMap(Collection::stream)
                                .collect(Collectors.toList());

                        if (!foundConfigRoots.isEmpty() || !foundConfigLayerRoots.isEmpty()) {
                            if (atgFacet == null) {
                                atgFacet = createDefaultFacetForModule(module);
                            }
                            AtgModuleFacetConfiguration atgFacetConfiguration = atgFacet.getConfiguration();
                            synchronizePersistedWitFoundByPatterns(foundConfigRoots, atgFacetConfiguration.getConfigRoots(),
                                    atgIndexService, removeRootsNonMatchedToPatterns,
                                    c -> atgFacetConfiguration.getConfigRoots().addAll(c));
                            synchronizePersistedWitFoundByPatterns(foundConfigLayerRoots, atgFacetConfiguration.getConfigLayerRoots().keySet(),
                                    atgIndexService, removeRootsNonMatchedToPatterns,
                                    c -> atgFacetConfiguration.getConfigLayerRoots().putAll(c.stream().collect((Collectors.toMap(f -> f, f -> "")))));
                        } else if (atgFacet != null) {
                            removePreviousRootsIfRequired(atgFacet, removeRootsNonMatchedToPatterns, atgIndexService);
                        }

                        deleteFacetIfRequired(module, atgFacet, removeFacetsIfModuleHasNoAtgRoots);
                        AtgEnvironmentUtil.runWriteAction(modifiableRootModel::commit);
                    }
                });
                indicator.setText(AtgToolkitBundle.message("action.update.dependencies.indexing.text"));
                indicator.setText2(null);
                notifyAboutChangedRoots(addedRoots, removedRoots, project);
            }
        });
    }

    private void notifyAboutChangedRoots(@NotNull List<VirtualFile> addedRoots,
                                         @NotNull List<VirtualFile> removedRoots,
                                         @NotNull Project project) {
        if (addedRoots.isEmpty() && removedRoots.isEmpty()) {
            new Notification(Constants.NOTIFICATION_GROUP_ID,
                    AtgToolkitBundle.message("gui.config.detection.changedRoots.title"),
                    AtgToolkitBundle.message("gui.config.detection.noChangedRoots.text"),
                    NotificationType.INFORMATION).notify(project);
        } else {
            if (!addedRoots.isEmpty()) {
                new Notification(Constants.NOTIFICATION_GROUP_ID,
                        AtgToolkitBundle.message("gui.config.detection.changedRoots.title"),
                        AtgToolkitBundle.message("gui.config.detection.addedRoots.text", getRelativePaths(addedRoots, project)),
                        NotificationType.INFORMATION).notify(project);
            }
            if (!removedRoots.isEmpty()) {
                new Notification(Constants.NOTIFICATION_GROUP_ID,
                        AtgToolkitBundle.message("gui.config.detection.changedRoots.title"),
                        AtgToolkitBundle.message("gui.config.detection.removedRoots.text", getRelativePaths(removedRoots, project)),
                        NotificationType.INFORMATION).notify(project);
            }
        }
    }

    @NotNull
    private List<String> getRelativePaths(@NotNull Collection<VirtualFile> virtualFiles,
                                          @NotNull Project project) {
        return virtualFiles.stream().map(f -> {
            String path = null;
            ProjectFileIndex projectFileIndex = ProjectFileIndex.getInstance(project);
            Module module = projectFileIndex.getModuleForFile(f);
            VirtualFile contentRoot = projectFileIndex.getContentRootForFile(f);
            if (contentRoot != null) {
                path = VfsUtilCore.getRelativePath(f, contentRoot);
                if (module != null) {
                    path = module.getName() + ":" + path;
                }
            }
            return path != null ? path : f.getPresentableName();
        }).collect(Collectors.toList());
    }

    private void removePreviousRootsIfRequired(@NotNull AtgModuleFacet atgFacet,
                                               boolean removeRootsNonMatchedToPatterns,
                                               @NotNull AtgIndexService atgIndexService) {
        if (removeRootsNonMatchedToPatterns) {
            //TODO ContainerUtil.intersection
            AtgModuleFacetConfiguration atgFacetConfiguration = atgFacet.getConfiguration();
            Collection<VirtualFile> configRoots = atgFacetConfiguration.getConfigRoots();
            Collection<VirtualFile> previousConfigRoots = Lists.newArrayList(configRoots);
            configRoots.removeAll(previousConfigRoots);
            atgIndexService.notifyConfigRootsChanged(previousConfigRoots);

            Collection<VirtualFile> configLayerRoots = atgFacetConfiguration.getConfigLayerRoots().keySet();
            Collection<VirtualFile> previousConfigLayerRoots = Lists.newArrayList(configLayerRoots);
            configRoots.removeAll(previousConfigLayerRoots);
            atgIndexService.notifyConfigRootsChanged(previousConfigLayerRoots);
        }
    }

    private void deleteFacetIfRequired(@NotNull Module module,
                                       @Nullable AtgModuleFacet atgFacet,
                                       boolean removeFacetsIfModuleHasNoAtgRoots) {
        if (atgFacet != null && removeFacetsIfModuleHasNoAtgRoots) {
            AtgModuleFacetConfiguration atgFacetConfiguration = atgFacet.getConfiguration();
            if (atgFacetConfiguration.getConfigRoots().isEmpty() && atgFacetConfiguration.getConfigLayerRoots().isEmpty() && atgFacetConfiguration.getWebRoots().isEmpty()) {
                ModifiableFacetModel modifiableFacetModel = FacetManager.getInstance(module).createModifiableModel();
                modifiableFacetModel.removeFacet(atgFacet);
                AtgEnvironmentUtil.runWriteAction(modifiableFacetModel::commit);
            }
        }
    }

    private AtgModuleFacet createDefaultFacetForModule(@NotNull Module module) {
        AtgModuleFacet atgFacet;
        AtgModuleFacetConfiguration atgConfiguration = atgModuleFacetType.createDefaultConfiguration();
        String defaultFacetName = atgModuleFacetType.getDefaultFacetName();
        atgFacet = atgModuleFacetType.createFacet(module, defaultFacetName, atgConfiguration, null);
        ModifiableFacetModel modifiableFacetModel = FacetManager.getInstance(module).createModifiableModel();
        modifiableFacetModel.addFacet(atgFacet);
        AtgEnvironmentUtil.runWriteAction(modifiableFacetModel::commit);
        return atgFacet;
    }

    private void synchronizePersistedWitFoundByPatterns(@NotNull List<VirtualFile> foundRoots,
                                                        @NotNull Collection<VirtualFile> facetExistRoots,
                                                        @NotNull AtgIndexService atgIndexService,
                                                        boolean removeRootsNonMatchedToPatterns,
                                                        @NotNull Consumer<Collection<VirtualFile>> persistFunction) {
        Collection<VirtualFile> previousConfigLayerRoots = Lists.newArrayList(facetExistRoots);
        previousConfigLayerRoots.removeAll(foundRoots);
        foundRoots.removeAll(facetExistRoots);
        persistFunction.accept(foundRoots);
        addedRoots.addAll(foundRoots);
        atgIndexService.notifyConfigRootsChanged(foundRoots);

        if (!previousConfigLayerRoots.isEmpty() && removeRootsNonMatchedToPatterns) {
            facetExistRoots.removeAll(previousConfigLayerRoots);
            removedRoots.addAll(previousConfigLayerRoots);
            atgIndexService.notifyConfigRootsChanged(previousConfigLayerRoots);
        }
    }
}
