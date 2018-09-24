package org.idea.plugin.atg.framework;

import com.intellij.framework.FrameworkTypeEx;
import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable;
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.roots.ModifiableModelsProvider;
import com.intellij.openapi.roots.ModifiableRootModel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Created by Igor_Pidgurskyi on 4/18/2017.
 */
public class AtgFramework extends FrameworkTypeEx {
    static final String FRAMEWORK_ID = "ATG";
    static final String FRAMEWORK_NAME = "Oracle ATG Framework";

    protected AtgFramework() {
        super(FRAMEWORK_ID);
    }

    @NotNull
    @Override
    public String getPresentableName() {
        return FRAMEWORK_NAME;
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return AllIcons.Providers.Apache;
    }

    @NotNull
    @Override
    public FrameworkSupportInModuleProvider createProvider() {
        return  new FrameworkSupportInModuleProvider() {
            @NotNull
            @Override
            public FrameworkTypeEx getFrameworkType() {
                return AtgFramework.this;
            }

            @NotNull
            @Override
            public FrameworkSupportInModuleConfigurable createConfigurable(@NotNull FrameworkSupportModel model) {
                return new FrameworkSupportInModuleConfigurable() {

                    @Override
                    public JComponent createComponent() {
                        return new JCheckBox("Extra Option");
                    }

                    @Override
                    public void addSupport(@NotNull Module module, @NotNull ModifiableRootModel model, @NotNull ModifiableModelsProvider provider) {
                        //do what you want here: setup a library, generate a specific file, etc
                    }
                };
            }

            @Override
            public boolean isEnabledForModuleType(@NotNull ModuleType type) {
                return true;
            }
        };
    }
}
