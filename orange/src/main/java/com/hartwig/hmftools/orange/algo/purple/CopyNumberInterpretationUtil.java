package com.hartwig.hmftools.orange.algo.purple;

import com.hartwig.hmftools.common.drivercatalog.DriverCatalog;
import com.hartwig.hmftools.common.utils.Doubles;
import com.hartwig.hmftools.datamodel.purple.CopyNumberInterpretation;

import org.jetbrains.annotations.NotNull;

public class CopyNumberInterpretationUtil {

    private CopyNumberInterpretationUtil() {
    }

    @NotNull
    public static String display(CopyNumberInterpretation copyNumberInterpretation) {
        return copyNumberInterpretation.toString().toLowerCase().replaceAll("_", " ");
    }

    @NotNull
    public static CopyNumberInterpretation fromCNADriver(@NotNull DriverCatalog cnaDriver) {
        switch (cnaDriver.driver()) {
            case AMP:
                return CopyNumberInterpretation.FULL_GAIN;
            case PARTIAL_AMP:
                return CopyNumberInterpretation.PARTIAL_GAIN;
            case DEL:
                return Doubles.greaterThan(cnaDriver.maxCopyNumber(), 0.5) ? CopyNumberInterpretation.PARTIAL_LOSS : CopyNumberInterpretation.FULL_LOSS;
            default:
                throw new IllegalStateException("Driver not an AMP or DEL: " + cnaDriver);
        }
    }
}