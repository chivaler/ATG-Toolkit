package org.idea.plugin.atg;

import com.intellij.AbstractBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public class AtgToolkitBundle extends AbstractBundle {

    @NonNls
    private static final String BUNDLE = "org.idea.plugin.atg.AtgToolkitBundle";
    private static final AtgToolkitBundle INSTANCE = new AtgToolkitBundle();

    private AtgToolkitBundle() {
        super(BUNDLE);
    }

    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, @NotNull Object... params) {
        return INSTANCE.getMessage(key, params);
    }
}
