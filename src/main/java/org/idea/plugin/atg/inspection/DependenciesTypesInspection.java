package org.idea.plugin.atg.inspection;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.ASTNode;
import com.intellij.lang.jvm.types.JvmType;
import com.intellij.lang.properties.PropertiesInspectionBase;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.TypeConversionUtil;
import org.idea.plugin.atg.AtgToolkitBundle;
import org.idea.plugin.atg.Constants;
import org.idea.plugin.atg.index.AtgComponentsService;
import org.idea.plugin.atg.util.AtgComponentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;

public class DependenciesTypesInspection extends PropertiesInspectionBase {

    private static boolean shouldCheckClassCast(@NotNull PsiMethod m) {
        JvmType parameterType = m.getParameters()[0].getType();
        if (!(parameterType instanceof PsiClassType)) return true;
        PsiClass psiClassForParameterType = ((PsiClassType) parameterType).resolve();
        if (psiClassForParameterType instanceof PsiTypeParameter) return false;
        String parameterClassName = ((PsiClassType) parameterType).getCanonicalText();
        if (parameterClassName.equals("atg.xml.XMLFile")) return false;
        if (parameterClassName.equals("java.io.File")) return false;
        if (parameterClassName.equals("java.lang.String")) return false;
        if (parameterClassName.equals("java.util.Map")) return false;
        if (parameterClassName.equals("java.util.List")) return false;
        if (parameterClassName.equals("atg.nucleus.ResolvingMap")) return false;
        if (parameterClassName.equals("atg.nucleus.ServiceMap")) return false;
        return true;
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        Optional<PsiClass> componentClass = AtgComponentUtil.getSupposedComponentClass(holder.getFile());
        if (!componentClass.isPresent()) return PsiElementVisitor.EMPTY_VISITOR;

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element instanceof PropertyImpl) {
                    PropertyImpl propertyElement = (PropertyImpl) element;
                    if (propertyElement.getKey() != null && !propertyElement.getKey().startsWith("$")) {
                        Optional<PsiMethod> setterForProperty = AtgComponentUtil.getSetterForProperty(propertyElement);
                        ASTNode valueNode = propertyElement.getValueNode();
                        if (setterForProperty.isPresent() && shouldCheckClassCast(setterForProperty.get()) && valueNode instanceof PropertyValueImpl) {
                            PropertyValueImpl propertyValue = (PropertyValueImpl) valueNode;
                            String dependencyStr = valueNode.getText();
                            String dependencyComponentName = dependencyStr.contains(".")
                                    ? dependencyStr.substring(0, dependencyStr.indexOf('.'))
                                    : dependencyStr;
                            String dependencyField = dependencyStr.contains(".") ? dependencyStr.substring(dependencyStr.indexOf('.') + 1) : "";
                            AtgComponentsService componentsService = ServiceManager.getService(holder.getProject(), AtgComponentsService.class);
                            Collection<PropertiesFileImpl> dependencyLayers = componentsService.getComponentsByName(dependencyComponentName);
                            if (!dependencyLayers.isEmpty()) {
                                PropertiesFileImpl dependency = dependencyLayers.iterator().next();
                                JvmType jvmTypeSetterMethod = AtgComponentUtil.getJvmTypeForSetterMethod(setterForProperty.get());
                                Optional<PsiClass> dependencyClass = AtgComponentUtil.getSupposedComponentClass(dependency);
                                if (jvmTypeSetterMethod instanceof PsiType && dependencyClass.isPresent() && Constants.Keywords.Java.NUCLEUS_REFERENCES.contains(dependencyClass.get().getQualifiedName())) {
                                    PsiClass setterClass = jvmTypeSetterMethod instanceof PsiClassType ? ((PsiClassType) jvmTypeSetterMethod).resolve() : null;
                                    String setterTypePresentableName = setterClass != null ? setterClass.getQualifiedName() : ((PsiType) jvmTypeSetterMethod).getPresentableText();
                                    if (!"".equals(dependencyField)) {
                                        Optional<PsiMethod> fieldGetter = AtgComponentUtil.getGetter(dependencyClass.get(), dependencyField);
                                        if (fieldGetter.isPresent()) {
                                            PsiType returnType = fieldGetter.get().getReturnType();
                                            if (returnType != null) {
                                                boolean assignable = TypeConversionUtil.isAssignable(returnType, (PsiType) jvmTypeSetterMethod);
                                                if (!assignable) {
                                                    PsiClass getterClass = returnType instanceof PsiClassType ? ((PsiClassType) returnType).resolve() : null;
                                                    String getterTypePresentableName = getterClass != null ? getterClass.getQualifiedName() : returnType.getPresentableText();
                                                    holder.registerProblem(propertyValue,
                                                            new TextRange(dependencyComponentName.length() + 1, dependencyComponentName.length() + dependencyField.length() + 1),
                                                            AtgToolkitBundle.message("inspection.dependenciesTypes.text",
                                                                    getterTypePresentableName,
                                                                    setterTypePresentableName));
                                                }
                                            }
                                        }
                                    } else {
                                        boolean assignable = setterClass != null && (dependencyClass.get().equals(setterClass) || dependencyClass.get().isInheritor(setterClass, true));
                                        if (!assignable) {
                                            holder.registerProblem(propertyValue,
                                                    AtgToolkitBundle.message("inspection.dependenciesTypes.text",
                                                            dependencyClass.get().getQualifiedName(),
                                                            setterTypePresentableName));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        };
    }
}


