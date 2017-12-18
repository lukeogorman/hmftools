package com.hartwig.hmftools.common.region.bed;

import java.io.IOException;

import com.hartwig.hmftools.common.position.GenomePosition;

import org.jetbrains.annotations.NotNull;

public interface BedFileLookup extends AutoCloseable {
    double score(@NotNull final GenomePosition position) throws IOException;

    @Override
    void close() throws IOException;
}
