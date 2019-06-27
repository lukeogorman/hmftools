package com.hartwig.hmftools.patientreporter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.hartwig.hmftools.common.actionability.EvidenceItem;
import com.hartwig.hmftools.common.chord.ChordAnalysis;
import com.hartwig.hmftools.common.chord.ChordFileReader;
import com.hartwig.hmftools.common.ecrf.projections.PatientTumorLocation;
import com.hartwig.hmftools.common.ecrf.projections.PatientTumorLocationFunctions;
import com.hartwig.hmftools.common.lims.LimsGermlineReportingChoice;
import com.hartwig.hmftools.common.purple.gender.Gender;
import com.hartwig.hmftools.common.purple.gene.GeneCopyNumber;
import com.hartwig.hmftools.common.purple.gene.GeneCopyNumberFile;
import com.hartwig.hmftools.common.purple.purity.FittedPurityFile;
import com.hartwig.hmftools.common.purple.purity.PurityContext;
import com.hartwig.hmftools.common.region.GenomeRegion;
import com.hartwig.hmftools.common.variant.ClonalityCutoffKernel;
import com.hartwig.hmftools.common.variant.ClonalityFactory;
import com.hartwig.hmftools.common.variant.EnrichedSomaticVariant;
import com.hartwig.hmftools.common.variant.EnrichedSomaticVariantFactory;
import com.hartwig.hmftools.common.variant.SomaticVariant;
import com.hartwig.hmftools.common.variant.SomaticVariantFactory;
import com.hartwig.hmftools.patientreporter.actionability.ClinicalTrialFactory;
import com.hartwig.hmftools.patientreporter.actionability.ReportableEvidenceItemFactory;
import com.hartwig.hmftools.patientreporter.copynumber.CopyNumberAnalysis;
import com.hartwig.hmftools.patientreporter.copynumber.CopyNumberAnalyzer;
import com.hartwig.hmftools.patientreporter.structural.SvAnalysis;
import com.hartwig.hmftools.patientreporter.structural.SvAnalyzer;
import com.hartwig.hmftools.patientreporter.variants.ReportableVariant;
import com.hartwig.hmftools.patientreporter.variants.ReportableVariantAnalyzer;
import com.hartwig.hmftools.patientreporter.variants.germline.BachelorFile;
import com.hartwig.hmftools.patientreporter.variants.germline.FilterGermlineVariants;
import com.hartwig.hmftools.patientreporter.variants.germline.GermlineVariant;
import com.hartwig.hmftools.patientreporter.variants.somatic.SomaticVariantAnalysis;
import com.hartwig.hmftools.patientreporter.variants.somatic.SomaticVariantAnalyzer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;

class PatientReporter {
    private static final Logger LOGGER = LogManager.getLogger(PatientReporter.class);

    @NotNull
    private final BaseReportData baseReportData;
    @NotNull
    private final SequencedReportData sequencedReportData;
    @NotNull
    private final SvAnalyzer svAnalyzer;

    PatientReporter(@NotNull final BaseReportData baseReportData, @NotNull final SequencedReportData sequencedReportData,
            @NotNull final SvAnalyzer svAnalyzer) {
        this.baseReportData = baseReportData;
        this.sequencedReportData = sequencedReportData;
        this.svAnalyzer = svAnalyzer;
    }

