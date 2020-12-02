package com.hartwig.hmftools.serve.hotspot;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hartwig.hmftools.common.variant.hotspot.VariantHotspot;
import com.hartwig.hmftools.serve.RefGenomeVersion;
import com.hartwig.hmftools.serve.transvar.Transvar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ProteinResolverFactory {

    private ProteinResolverFactory() {
    }

    @NotNull
    public static ProteinResolver transvarWithRefGenome(@NotNull RefGenomeVersion refGenomeVersion, @NotNull String refGenomeFastaFile)
            throws FileNotFoundException {
        return Transvar.withRefGenome(refGenomeVersion, refGenomeFastaFile);
    }

    @NotNull
    public static ProteinResolver dummy() {
        return new ProteinResolver() {
            @NotNull
            @Override
            public List<VariantHotspot> resolve(@NotNull final String gene, @Nullable final String specificTranscript,
                    @NotNull final String proteinAnnotation) {
                return Lists.newArrayList();
            }

            @NotNull
            @Override
            public Set<String> unresolvedProteinAnnotations() {
                return Sets.newHashSet();
            }
        };
    }
}
