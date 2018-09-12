package org.idea.plugin.atg.config;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.intellij.ide.util.DirectoryUtil;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.module.AtgModuleFacet;
import org.idea.plugin.atg.module.AtgModuleFacetConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AtgConfigHelper {
    private static Cache<VirtualFile, String> webRootsContextCache = CacheBuilder.newBuilder().build();

    private AtgConfigHelper() {

    }

    public static PsiDirectory getComponentConfigPsiDirectory(AtgModuleFacet atgModuleFacet, PsiPackage srcPackage) {
        PsiManager psiManager = srcPackage.getManager();
        Iterator<VirtualFile> iterator = atgModuleFacet.getConfiguration().getConfigRoots().iterator();
        if (iterator.hasNext()) {
            String targetDirStr = iterator.next().getCanonicalPath();
            if (targetDirStr != null) {
                return WriteCommandAction.writeCommandAction(srcPackage.getProject())
                        .withName(AtgToolkitBundle.message("create.directory.command"))
                        .compute(() -> DirectoryUtil.mkdirs(psiManager, targetDirStr + "/" + srcPackage.getQualifiedName().replace('.', '/')));
            }
        }

        return null;
    }

    public static List<Pattern> convertToPatternList(@NotNull String str) {
        String[] patternStrArray = str.replace(".", "\\.")
                .replace("?", ".?")
                .replace("*", "[\\w-]+")
                .split("[,;]");
        return Stream.of(patternStrArray)
                .map(s -> s + "$")
                .map(Pattern::compile)
                .collect(Collectors.toList());
    }

    @NotNull
    public static Map<VirtualFile, String> detectWebRootsForModule(ModifiableRootModel model) {
        return Arrays.stream(model.getContentEntries())
                .map(ContentEntry::getFile)
                .map(f -> AtgConfigHelper.collectWebRoots(f, model.getProject()).entrySet())
                .flatMap(Set::stream)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    }


    private static Map<VirtualFile, String> collectWebRoots(final VirtualFile contentEntryRoot, final Project project) {
        Map<VirtualFile, String> result = new HashMap<>();
        VfsUtilCore.visitChildrenRecursively(contentEntryRoot, new VirtualFileVisitor() {
            @Override
            public boolean visitFile(@NotNull VirtualFile root) {
                Optional<Pair<VirtualFile, String>> webRoot = suggestWebRootForRoot(root, project);
                if (webRoot.isPresent()) {
                    result.put(webRoot.get().getFirst(), webRoot.get().getSecond());
                    return false;
                }
                return true;
            }
        });
        return result;
    }

    @NotNull
    public static Set<Pair<VirtualFile, String>> getWebRootsWithContexts(@NotNull AtgModuleFacetConfiguration atgModuleFacetConfiguration, @NotNull Project project) {
        return atgModuleFacetConfiguration.getWebRoots().stream()
                .map(f -> {
                    try {
                        return new Pair<>(f, webRootsContextCache.get(f, () -> {
                            Optional<Pair<VirtualFile, String>> virtualFileStringPair = suggestWebRootForRoot(f, project);
                            if (virtualFileStringPair.isPresent()) {
                                return virtualFileStringPair.get().second;
                            }
                            return "/";
                        }));
                    } catch (ExecutionException | UncheckedExecutionException e) {
                        return new Pair<>(f, "/");
                    }
                })
                .collect(Collectors.toSet());
    }

    @NotNull
    public static Optional<Pair<VirtualFile, String>> suggestWebRootForRoot(@NotNull final VirtualFile root, final Project project) {
        if (root.isDirectory()) {
            VirtualFile webInfFolder = root.findChild("WEB-INF");
            if (webInfFolder != null) {
                VirtualFile webXml = webInfFolder.findChild("web.xml");
                if (webXml != null) {
                    return Optional.of(new Pair<>(root, detectContextRootFromWebXml(webXml, project)));
                }
            }
        }
        return Optional.empty();
    }

    private static List<VirtualFile> collectRootsMatchedPatterns(final VirtualFile contentEntryRoot, final List<Pattern> patterns) {
        List<VirtualFile> result = new ArrayList<>();
        VfsUtilCore.visitChildrenRecursively(contentEntryRoot, new VirtualFileVisitor() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                if (file.isDirectory() && file.getCanonicalPath() != null) {
                    if (patterns.stream().anyMatch(p -> p.matcher(file.getCanonicalPath()).find())) {
                        result.add(file);
                        return false;
                    }
                }
                return true;
            }
        });
        return result;
    }

    @NotNull
    public static String detectContextRootFromWebXml(final VirtualFile webXml, final Project project) {
        PsiFile psiWebXml = PsiManager.getInstance(project).findFile(webXml);
        if (psiWebXml instanceof XmlFile) {
            XmlTag rootTag = ((XmlFile) psiWebXml).getRootTag();
            if (rootTag != null) {
                XmlTag[] contextParamTags = rootTag.findSubTags("context-param");
                for (XmlTag contextParamTag : contextParamTags) {
                    XmlTag[] paramNameTags = contextParamTag.findSubTags("param-name");
                    if (paramNameTags.length > 0 && "context-root".equals(paramNameTags[0].getValue().getTrimmedText())) {
                        XmlTag[] contextRootValue = contextParamTag.findSubTags("param-value");
                        if (contextRootValue.length > 0) {
                            String contextStr = contextRootValue[0].getValue().getTrimmedText();
                            if (!contextStr.startsWith("/")) {
                                contextStr = "/" + contextStr;
                            }
                            return contextStr;
                        }
                    }
                }
            }
        }
        return "/";
    }

}
