package org.idea.plugin.atg.index;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ComponentWrapper {
    @Nullable
    private String scope;
    @Nullable
    private String javaClass;
    @Nullable
    private String basedOn;

    public ComponentWrapper(@Nullable String scope, @Nullable String javaClass, @Nullable String basedOn) {
        this.scope = scope;
        this.javaClass = javaClass;
        this.basedOn = basedOn;
    }

    public String getScope() {
        return scope;
    }

    public String getJavaClass() {
        return javaClass;
    }

    public String getBasedOn() {
        return basedOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComponentWrapper that = (ComponentWrapper) o;
        return Objects.equals(scope, that.scope) &&
                Objects.equals(javaClass, that.javaClass) &&
                Objects.equals(basedOn, that.basedOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scope, javaClass, basedOn);
    }
}
