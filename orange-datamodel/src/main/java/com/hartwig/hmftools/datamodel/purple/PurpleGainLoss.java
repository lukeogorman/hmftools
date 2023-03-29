package com.hartwig.hmftools.datamodel.purple;

import org.immutables.gson.Gson;
import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Gson.TypeAdapters
@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public interface PurpleGainLoss {

    @NotNull
    CopyNumberInterpretation interpretation();

    @NotNull
    String chromosome();

    @NotNull
    String chromosomeBand();

    @NotNull
    String gene();

    @NotNull
    String transcript();

    boolean isCanonical();

    long minCopies();

    long maxCopies();
}