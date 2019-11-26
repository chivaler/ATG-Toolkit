package org.idea.plugin.atg.iterator;

import com.intellij.lang.properties.PropertiesFileType;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PropertiesContentIterator implements ContentIterator {

    private final String componentPath;
    private final List<VirtualFile> virtualFiles;

    public PropertiesContentIterator(String componentPath, List<VirtualFile> virtualFiles) {
        this.componentPath = componentPath;
        this.virtualFiles = virtualFiles;
    }

    @Override
    public boolean processFile(@NotNull VirtualFile fileOrDir) {
        if (fileOrDir.getPath().endsWith(componentPath + PropertiesFileType.DOT_DEFAULT_EXTENSION)) {
            virtualFiles.add(fileOrDir);
        }
        return true;
    }
}
