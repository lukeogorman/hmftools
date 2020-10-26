package com.hartwig.hmftools.common.doid;

import java.util.List;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(allParameters = true,
             passAnnotations = { NotNull.class, Nullable.class })
public abstract class Doid {

    // TODO rename to DoidNode
    @NotNull
    public abstract List<DoidEntry> entries();

    @NotNull
    public abstract DoidEdges edges();

    @NotNull
    public abstract String id();

    // TODO Is this a list of strings?
    @NotNull
    public abstract List<String> equivalentNodsSets();

    // TODO Add all other fields of a graphElement.
}
