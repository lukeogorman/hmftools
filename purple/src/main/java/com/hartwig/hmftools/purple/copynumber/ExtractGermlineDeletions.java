package com.hartwig.hmftools.purple.copynumber;

import java.util.EnumSet;
import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.genome.chromosome.CobaltChromosomes;
import com.hartwig.hmftools.common.purple.copynumber.CopyNumberMethod;
import com.hartwig.hmftools.common.purple.region.FittedRegion;
import com.hartwig.hmftools.common.purple.region.GermlineStatus;
import com.hartwig.hmftools.common.purple.segment.SegmentSupport;
import com.hartwig.hmftools.common.utils.Doubles;

import org.jetbrains.annotations.NotNull;

public class ExtractGermlineDeletions
{
    private final CobaltChromosomes mCobaltChromosomes;

    public ExtractGermlineDeletions(final CobaltChromosomes cobaltChromosomes)
    {
        mCobaltChromosomes = cobaltChromosomes;
    }

    private List<CombinedRegion> extractChildren(
            final EnumSet<GermlineStatus> eligibleStatus, final CombinedRegion parent, final SegmentSupport parentNext)
    {
        final List<CombinedRegion> children = Lists.newArrayList();

        double baf = parent.tumorBAF();
        double copyNumber = parent.tumorCopyNumber();
        for(int i = 0; i < parent.regions().size(); i++)
        {
            final FittedRegion child = parent.regions().get(i);
            final SegmentSupport childNext = i == parent.regions().size() - 1 ? parentNext : parent.regions().get(i + 1).support();

            if(eligibleStatus.contains(child.germlineStatus()))
            {
                if(child.germlineStatus().equals(GermlineStatus.HET_DELETION))
                {
                    final double upperBound = upperBound(child);
                    if(Doubles.lessThan(upperBound, Math.min(0.5, copyNumber)))
                    {
                        children.add(createChild(child, upperBound, baf, childNext));
                    }
                }

                if(child.germlineStatus().equals(GermlineStatus.HOM_DELETION))
                {
                    children.add(createChild(child, child.refNormalisedCopyNumber(), baf, childNext));
                }
            }
        }

        return extendRight(children);
    }

    private static CombinedRegion createChild(final FittedRegion child, double newCopyNumber, double newBaf, SegmentSupport next)
    {
        final CombinedRegionImpl result = new CombinedRegionImpl(child);
        result.setTumorCopyNumber(method(child), newCopyNumber);
        result.setInferredTumorBAF(newBaf);
        result.setGermlineEndSupport(next);
        return result;
    }

    private static CopyNumberMethod method(final FittedRegion child)
    {
        switch(child.germlineStatus())
        {
            case HOM_DELETION:
                return CopyNumberMethod.GERMLINE_HOM_DELETION;
            default:
                return CopyNumberMethod.GERMLINE_HET2HOM_DELETION;
        }
    }

    private static List<CombinedRegion> extendRight(final List<CombinedRegion> children)
    {
        int i = 0;
        while(i < children.size() - 1)
        {
            final CombinedRegion target = children.get(i);
            final CombinedRegion neighbour = children.get(i + 1);

            if(target.region().germlineStatus().equals(neighbour.region().germlineStatus()) && target.end() + 1 == neighbour.start())
            {
                target.extendWithUnweightedAverage(neighbour.region());
                children.remove(i + 1);
            }
            else
            {
                i++;
            }
        }

        return children;
    }

    private double upperBound(final FittedRegion region)
    {
        double expectedNormalRatio = mCobaltChromosomes.get(region.chromosome()).actualRatio();
        return Math.max(region.tumorCopyNumber(), region.refNormalisedCopyNumber()) / (2 * Math.min(expectedNormalRatio,
                region.observedNormalRatio()));
    }
}
