package org.idea.plugin.atg.index;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.idea.plugin.atg.visitor.XmlPsiRecursiveElementVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class XmlActorIndexExtension extends FileBasedIndexExtension<String, String> {
    private static final Logger LOG = Logger.getInstance(XmlActorIndexExtension.class);
    private static final String ACTOR_TEMPLATE_XML_TAG = "actor-template";
    public static final ID<String, String> NAME = ID.create("atgXmlActors");


    @NotNull
    @Override
    public ID<String, String> getName() {
        return NAME;
    }

    @NotNull
    @Override
    public DataIndexer<String, String, FileContent> getIndexer() {
        return inputData -> {
            VirtualFile file = inputData.getFile();

            return Collections.singletonMap(file.getName(), file.getPath());
        };
    }

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @NotNull
    @Override
    public DataExternalizer<String> getValueExternalizer() {
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
            private Project project = ProjectManager.getInstance().getDefaultProject();
            private PsiManager psiManager = PsiManager.getInstance(project);

            @Override
            public boolean acceptInput(@NotNull VirtualFile file) {
                boolean foundActorTag = false;
                if(isXmlFile(file)) {
                    PsiFile psiFile = psiManager.findFile(file);
                    if (psiFile != null) {
                        PsiElement psiElement = psiFile.getOriginalElement();
                        List<PsiElement> xmlTags = new ArrayList<>();
                        PsiElementVisitor elementVisitor =
                                new XmlPsiRecursiveElementVisitor(ACTOR_TEMPLATE_XML_TAG, xmlTags);
                        elementVisitor.visitElement(psiElement);
                        foundActorTag = !xmlTags.isEmpty();
                    }
                }
                return foundActorTag;
            }
        };
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    private boolean isXmlFile(@NotNull VirtualFile fileOrDir) {
        return XmlFileType.DEFAULT_EXTENSION.equals(fileOrDir.getExtension());
    }
}
