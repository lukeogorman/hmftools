package com.hartwig.hmftools.pave;

import static com.hartwig.hmftools.common.drivercatalog.panel.DriverGenePanelConfig.DRIVER_GENE_PANEL_OPTION;
import static com.hartwig.hmftools.common.drivercatalog.panel.DriverGenePanelConfig.DRIVER_GENE_PANEL_OPTION_DESC;
import static com.hartwig.hmftools.common.ensemblcache.EnsemblDataCache.addEnsemblDir;
import static com.hartwig.hmftools.common.genome.refgenome.RefGenomeSource.REF_GENOME;
import static com.hartwig.hmftools.common.genome.refgenome.RefGenomeSource.REF_GENOME_CFG_DESC;
import static com.hartwig.hmftools.common.genome.refgenome.RefGenomeVersion.REF_GENOME_VERSION;
import static com.hartwig.hmftools.common.genome.refgenome.RefGenomeVersion.REF_GENOME_VERSION_CFG_DESC;
import static com.hartwig.hmftools.common.genome.refgenome.RefGenomeVersion.V37;
import static com.hartwig.hmftools.common.utils.ConfigUtils.addLoggingOptions;
import static com.hartwig.hmftools.common.utils.FileWriterUtils.addOutputDir;
import static com.hartwig.hmftools.common.utils.FileWriterUtils.parseOutputDir;

import com.hartwig.hmftools.common.genome.refgenome.RefGenomeVersion;
import com.hartwig.hmftools.pave.external.GnomadAnnotation;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class PaveConfig
{
    public final String SampleId;

    public final String VcfFile;
    public final RefGenomeVersion RefGenVersion;

    public final String OutputDir;
    public final String OutputVcfFile;
    public final boolean WriteTranscriptCsv;
    public final boolean CompareSnpEff;
    public final boolean WriteDiffs;
    public final boolean OnlyCanonical;
    public final boolean FilterPass;

    private static final String SAMPLE = "sample";
    private static final String VCF_FILE = "vcf_file";
    private static final String OUTPUT_VCF_FILE = "output_vcf_file";
    private static final String COMPARE_SNPEFF = "compare_snpeff";
    private static final String ONLY_CANONCIAL = "only_canonical";
    private static final String FILTER_PASS = "filter_pass";
    private static final String WRITE_DIFFS = "write_diffs";
    private static final String WRITE_TRANSCRIPT_CSV = "write_transcript_csv";

    public static final Logger PV_LOGGER = LogManager.getLogger(PaveConfig.class);

    public PaveConfig(final CommandLine cmd)
    {
        SampleId = cmd.getOptionValue(SAMPLE);
        VcfFile = cmd.getOptionValue(VCF_FILE);

        RefGenVersion = RefGenomeVersion.from(cmd.getOptionValue(REF_GENOME_VERSION, V37.toString()));

        OutputVcfFile = cmd.getOptionValue(OUTPUT_VCF_FILE);
        WriteTranscriptCsv = cmd.hasOption(WRITE_TRANSCRIPT_CSV);
        CompareSnpEff = cmd.hasOption(COMPARE_SNPEFF);
        WriteDiffs = cmd.hasOption(WRITE_DIFFS);
        OnlyCanonical = cmd.hasOption(ONLY_CANONCIAL);
        FilterPass = cmd.hasOption(FILTER_PASS);

        OutputDir = parseOutputDir(cmd);
    }

    public boolean isValid()
    {
        if(VcfFile == null)
            return false;

        return true;
    }

    @NotNull
    public static Options createOptions()
    {
        Options options = new Options();
        options.addOption(SAMPLE, true, "Name of sample");
        options.addOption(VCF_FILE, true, "VCF input file");
        options.addOption(OUTPUT_VCF_FILE, true, "Option VCF output file, otherwise will append 'pave' suffix to input filename");

        options.addOption(REF_GENOME, true, REF_GENOME_CFG_DESC);
        options.addOption(REF_GENOME_VERSION, true, REF_GENOME_VERSION_CFG_DESC);
        addEnsemblDir(options);
        options.addOption(DRIVER_GENE_PANEL_OPTION, true, DRIVER_GENE_PANEL_OPTION_DESC);

        options.addOption(COMPARE_SNPEFF, false, "Check against SnpEff annotations");
        options.addOption(WRITE_DIFFS, false, "Only write transcript diffs to CSV file");
        options.addOption(WRITE_TRANSCRIPT_CSV, false, "Write variant impacts per transcript to CSV");
        options.addOption(ONLY_CANONCIAL, false, "Only check canonical transcripts");
        options.addOption(FILTER_PASS, false, "Only annotate passing variants");

        GnomadAnnotation.addCmdLineArgs(options);

        addOutputDir(options);
        addLoggingOptions(options);

        return options;
    }
}