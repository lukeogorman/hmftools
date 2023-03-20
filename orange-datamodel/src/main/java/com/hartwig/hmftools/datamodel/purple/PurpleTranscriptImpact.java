package com.hartwig.hmftools.datamodel.purple;

import org.immutables.gson.Gson;
import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

@Gson.TypeAdapters
@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public interface PurpleTranscriptImpact {

    @NotNull
    String transcript();

    @NotNull
    String hgvsCodingImpact();

    @NotNull
    String hgvsProteinImpact();

    @Nullable
    Integer affectedCodon();

    @Nullable
    Integer affectedExon();

    boolean spliceRegion();

    @NotNull
    Set<PurpleVariantEffect> effects();

    @NotNull
    PurpleCodingEffect codingEffect();
}
