package org.idea.plugin.atg.index;

import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.PropertiesFileType;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.IOUtil;
import com.intellij.util.io.KeyDescriptor;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

public class AtgComponentsIndexExtension extends FileBasedIndexExtension<String, ComponentWrapper> {
    private static final Logger LOG = Logger.getInstance(AtgComponentsIndexExtension.class);
    private static final String NULL_SUBSTITUTE = "NULL";

    public static final ID<String, ComponentWrapper> NAME = ID.create("atgComponents");

    @NotNull
    @Override
    public ID<String, ComponentWrapper> getName() {
        return NAME;
    }

    @NotNull
    @Override
    public DataIndexer<String, ComponentWrapper, FileContent> getIndexer() {
        return inputData -> {
            try {
                FileContentImpl fileContent = (FileContentImpl) inputData;
                PsiFile psiFile = fileContent.getPsiFile();
                if (psiFile instanceof PropertiesFileImpl) {
                    Optional<String> componentName = AtgComponentUtil.getComponentCanonicalName((PropertiesFileImpl) psiFile);
                    if (componentName.isPresent()) {
                        IProperty classProperty = ((PropertiesFileImpl) psiFile).findPropertyByKey(Constants.Keywords.Properties.CLASS_PROPERTY);
                        String javaClass = classProperty != null ? classProperty.getUnescapedValue() : null;

                        IProperty scopeProperty = ((PropertiesFileImpl) psiFile).findPropertyByKey(Constants.Keywords.Properties.SCOPE_PROPERTY);
                        String scope = scopeProperty != null ? scopeProperty.getUnescapedValue() : null;

                        IProperty basedOnProperty = ((PropertiesFileImpl) psiFile).findPropertyByKey(Constants.Keywords.Properties.BASED_ON_PROPERTY);
                        String basedOn = basedOnProperty != null ? basedOnProperty.getUnescapedValue() : null;

                        ComponentWrapper componentWrapper = new ComponentWrapper(scope, javaClass, basedOn);
                        return Collections.singletonMap(componentName.get(), componentWrapper);
                    }
                }
            } catch (ProcessCanceledException e) {
                throw e;
            } catch (Exception e) {
                LOG.info("Unexpected Error during indexing of properties file", e);
            }
            return Collections.emptyMap();
        };
    }

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @NotNull
    @Override
    public DataExternalizer<ComponentWrapper> getValueExternalizer() {
        return new DataExternalizer<>() {
            @Override
            public void save(@NotNull DataOutput out, ComponentWrapper componentWrapper) throws IOException {
                String javaClassStr = componentWrapper.getJavaClass();
                String scope = componentWrapper.getScope();
                String basedOn = componentWrapper.getBasedOn();
                IOUtil.writeUTF(out, javaClassStr != null ? javaClassStr : NULL_SUBSTITUTE);
                IOUtil.writeUTF(out, scope != null ? scope : NULL_SUBSTITUTE);
                IOUtil.writeUTF(out, basedOn != null ? basedOn : NULL_SUBSTITUTE);
            }

            @Override
            public ComponentWrapper read(@NotNull DataInput in) throws IOException {
                String javaClass = IOUtil.readUTF(in);
                if (NULL_SUBSTITUTE.equals(javaClass)) javaClass = null;
                String scope = IOUtil.readUTF(in);
                if (NULL_SUBSTITUTE.equals(scope)) scope = null;
                String basedOn = IOUtil.readUTF(in);
                if (NULL_SUBSTITUTE.equals(basedOn)) basedOn = null;
                return new ComponentWrapper(scope, javaClass, basedOn);
            }
        };
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return new DefaultFileTypeSpecificInputFilter(PropertiesFileType.INSTANCE);
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getCacheSize() {
        return 8096;
    }
}
