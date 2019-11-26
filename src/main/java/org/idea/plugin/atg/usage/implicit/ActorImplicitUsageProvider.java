package org.idea.plugin.atg.usage.implicit;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.MethodSignature;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.indexing.FileBasedIndex;
import org.apache.commons.lang.StringUtils;
import org.idea.plugin.atg.filter.ExcludedDirectoryVirtualFileFilter;
import org.idea.plugin.atg.index.XmlActorIndexExtension;
import org.idea.plugin.atg.iterator.PropertiesContentIterator;
import org.idea.plugin.atg.visitor.XmlPsiRecursiveElementVisitor;


import java.util.*;
import java.util.stream.Collectors;


public class ActorImplicitUsageProvider implements ImplicitUsageProvider {

    private static final String PROPERTY_CLASS = "$class";
    private static final String XML_TAG_COMPONENT = "component";
    private static final String XML_TAG_FORM = "form";
    private static final String DOT_DELIMITER = ".";
    private static final String XML_TAG_INPUT = "input";
    private static final String XML_ATTRIBUTE_CLASS_NAME = "class-name";
    private static final String XML_ATTRIBUTE_NAME = "name";
    private static final String EMPTY_STRING = "";
    private static final String PREFIX_HANDLE = "handle";

    @Override
    public boolean isImplicitRead(PsiElement element) {
        return false;
    }

    @Override
    public boolean isImplicitWrite(PsiElement element) {
        return false;
    }

    @Override
    public boolean isImplicitUsage(PsiElement element) {
        boolean result = false;
        if (element instanceof PsiMethod) {
            PsiMethod inspectMethod = (PsiMethod) element;
            Project project = inspectMethod.getProject();
            Collection<String> xmlFileName = FileBasedIndex.getInstance().getAllKeys(XmlActorIndexExtension.NAME, project);
            List<String> pathToFiles = xmlFileName.stream()
                    .flatMap(nameOfFile -> FileBasedIndex.getInstance()
                            .getValues(XmlActorIndexExtension.NAME, nameOfFile, GlobalSearchScope.allScope(project)).stream())
                    .collect(Collectors.toList());
            List<String> componentPaths;
            ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
            if (((PsiMethod) element).getName().startsWith(PREFIX_HANDLE)) {
                componentPaths = findComponentNameWithXmlTag(pathToFiles, XML_TAG_FORM, project, inspectMethod);
            } else {
                componentPaths = findComponentNameWithXmlTag(pathToFiles, XML_TAG_COMPONENT, project, inspectMethod);
            }

            // TODO refactor get property files from index
            result = componentPaths.stream().anyMatch(componentPath -> {
                List<VirtualFile> propertiesVirtualFiles = new ArrayList<>();
                PropertiesContentIterator propertiesContentIterator =
                        new PropertiesContentIterator(componentPath, propertiesVirtualFiles);
                projectFileIndex.iterateContent(propertiesContentIterator);
                return isUsageInComponent(propertiesVirtualFiles, PROPERTY_CLASS, project, inspectMethod);
            });
        }
        return result;
    }

    private String getNucleusComponent(PsiElement[] componentChild, PsiMethod method, XmlTag parentTag) {
        String result = null;

        Optional<XmlAttribute> xmlMethodName = Arrays.stream(componentChild)
                .filter(element -> element instanceof XmlAttribute)
                .map(element -> ((XmlAttribute) element))
                .filter(element -> Objects.equals(getXmlMethodName(element, parentTag), method.getName()))
                .findFirst();

        MethodSignature signature = method.getSignature(PsiSubstitutor.EMPTY);
        if (xmlMethodName.isPresent() && checkMethodSignature(componentChild, signature, parentTag)) {
            result = Arrays.stream(componentChild)
                    .filter(element -> element instanceof XmlAttribute)
                    .filter(element -> XML_ATTRIBUTE_NAME.equals(((XmlAttribute) element).getName()))
                    .map(element -> ((XmlAttribute) element).getValue())
                    .findAny()
                    .orElse(EMPTY_STRING);
        }
        return result;
    }

