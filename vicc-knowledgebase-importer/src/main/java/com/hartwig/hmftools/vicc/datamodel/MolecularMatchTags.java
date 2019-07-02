package com.hartwig.hmftools.vicc.datamodel;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public abstract class MolecularMatchTags {

    @NotNull
    public abstract String priority();

    @NotNull
    public abstract String compositeKey();

    @NotNull
    public abstract String suppress();

    @NotNull
    public abstract String filterType();

    @NotNull
    public abstract String term();

    @NotNull
    public abstract String primary();

    @NotNull
    public abstract String facet();

    @NotNull
    public abstract String valid();

    @NotNull
    public abstract String custom();
}
