package org.idea.plugin.atg;

import com.intellij.AbstractBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public class AtgModuleBundle extends AbstractBundle {

    @NonNls
    private static final String BUNDLE = "org.idea.plugin.atg.AtgModuleBundle";
    private static final AtgModuleBundle INSTANCE = new AtgModuleBundle();

    private AtgModuleBundle() {
        super(BUNDLE);
    }

    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, @NotNull Object... params) {
        return INSTANCE.getMessage(key, params);
    }
}