    private boolean checkMethodSignature(PsiElement[] componentChild, MethodSignature signature, XmlTag parentTag) {
        boolean result = false;
        List<PsiElement> tagInputList = Arrays.stream(componentChild)
                .filter(element -> element instanceof XmlTag)
                .filter(element -> (XML_TAG_INPUT.equals(((XmlTag) element).getName())))
                .collect(Collectors.toList());

        if (XML_TAG_FORM.equals(parentTag.getName())) {
            result = true;
        } else if (tagInputList.size() == signature.getParameterTypes().length) {
            PsiType[] signatureParamTypes = signature.getParameterTypes();
            result = tagInputList.stream()
                    .flatMap(element -> Arrays.stream(element.getChildren()))
                    .filter(element -> element instanceof XmlAttribute)
                    .filter(element -> (XML_ATTRIBUTE_CLASS_NAME.equals(((XmlAttribute) element).getName())))
                    .map(element -> ((XmlAttribute) element).getValue())
                    .filter(Objects::nonNull)
                    .anyMatch(element -> Arrays.stream(signatureParamTypes).map(PsiType::getPresentableText)
                            .noneMatch(signaturesClass -> signaturesClass.equals(element)));
        }
        return result;
    }

    private boolean isUsageInComponent(List<VirtualFile> propertiesVirtualFiles,
                                       String propertyName,
                                       Project project,
                                       PsiMethod inspectMethod) {
        PsiManager psiManager = PsiManager.getInstance(project);
        return propertiesVirtualFiles.stream()
                .map(psiManager::findFile)
                .filter(Objects::nonNull)
                .map(propPsiFile -> (PropertiesFile) propPsiFile.getOriginalElement())
                .map(propPsiFile -> propPsiFile.findPropertyByKey(propertyName))
                .filter(Objects::nonNull)
                .anyMatch(property -> isSameJavaClass(property, inspectMethod));
    }

    private boolean isSameJavaClass(IProperty property, PsiMethod inspectMethod) {
        String classProp = property.getValue();
        PsiJavaFile javaFile = (PsiJavaFile) inspectMethod.getContainingFile();
        PsiClass javaClassName = (PsiClass) inspectMethod.getParent();
        return classProp != null
                && classProp.equals(javaFile.getPackageName() + DOT_DELIMITER + javaClassName.getName());
    }

    private String getXmlMethodName(XmlAttribute xmlAttribute, XmlTag xmlParentTag) {
        return XML_TAG_FORM.equals(xmlParentTag.getName())
                ? xmlAttribute.getName() + StringUtils.capitalize(xmlAttribute.getValue())
                : xmlAttribute.getValue();
    }

    private List<String> findComponentNameWithXmlTag(List<String> pathToFiles,
                                                     String xmlTag,
                                                     Project project,
                                                     PsiMethod inspectMethod) {
        PsiManager psiManager = PsiManager.getInstance(project);
        List<PsiElement> xmlTags = new ArrayList<>();
        XmlPsiRecursiveElementVisitor xmlPsiRecursiveElementVisitor = new XmlPsiRecursiveElementVisitor(xmlTag, xmlTags);
        ExcludedDirectoryVirtualFileFilter fileFilter = new ExcludedDirectoryVirtualFileFilter();
        return pathToFiles.stream()
                .map(path -> LocalFileSystem.getInstance().findFileByPathIfCached(path))
                .filter(Objects::nonNull)
                .filter(fileFilter::accept)
                .map(psiManager::findFile)
                .filter(Objects::nonNull)
                .map(PsiFile::getOriginalElement)
                .peek(xmlPsiRecursiveElementVisitor::visitElement)
                .flatMap(psiElement -> xmlTags.stream())
                .map(xmlComponent ->
                        getNucleusComponent(xmlComponent.getChildren(), inspectMethod, ((XmlTag) xmlComponent)))
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toList());
    }

}
