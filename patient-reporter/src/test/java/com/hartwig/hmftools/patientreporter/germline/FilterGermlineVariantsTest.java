package com.hartwig.hmftools.patientreporter.germline;

import static com.hartwig.hmftools.patientreporter.PatientReporterTestFactory.createTestCopyNumberBuilder;

import static org.junit.Assert.*;

import java.io.IOException;
import java.security.acl.LastOwnerException;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.drivercatalog.DriverCategory;
import com.hartwig.hmftools.common.purple.PurityAdjuster;
import com.hartwig.hmftools.common.purple.copynumber.CopyNumberMethod;
import com.hartwig.hmftools.common.purple.gene.GeneCopyNumber;
import com.hartwig.hmftools.common.purple.gene.ImmutableGeneCopyNumber;
import com.hartwig.hmftools.common.purple.segment.SegmentSupport;
import com.hartwig.hmftools.common.variant.CodingEffect;
import com.hartwig.hmftools.common.variant.EnrichedSomaticVariant;
import com.hartwig.hmftools.common.variant.ImmutableEnrichedSomaticVariant;
import com.hartwig.hmftools.common.variant.SomaticVariantTestBuilderFactory;
import com.hartwig.hmftools.patientreporter.PatientReporterTestUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class FilterGermlineVariantsTest {

    private static final CodingEffect SPLICE = CodingEffect.SPLICE;
    private static final CodingEffect MISSENSE = CodingEffect.MISSENSE;
    private static final CodingEffect SYNONYMOUS = CodingEffect.SYNONYMOUS;
    private static final String RIGHT_GENE = "RIGHT";
    private static final String WRONG_GENE = "WRONG";
    private static final Logger LOGGER = LogManager.getLogger(FilterGermlineVariantsTest.class);

    @Test
    public void filteringONCOGenesForGermlineVariantsCheckONCO() throws IOException {
        List<GermlineVariant> germlineVariants = createTestGermlineVariantsONCOGene();
        GermlineGenesReporting germlineGenesReporting = PatientReporterTestUtil.testGermlineModel();
        Map<String, DriverCategory> driverCategoryMap = Maps.newHashMap();
        driverCategoryMap.put("BRCA2", DriverCategory.ONCO);

        List<GeneCopyNumber> geneCopyNumbers = Lists.newArrayList();
        String sampleId = "CPCT02990001T";
        List<EnrichedSomaticVariant> variants = Lists.newArrayList();

        List<GermlineVariant> filteredGermlineVariantONCO = FilterGermlineVariants.filteringReportedGermlineVariant(germlineVariants,
                germlineGenesReporting,
                driverCategoryMap,
                geneCopyNumbers,
                sampleId,
                variants);
        assertEquals(filteredGermlineVariantONCO.size(), 1);

        Map<String, DriverCategory> driverCategoryMapTSG = Maps.newHashMap();
        driverCategoryMapTSG.put("ATM", DriverCategory.TSG);

        List<GermlineVariant> filteredGermlineVariantTSG = FilterGermlineVariants.filteringReportedGermlineVariant(germlineVariants,
                germlineGenesReporting,
                driverCategoryMapTSG,
                geneCopyNumbers,
                sampleId,
                variants);
        assertEquals(filteredGermlineVariantTSG.size(), 0);
    }


    @NotNull
    private static ImmutableEnrichedSomaticVariant.Builder builder() {
        return SomaticVariantTestBuilderFactory.createEnriched().filter("PASS");
    }

    @NotNull
    private static List<GermlineVariant> createTestGermlineVariantsONCOGene() {
        List<GermlineVariant> germlineVariants = Lists.newArrayList();

        int totalReads = 112;
        int altReads = 67;
        double adjustedCopyNumber = 3D;

        germlineVariants.add(ImmutableGermlineVariant.builder()
                .passFilter(true)
                .gene("BRCA2")
                .hgvsCodingImpact("c.5946delT")
                .hgvsProteinImpact("p.Ser1982fs")
                .totalReadCount(totalReads)
                .alleleReadCount(altReads)
                .germlineStatus("HET")
                .adjustedCopyNumber(adjustedCopyNumber)
                .adjustedVAF(12)
                .minorAllelePloidy(1D)
                .biallelic(false)
                .build());

        return germlineVariants;
    }

    @NotNull
    private static List<GermlineVariant> createTestGermlineVariantsTSGGene() {
        List<GermlineVariant> germlineVariants = Lists.newArrayList();

        int totalReads = 112;
        int altReads = 67;
        double adjustedCopyNumber = 3D;

        germlineVariants.add(ImmutableGermlineVariant.builder()
                .passFilter(true)
                .gene("ATM")
                .hgvsCodingImpact("c.5946delT")
                .hgvsProteinImpact("p.Ser1982fs")
                .totalReadCount(totalReads)
                .alleleReadCount(altReads)
                .germlineStatus("HET")
                .adjustedCopyNumber(adjustedCopyNumber)
                .adjustedVAF(12)
                .minorAllelePloidy(1D)
                .biallelic(false)
                .build());

        return germlineVariants;
    }

}