    @NotNull
    public AnalysedPatientReport run(@NotNull String tumorSample, @NotNull String refSample, @NotNull String purplePurityTsv,
            @NotNull String purpleGeneCnvTsv, @NotNull String somaticVariantVcf, @Nullable String bachelorCsv,
            @NotNull String chordPredictionFile, @NotNull String circosFile, @Nullable String comments) throws IOException {
        PatientTumorLocation patientTumorLocation =
                PatientTumorLocationFunctions.findPatientTumorLocationForSample(baseReportData.patientTumorLocations(), tumorSample);

        SampleReport sampleReport = SampleReportFactory.fromLimsAndHospitalModel(tumorSample,
                refSample,
                baseReportData.limsModel(),
                baseReportData.hospitalModel(),
                patientTumorLocation);

        CopyNumberAnalysis copyNumberAnalysis = analyzeCopyNumbers(purplePurityTsv, purpleGeneCnvTsv, patientTumorLocation);
        SomaticVariantAnalysis somaticVariantAnalysis = analyzeSomaticVariants(tumorSample,
                somaticVariantVcf,
                copyNumberAnalysis.gender(),
                copyNumberAnalysis.purity(),
                patientTumorLocation);
        List<GermlineVariant> germlineVariantsToReport =
                analyzeGermlineVariants(tumorSample, bachelorCsv, copyNumberAnalysis, somaticVariantAnalysis);

        List<ReportableVariant> reportableVariants =
                ReportableVariantAnalyzer.mergeSomaticAndGermlineVariants(somaticVariantAnalysis.variantsToReport(),
                        somaticVariantAnalysis.driverCatalog(),
                        sequencedReportData.panelGeneModel().geneDriverCategoryMap(),
                        sequencedReportData.panelGeneModel().drupActionableGenes(),
                        germlineVariantsToReport,
                        sequencedReportData.germlineReportingModel(),
                        baseReportData.limsModel().germlineReportingChoice(tumorSample));

        SvAnalysis svAnalysis = analyzeStructuralVariants(copyNumberAnalysis, patientTumorLocation);
        ChordAnalysis chordAnalysis = analyzeChord(chordPredictionFile);

        String clinicalSummary = sequencedReportData.summaryModel().findSummaryForSample(tumorSample);

        List<EvidenceItem> allEvidenceItems = Lists.newArrayList();
        allEvidenceItems.addAll(somaticVariantAnalysis.evidenceItems());
        allEvidenceItems.addAll(copyNumberAnalysis.evidenceItems());
        allEvidenceItems.addAll(svAnalysis.evidenceItems());

        List<EvidenceItem> nonTrials = ReportableEvidenceItemFactory.extractNonTrials(allEvidenceItems);

        AnalysedPatientReport report = ImmutableAnalysedPatientReport.of(sampleReport,
                copyNumberAnalysis.hasReliablePurityFit(),
                copyNumberAnalysis.purity(),
                copyNumberAnalysis.ploidy(),
                clinicalSummary,
                nonTrials.stream().filter(EvidenceItem::isOnLabel).collect(Collectors.toList()),
                ClinicalTrialFactory.extractOnLabelTrials(allEvidenceItems),
                nonTrials.stream().filter(item -> !item.isOnLabel()).collect(Collectors.toList()),
                reportableVariants,
                somaticVariantAnalysis.microsatelliteIndelsPerMb(),
                somaticVariantAnalysis.tumorMutationalLoad(),
                somaticVariantAnalysis.tumorMutationalBurden(),
                chordAnalysis,
                copyNumberAnalysis.reportableGeneCopyNumbers(),
                svAnalysis.reportableFusions(),
                svAnalysis.reportableDisruptions(),
                circosFile,
                Optional.ofNullable(comments),
                baseReportData.signaturePath(),
                baseReportData.logoRVAPath(),
                baseReportData.logoCompanyPath());

        logReportToStdOut(report);

        return report;
    }

    @NotNull
    private CopyNumberAnalysis analyzeCopyNumbers(@NotNull String purplePurityTsv, @NotNull String purpleGeneCnvTsv,
            @Nullable PatientTumorLocation patientTumorLocation) throws IOException {
        LOGGER.info("Loading purple purity data from {}", purplePurityTsv);
        final PurityContext purityContext = FittedPurityFile.read(purplePurityTsv);
        LOGGER.info(" Purple purity " + purityContext.bestFit().purity());
        LOGGER.info(" Purple average tumor ploidy: " + purityContext.bestFit().ploidy());
        LOGGER.info(" Purple status " + purityContext.status());

        LOGGER.info("Loading purple gene copynumber data from {}", purpleGeneCnvTsv);
        final List<GeneCopyNumber> exomeGeneCopyNumbers = GeneCopyNumberFile.read(purpleGeneCnvTsv);
        LOGGER.info(" Loaded {} gene copy numbers", exomeGeneCopyNumbers.size());

        LOGGER.info("Analyzing purple copy numbers");
        return CopyNumberAnalyzer.run(purityContext,
                exomeGeneCopyNumbers,
                sequencedReportData.panelGeneModel(),
                sequencedReportData.actionabilityAnalyzer(),
                patientTumorLocation);
    }

    @NotNull
    private SomaticVariantAnalysis analyzeSomaticVariants(@NotNull String sample, @NotNull String somaticVariantVcf, @NotNull Gender gender,
            double purity, @Nullable PatientTumorLocation patientTumorLocation) throws IOException {
        LOGGER.info("Loading somatic variants from {}", somaticVariantVcf);
        final List<SomaticVariant> variants = SomaticVariantFactory.passOnlyInstance().fromVCFFile(sample, somaticVariantVcf);
        LOGGER.info(" {} PASS somatic variants loaded", variants.size());

        LOGGER.info("Enriching somatic variants");
        final List<EnrichedSomaticVariant> enrichedSomaticVariants =
                enrich(variants, gender, purity, sequencedReportData.highConfidenceRegions(), sequencedReportData.refGenomeFastaFile());

        LOGGER.info("Analyzing somatic variants");
        return SomaticVariantAnalyzer.run(enrichedSomaticVariants,
                sequencedReportData.panelGeneModel().somaticVariantGenes(),
                sequencedReportData.panelGeneModel().geneDriverCategoryMap(),
                sequencedReportData.actionabilityAnalyzer(),
                patientTumorLocation);
    }

    @NotNull
    private static List<EnrichedSomaticVariant> enrich(@NotNull List<SomaticVariant> variants, @NotNull Gender gender, double purity,
            @NotNull Multimap<String, GenomeRegion> highConfidenceRegions, @NotNull IndexedFastaSequenceFile refGenomeFastaFile) {
        final double clonalPloidy = ClonalityCutoffKernel.clonalCutoff(variants);
        final ClonalityFactory clonalityFactory = new ClonalityFactory(gender, purity, clonalPloidy);

        final EnrichedSomaticVariantFactory enrichedSomaticFactory =
                new EnrichedSomaticVariantFactory(highConfidenceRegions, refGenomeFastaFile, clonalityFactory);

        return enrichedSomaticFactory.enrich(variants);
    }

