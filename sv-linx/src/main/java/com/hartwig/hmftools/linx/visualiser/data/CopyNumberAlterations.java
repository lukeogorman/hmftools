package com.hartwig.hmftools.linx.visualiser.data;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.numeric.Doubles;
import com.hartwig.hmftools.common.region.GenomeRegion;
import com.hartwig.hmftools.linx.visualiser.file.VisCopyNumberFile;

import org.jetbrains.annotations.NotNull;

public class CopyNumberAlterations
{

    // From DB: select sampleId, chromosome, start, end, copyNumber, baf from copyNumber where sampleId = 'xxxxx';

    @NotNull
    public static List<CopyNumberAlteration> copyNumbers(double copyNumberDistancePercent, long copyNumberDistanceMax, @NotNull final List<CopyNumberAlteration> alterations,  @NotNull final List<GenomeRegion> span)
    {
        final List<CopyNumberAlteration> result = Lists.newArrayList();

        for (int i = 0; i < alterations.size(); i++)
        {
            CopyNumberAlteration alteration = alterations.get(i);
            final String contig = alteration.chromosome();
            final List<GenomeRegion> chromosomeSegments =
                    span.stream().filter(x -> x.chromosome().equals(contig)).collect(Collectors.toList());
            if (!chromosomeSegments.isEmpty())
            {
                long minTrackPosition = chromosomeSegments.stream().mapToLong(GenomeRegion::start).min().orElse(0);
                long maxTrackPosition = chromosomeSegments.stream().mapToLong(GenomeRegion::end).max().orElse(0);
                long chromosomeDistance = maxTrackPosition - minTrackPosition;
                long additional = Math.min(copyNumberDistanceMax, Math.round(copyNumberDistancePercent * chromosomeDistance));
                minTrackPosition = minTrackPosition - additional;
                maxTrackPosition = maxTrackPosition + additional;

                if (alteration.end() >= minTrackPosition && alteration.start() <= maxTrackPosition)
                {

                    boolean isStartDecreasing = i > 0 && lessThan(alteration, alterations.get(i - 1));
                    long startPosition = isStartDecreasing ? alteration.start() - 1 : alteration.start();

                    boolean isEndIncreasing = i < alterations.size() - 1 && lessThan(alteration, alterations.get(i + 1));
                    long endPosition = isEndIncreasing ? alteration.end() + 1 : alteration.end();

                    result.add(ImmutableCopyNumberAlteration.builder()
                            .from(alteration)
                            .start(Math.max(minTrackPosition, startPosition))
                            .end(Math.min(maxTrackPosition, endPosition))
                            .truncated(minTrackPosition > startPosition || maxTrackPosition < endPosition)
                            .build());
                }
            }
        }

        return result;
    }

    private static boolean lessThan(@NotNull final CopyNumberAlteration first, @NotNull final CopyNumberAlteration second)
    {
        return first.chromosome().equals(second.chromosome()) && Doubles.lessThan(first.copyNumber(), second.copyNumber());
    }

    @NotNull
    public static List<CopyNumberAlteration> read(@NotNull final String fileName) throws IOException
    {
        return VisCopyNumberFile.read(fileName).stream().map(CopyNumberAlterations::fromVis).collect(Collectors.toList());
    }

    @NotNull
    private static CopyNumberAlteration fromVis(@NotNull final VisCopyNumberFile visCopyNumberFile)
    {
        return ImmutableCopyNumberAlteration.builder()
                .sampleId(visCopyNumberFile.SampleId)
                .chromosome(visCopyNumberFile.Chromosome)
                .start(visCopyNumberFile.Start)
                .end(visCopyNumberFile.End)
                .copyNumber(visCopyNumberFile.CopyNumber)
                .baf(visCopyNumberFile.BAF)
                .truncated(false)
                .build();
    }

}
