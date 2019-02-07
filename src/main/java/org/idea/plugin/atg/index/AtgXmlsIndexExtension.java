package org.idea.plugin.atg.index;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

public class AtgXmlsIndexExtension extends ScalarIndexExtension<String> {
    private static final Logger LOG = Logger.getInstance(AtgXmlsIndexExtension.class);

    public static final ID<String, Void> NAME = ID.create("atgXmls");

    @NotNull
    @Override
    public ID<String, Void> getName() {
        return NAME;
    }

    @NotNull
    @Override
    public DataIndexer<String, Void, FileContent> getIndexer() {
        return inputData -> {
            try {
                VirtualFile file = inputData.getFile();
                Project project = Optional.ofNullable(inputData.getProject())
                        .orElse(Arrays.stream(ProjectManager.getInstance().getOpenProjects())
                                .filter(p -> {
                                    ProjectFileIndex index = ProjectFileIndex.SERVICE.getInstance(p);
                                    return index.isInLibrary(file) || index.getModuleForFile(file) != null;
                                })
                                .findAny()
                                .orElse(null));
                Optional<String> xmlRelativePath = AtgComponentUtil.getAtgRelativeName(file, project, "");
                if (xmlRelativePath.isPresent()) {
                    return Collections.singletonMap(xmlRelativePath.get(), null);
                }
            } catch (ProcessCanceledException e) {
                throw e;
            } catch (Exception e) {
                LOG.info("Unexpected Error during indexing of bytecode", e);
            }
            return Collections.emptyMap();
        };
    }

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return new DefaultFileTypeSpecificInputFilter(XmlFileType.INSTANCE) {
            @Override
            public boolean acceptInput(@NotNull VirtualFile file) {
                return XmlFileType.DEFAULT_EXTENSION.equals(file.getExtension());
            }
        };
    }

    @Override
    public boolean dependsOnFileContent() {
        return false;
    }

    @Override
    public int getCacheSize() {
        return 256;
    }
}
