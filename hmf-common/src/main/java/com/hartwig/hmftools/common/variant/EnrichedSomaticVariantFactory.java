package com.hartwig.hmftools.common.variant;

import static com.hartwig.hmftools.common.variant.ImmutableEnrichedSomaticVariant.Builder;
import static com.hartwig.hmftools.common.variant.ImmutableEnrichedSomaticVariant.builder;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.hartwig.hmftools.common.purple.repeat.RepeatContextFactory;
import com.hartwig.hmftools.common.region.GenomeRegion;
import com.hartwig.hmftools.common.region.GenomeRegionSelector;
import com.hartwig.hmftools.common.region.GenomeRegionSelectorFactory;
import com.hartwig.hmftools.common.region.bed.BEDFileLookup;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;

public class EnrichedSomaticVariantFactory {

    private static final Logger LOGGER = LogManager.getLogger(EnrichedSomaticVariantFactory.class);

    @NotNull
    private final GenomeRegionSelector<GenomeRegion> highConfidenceSelector;
    @NotNull
    private final IndexedFastaSequenceFile reference;
    @NotNull
    private final BEDFileLookup mappabilityLookup;

    private int unmatchedAnnotations;

    public EnrichedSomaticVariantFactory(@NotNull final Multimap<String, GenomeRegion> highConfidenceRegions,
            @NotNull final IndexedFastaSequenceFile reference, @NotNull final BEDFileLookup mappabilityLookup) {
        highConfidenceSelector = GenomeRegionSelectorFactory.create(highConfidenceRegions);
        this.reference = reference;
        this.mappabilityLookup = mappabilityLookup;
    }

    public List<EnrichedSomaticVariant> enrich(final List<PurityAdjustedSomaticVariant> variants) throws IOException {

        final List<EnrichedSomaticVariant> result = Lists.newArrayList();

        for (PurityAdjustedSomaticVariant variant : variants) {
            result.add(enrich(variant));
        }

        if (unmatchedAnnotations > 0) {
            LOGGER.warn("There were {} unmatched annotated genes.", unmatchedAnnotations);
        }

        return result;
    }

    @NotNull
    private EnrichedSomaticVariant enrich(@NotNull final PurityAdjustedSomaticVariant variant) throws IOException {
        double mappability = Math.round(mappabilityLookup.score(variant) * 1000) / 1000d;

        final Builder builder = createBuilder(variant).mappability(mappability);

        highConfidenceSelector.select(variant).ifPresent(x -> inHighConfidenceRegion(builder));
        addAnnotations(builder, variant);
        addTrinucleotideContext(builder, variant);
        addGenomeContext(builder, variant);

        return builder.build();
    }

    private void addAnnotations(@NotNull final Builder builder, @NotNull final SomaticVariant variant) {
        final List<VariantAnnotation> annotations = variant.annotations();
        if (!annotations.isEmpty()) {
            // MIVO: get the first annotation for now, eventually we will want all
            final VariantAnnotation variantAnnotation = variant.annotations().get(0);
            variant.annotations().forEach(annotation -> {
                if (!annotation.gene().equals(variantAnnotation.gene())) {
                    unmatchedAnnotations++;
                    LOGGER.debug("Annotated gene (" + annotation.gene() + ") does not match gene expected from first annotation ( "
                            + variantAnnotation.gene() + ") for variant: " + variant);
                }
            });
            builder.gene(variantAnnotation.gene());
            builder.effect(variantAnnotation.consequenceString());
        }
    }

    @NotNull
    private static Builder createBuilder(@NotNull final SomaticVariant variant) {

        return builder().from(variant)
                .trinucleotideContext("")
                .microhomology("")
                .refGenomeContext("")
                .gene("")
                .effect("")
                .repeatCount(0)
                .repeatSequence("")
                .highConfidenceRegion(false)
                .adjustedCopyNumber(0)
                .clonality(Clonality.UNKNOWN)
                .lossOfHeterozygosity(false)
                .adjustedVAF(0);
    }

    private void addGenomeContext(@NotNull final Builder builder, @NotNull final SomaticVariant variant) {
        long positionBeforeEvent = variant.position();
        long start = Math.max(positionBeforeEvent - 100, 1);
        long maxEnd = reference.getSequenceDictionary().getSequence(variant.chromosome()).getSequenceLength() - 1;
        long end = Math.min(positionBeforeEvent + 100, maxEnd);
        int relativePosition = (int) (positionBeforeEvent - start);
        final String sequence = reference.getSubsequenceAt(variant.chromosome(), start, end).getBaseString();
        builder.refGenomeContext(sequence);

        RepeatContextFactory.repeats(relativePosition, sequence, variant.ref(), variant.alt())
                .ifPresent(x -> builder.repeatSequence(x.sequence()).repeatCount(x.count()));

        if (variant.ref().length() != variant.alt().length()) {
            final String microhomology = Microhomology.microhomology(relativePosition, sequence, variant.ref(), variant.alt());
            builder.microhomology(microhomology);
        }
    }

    private void addTrinucleotideContext(@NotNull final Builder builder, @NotNull final SomaticVariant variant) {
        final ReferenceSequence sequence =
                reference.getSubsequenceAt(variant.chromosome(), Math.max(1, variant.position() - 1), variant.position() + 1);
        builder.trinucleotideContext(sequence.getBaseString());
    }

    private Builder inHighConfidenceRegion(@NotNull final Builder builder) {
        return builder.highConfidenceRegion(true);
    }
}