    @NotNull
    private List<GermlineVariant> analyzeGermlineVariants(@NotNull String sample, @Nullable String bachelorCsv,
            @NotNull CopyNumberAnalysis copyNumberAnalysis, @NotNull SomaticVariantAnalysis somaticVariantAnalysis) throws IOException {
        if (bachelorCsv == null) {
            LOGGER.info("Skipping germline analysis - no bachelor CSV passed. Presumably no pathogenic germline variants found.");
            return Lists.newArrayList();
        }

        LOGGER.info("Loading germline variants from {}", bachelorCsv);
        List<GermlineVariant> variants =
                BachelorFile.loadBachelorFile(bachelorCsv).stream().filter(GermlineVariant::passFilter).collect(Collectors.toList());
        LOGGER.info(" {} PASS germline variants loaded", variants.size());

        LimsGermlineReportingChoice germlineChoice = baseReportData.limsModel().germlineReportingChoice(sample);
        if (germlineChoice == LimsGermlineReportingChoice.UNKNOWN) {
            LOGGER.info(" No germline reporting choice known. No germline variants will be reported!");
            return Lists.newArrayList();
        } else {
            LOGGER.info(" Patient has given the following germline consent: {}", germlineChoice);
            return FilterGermlineVariants.filterGermlineVariantsForReporting(variants,
                    sequencedReportData.germlineReportingModel(),
                    sequencedReportData.panelGeneModel().geneDriverCategoryMap(),
                    copyNumberAnalysis.exomeGeneCopyNumbers(),
                    somaticVariantAnalysis.variantsToReport());
        }
    }

    @NotNull
    private SvAnalysis analyzeStructuralVariants(@NotNull CopyNumberAnalysis copyNumberAnalysis,
            @Nullable PatientTumorLocation patientTumorLocation) {
        return svAnalyzer.run(copyNumberAnalysis.exomeGeneCopyNumbers(), sequencedReportData.actionabilityAnalyzer(), patientTumorLocation);
    }

    @NotNull
    private static ChordAnalysis analyzeChord(@NotNull String chordPredictionFile) throws IOException {
        LOGGER.info("Loading CHORD analysis from {}", chordPredictionFile);
        return ChordFileReader.read(chordPredictionFile);
    }

    private static void logReportToStdOut(@NotNull AnalysedPatientReport report) {
        LocalDate tumorArrivalDate = report.sampleReport().tumorArrivalDate();
        String formattedTumorArrivalDate =
                tumorArrivalDate != null ? DateTimeFormatter.ofPattern("dd-MMM-yyyy").format(tumorArrivalDate) : "?";

        LOGGER.info("Printing clinical & laboratory data for {}", report.sampleReport().sampleId());
        LOGGER.info(" Tumor sample arrived at HMF on {}", formattedTumorArrivalDate);
        LOGGER.info(" Primary tumor location: {} - {}",
                report.sampleReport().primaryTumorLocationString(),
                report.sampleReport().cancerSubTypeString());
        LOGGER.info("Shallow seq purity: {}", report.sampleReport().purityShallowSeq());
        LOGGER.info("Lab SOPs used: {}", report.sampleReport().labProcedures());

        List<ReportableVariant> variantsWithNotify =
                report.reportableVariants().stream().filter(ReportableVariant::notifyClinicalGeneticist).collect(Collectors.toList());
        LOGGER.info("Printing genomic analysis results for {}:", report.sampleReport().sampleId());
        LOGGER.info(" Clinical summary present: {}", (!report.clinicalSummary().isEmpty() ? "yes" : "no"));
        LOGGER.info(" Somatic variants to report: {}", report.reportableVariants().size());
        LOGGER.info("  Variants for which to notify clinical geneticist: {}", variantsWithNotify.size());
        LOGGER.info(" Microsatellite Indels per Mb: {}", report.microsatelliteIndelsPerMb());
        LOGGER.info(" Tumor mutational load: {}", report.tumorMutationalLoad());
        LOGGER.info(" Tumor mutational burden: {}", report.tumorMutationalBurden());
        LOGGER.info(" CHORD analysis HRD prediction: {}", report.chordAnalysis().hrdValue());
        LOGGER.info(" Copy number events to report: {}", report.geneCopyNumbers().size());
        LOGGER.info(" Gene fusions to report : {}", report.geneFusions().size());
        LOGGER.info(" Gene disruptions to report : {}", report.geneDisruptions().size());
        LOGGER.info("Printing actionability results for {}", report.sampleReport().sampleId());
        LOGGER.info(" Tumor-specific evidence items found: {}", report.tumorSpecificEvidence().size());
        LOGGER.info(" Off-label evidence items found: {}", report.offLabelEvidence().size());
        LOGGER.info(" Clinical trials matched to molecular profile: {}", report.clinicalTrials());

    }
}
