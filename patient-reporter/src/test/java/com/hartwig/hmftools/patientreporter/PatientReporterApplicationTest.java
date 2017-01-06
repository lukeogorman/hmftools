package com.hartwig.hmftools.patientreporter;

import java.io.File;
import java.io.IOException;

import com.google.common.io.Resources;
import com.hartwig.hmftools.common.exception.HartwigException;
import com.hartwig.hmftools.patientreporter.slicing.Slicer;
import com.hartwig.hmftools.patientreporter.slicing.SlicerFactory;

import org.junit.Test;

public class PatientReporterApplicationTest {

    private static final String RUN_DIRECTORY = Resources.getResource("example").getPath();
    private static final String BED_DIRECTORY = Resources.getResource("bed").getPath();

    @Test
    public void canRunOnRunDirectory() throws IOException, HartwigException {
        final String bedFile = BED_DIRECTORY + File.separator + "valid.bed";
        final Slicer slicer = SlicerFactory.fromBedFile(bedFile);
        final ConsensusRule consensusRule = new ConsensusRule(slicer, slicer);
        final ConsequenceRule consequenceRule = new ConsequenceRule(slicer);

        final PatientReporterApplication app = new PatientReporterApplication(RUN_DIRECTORY, consensusRule,
                consequenceRule, null, false);
        app.run();
    }
}