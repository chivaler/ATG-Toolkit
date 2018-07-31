/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.idea.plugin.atg.roots;

import com.intellij.ide.util.projectWizard.importSources.DetectedProjectRoot;
import com.intellij.ide.util.projectWizard.importSources.DetectedSourceRoot;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
public class AtgConfigurationSourceRoot extends DetectedSourceRoot {
  private final List<String> myLanguages;
  private final boolean myWithModuleInfoFile; // module-info.java

  public AtgConfigurationSourceRoot(File directory, @Nullable String packagePrefix, @NotNull String language) {
    super(directory, packagePrefix);
    myLanguages = new ArrayList<>();
    myLanguages.add(language);
    myWithModuleInfoFile = false;
  }

  public AtgConfigurationSourceRoot(File directory, @NotNull String language, boolean withModuleInfoFile) {
    super(directory, "");
    myLanguages = new ArrayList<>();
    myLanguages.add(language);
    myWithModuleInfoFile = withModuleInfoFile;
  }

  private AtgConfigurationSourceRoot(File directory, String packagePrefix, List<String> languages) {
    super(directory, packagePrefix);
    myLanguages = languages;
    myWithModuleInfoFile = false;
  }

  @NotNull
  @Override
  public String getRootTypeName() {
    return StringUtil.join(myLanguages, ", ");
  }

  @Override
  public DetectedProjectRoot combineWith(@NotNull DetectedProjectRoot root) {
    if (root instanceof AtgConfigurationSourceRoot) {
      return combineWith((AtgConfigurationSourceRoot)root);
    }
    return null;
  }

  @NotNull
  public AtgConfigurationSourceRoot combineWith(@NotNull AtgConfigurationSourceRoot root) {
    List<String> union = new ArrayList<>(myLanguages.size() + root.myLanguages.size());
    union.addAll(myLanguages);
    union.addAll(root.myLanguages);
    ContainerUtil.removeDuplicates(union);
    return new AtgConfigurationSourceRoot(getDirectory(), getPackagePrefix(), union);
  }

  public boolean isWithModuleInfoFile() {
    return myWithModuleInfoFile;
  }
}
