package com.hartwig.hmftools.linx.visualiser.data;

import static com.hartwig.hmftools.linx.visualiser.data.Exons.sortedDownstreamExons;
import static com.hartwig.hmftools.linx.visualiser.data.Exons.sortedUpstreamExons;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.region.GenomeRegion;

import org.jetbrains.annotations.NotNull;

public class DisruptedGene
{

    @NotNull
    public static List<GenomeRegion> disruptedGeneRegions(@NotNull final Fusion fusion, @NotNull final List<Exon> exons)
    {

        final List<Exon> upStreamExons =
                sortedUpstreamExons(fusion, exons).stream().filter(x -> x.rank() >= fusion.fusedExonUp()).collect(Collectors
                        .toList());
        final List<Exon> downStreamExons =
                sortedDownstreamExons(fusion, exons).stream().filter(x -> x.rank() <= fusion.fusedExonDown()).collect(Collectors.toList());
        if (upStreamExons.isEmpty() || downStreamExons.isEmpty())
        {
            return Collections.emptyList();
        }

        final Exon finalIncludedUpExon = upStreamExons.get(0);
        final Exon finalExcludedUpExon = upStreamExons.get(upStreamExons.size() - 1);
        final GenomeRegion upGeneRegion = upGeneExcludedRegion(fusion, finalIncludedUpExon, finalExcludedUpExon);

        final Exon firstExcludedDownExon = downStreamExons.get(0);
        final Exon firstIncludedDownExon = downStreamExons.get(downStreamExons.size() - 1);
        final GenomeRegion downGeneRegion = downGeneExcludedRegion(fusion, firstExcludedDownExon, firstIncludedDownExon);

        return Lists.newArrayList(upGeneRegion, downGeneRegion);
    }

    @NotNull
    private static Gene downGeneExcludedRegion(@NotNull final Fusion fusion, @NotNull final Exon firstExcludedDownExon, @NotNull final Exon firstIncludedDoneExon)
    {
        return fusion.strandDown() < 0 ?
                ImmutableGene.builder()
                        .chromosome(firstExcludedDownExon.chromosome())
                        .end(firstExcludedDownExon.end())
                        .start(Math.min(fusion.positionDown(), firstIncludedDoneExon.end()))
                        .namePosition(0)
                        .name(fusion.geneDown())
                        .transcript(fusion.transcriptDown())
                        .build() :
                ImmutableGene.builder()
                        .chromosome(firstExcludedDownExon.chromosome())
                        .start(firstExcludedDownExon.start())
                        .end(Math.max(fusion.positionDown(), firstIncludedDoneExon.start()))
                        .namePosition(0)
                        .name(fusion.geneDown())
                        .transcript(fusion.transcriptDown())
                        .build();
    }

    @NotNull
    private static Gene upGeneExcludedRegion(@NotNull final Fusion fusion, @NotNull final Exon finalIncludedExon,
            @NotNull final Exon finalExcludedUpExon)
    {
        return fusion.strandUp() < 0 ?
                ImmutableGene.builder().chromosome(finalExcludedUpExon.chromosome())
                        .start(finalExcludedUpExon.start())
                        .end(Math.max(fusion.positionUp(), finalIncludedExon.start()))
                        .namePosition(0)
                        .name(fusion.geneUp())
                        .transcript(fusion.transcriptUp())
                        .build() :
                ImmutableGene.builder()
                        .chromosome(finalExcludedUpExon.chromosome())
                        .start(Math.min(fusion.positionUp(), finalIncludedExon.end()))
                        .end(finalExcludedUpExon.end())
                        .namePosition(0)
                        .name(fusion.geneUp())
                        .transcript(fusion.transcriptUp())
                        .build();
    }


}
