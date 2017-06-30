package com.hartwig.hmftools.common.purity;

import static com.hartwig.hmftools.common.numeric.Doubles.greaterThan;

import com.hartwig.hmftools.common.numeric.Doubles;
import com.hartwig.hmftools.common.purple.gender.Gender;
import com.hartwig.hmftools.common.purple.purity.FittedPurity;

import org.jetbrains.annotations.NotNull;

public class PurityAdjuster {

    public static double impliedSamplePloidy(final double purity, final double normFactor) {
        return new PurityAdjuster(Gender.FEMALE, purity, normFactor).purityAdjustedCopyNumber("1", 1);
    }

    @NotNull
    private final Gender gender;
    private final double purity;
    private final double normFactor;

    public PurityAdjuster(@NotNull final Gender gender, @NotNull final FittedPurity fittedPurity) {
        this(gender, fittedPurity.purity(), fittedPurity.normFactor());
    }

    public PurityAdjuster(@NotNull final Gender gender, final double purity, final double normFactor) {
        this.gender = gender;
        this.purity = purity;
        this.normFactor = normFactor;
    }

    public double purityAdjustedCopyNumber(final String chromosome, final double ratio) {
        final double typicalCopyNumber = isMaleSexChromosome(chromosome) ? 1 : 2;
        return purityAdjustedCopyNumber(ratio, typicalCopyNumber);
    }

    private double purityAdjustedCopyNumber(final double ratio, final double typicalCopyNumber) {
        return Doubles.isZero(ratio) ? 0 : typicalCopyNumber + (2 * ratio - typicalCopyNumber * normFactor) / purity / normFactor;
    }

    public double purityAdjustedVAF(final double copyNumber, final double observedFrequency) {
        return purityAdjustedFrequency(copyNumber, observedFrequency, 0);
    }

    public double purityAdjustedBAF(final String chromosome, final double copyNumber, final double observedFrequency) {
        double typicalFrequency = isMaleSexChromosome(chromosome) ? 0 : 0.5;
        return purityAdjustedFrequency(copyNumber, observedFrequency, typicalFrequency);
    }

    private double purityAdjustedFrequency(final double copyNumber, final double observedFrequency, final double typicalFrequency) {
        assert (greaterThan(copyNumber, 0));
        assert (greaterThan(purity, 0));

        double normalPloidy = 2 * (1 - purity);
        double tumorPloidy = copyNumber * purity;
        double normalAmount = 2 * (1 - purity) * typicalFrequency;

        return (observedFrequency * (normalPloidy + tumorPloidy) - normalAmount) / copyNumber / purity;
    }

    private boolean isMaleSexChromosome(String chromosome) {
        return gender.equals(Gender.MALE) && (chromosome.equals("X") || chromosome.equals("Y"));
    }

}
