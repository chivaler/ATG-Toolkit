package org.idea.plugin.atg.roots;

import com.intellij.openapi.util.Comparing;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.ex.JpsElementBase;

public class AtgConfigurationRootProperties extends JpsElementBase<AtgConfigurationRootProperties> {
  private String myRelativeOutputPath = "";

  public AtgConfigurationRootProperties(@NotNull String relativeOutputPath) {
    myRelativeOutputPath = relativeOutputPath;
  }

  /**
   * @return relative path to the target directory under the module output directory for resource files from this root
   */
  @NotNull
  public String getRelativeOutputPath() {
    return myRelativeOutputPath;
  }

  @NotNull
  @Override
  public AtgConfigurationRootProperties createCopy() {
    return new AtgConfigurationRootProperties(myRelativeOutputPath);
  }

  public void setRelativeOutputPath(@NotNull String relativeOutputPath) {
    if (!Comparing.equal(myRelativeOutputPath, relativeOutputPath)) {
      myRelativeOutputPath = relativeOutputPath;
      fireElementChanged();
    }
  }


  @Override
  public void applyChanges(@NotNull AtgConfigurationRootProperties modified) {
    setRelativeOutputPath(modified.myRelativeOutputPath);
  }
}
