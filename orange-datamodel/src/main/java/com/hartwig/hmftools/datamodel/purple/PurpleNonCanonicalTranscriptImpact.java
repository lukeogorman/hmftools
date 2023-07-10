package com.hartwig.hmftools.datamodel.purple;

import java.util.Set;

import org.immutables.gson.Gson;
import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Gson.TypeAdapters
@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public interface PurpleNonCanonicalTranscriptImpact {
    @NotNull
    String transcript();
    @NotNull
    String hgvsCodingImpact();
    @NotNull
    String hgvsProteinImpact();
    boolean spliceRegion();
    @NotNull
    Set<PurpleVariantEffect> effects();
}
