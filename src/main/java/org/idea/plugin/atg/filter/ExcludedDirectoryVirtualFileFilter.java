package org.idea.plugin.atg.filter;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ExcludedDirectoryVirtualFileFilter implements VirtualFileFilter {

    private static final String[] EXCLUDED_DIRECTORY = new String[] { "/.idea/", "/build/", "/out/"};
    private static final Set<String> excludedDirectories = new HashSet<>(Arrays.asList(EXCLUDED_DIRECTORY));

    @Override
    public boolean accept(VirtualFile file) {
        return file != null && !isExcludedDirectory(file);
    }

    private boolean isExcludedDirectory(VirtualFile file) {
        boolean result = true;
        for (String excludeDirectory : excludedDirectories) {
            result = !file.isDirectory() && file.getPath().contains(excludeDirectory);
            if(result) {
                break;
            }
        }
        return result;
    }
}
