package org.idea.plugin.atg.framework;

import com.intellij.framework.FrameworkTypeEx;
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider;
import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Created by Igor_Pidgurskyi on 4/20/2017.
 */
public class AtgFrameworkType extends FrameworkTypeEx {
    private static AtgFrameworkType INSTANCE = new AtgFrameworkType();
    public static AtgFrameworkType getInstance() {
        return INSTANCE;
    }

    protected AtgFrameworkType() {
        super(AtgFramework.FRAMEWORK_ID);
    }

    @NotNull
    @Override
    public FrameworkSupportInModuleProvider createProvider() {
        return null;
    }

    @NotNull
    @Override
    public String getPresentableName() {
        return AtgFramework.FRAMEWORK_NAME;
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return AllIcons.Providers.Apache;
    }


}
