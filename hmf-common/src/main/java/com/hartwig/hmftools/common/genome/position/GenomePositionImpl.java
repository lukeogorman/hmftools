package com.hartwig.hmftools.common.genome.position;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
abstract class GenomePositionImpl implements GenomePosition
{

}
