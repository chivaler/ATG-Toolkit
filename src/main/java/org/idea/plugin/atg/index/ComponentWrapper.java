package org.idea.plugin.atg.index;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ComponentWrapper {
    @Nullable
    private final String scope;
    @Nullable
    private final String javaClass;
    @Nullable
    private final String basedOn;

    public ComponentWrapper(@Nullable String scope, @Nullable String javaClass, @Nullable String basedOn) {
        this.scope = scope;
        this.javaClass = javaClass;
        this.basedOn = basedOn;
    }

    @Nullable
    public String getScope() {
        return scope;
    }

    @Nullable
    public String getJavaClass() {
        return javaClass;
    }

    @Nullable
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
