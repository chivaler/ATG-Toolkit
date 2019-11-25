package org.idea.plugin.atg.psi.reference.contribution;

import com.intellij.patterns.StandardPatterns;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import org.idea.plugin.atg.Constants.Keywords.Actor;
import org.idea.plugin.atg.psi.provider.XmlAttributeComponentNamesProvider;
import org.jetbrains.annotations.NotNull;

public class ActorXmlContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(XmlPatterns.xmlAttributeValue()
                        .withParent(XmlPatterns.xmlAttribute().withName(Actor.NAME)
                                .withParent(XmlPatterns.xmlTag()
                                        .withName(Actor.ACTOR, Actor.COMPONENT, Actor.DROPLET, Actor.FORM)
                                ))
                        .and(XmlPatterns.xmlAttributeValue().withValue(StandardPatterns.string().startsWith("/"))),
                new XmlAttributeComponentNamesProvider());
    }
}
