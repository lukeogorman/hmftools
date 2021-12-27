package com.hartwig.hmftools.purple.config;

import static com.hartwig.hmftools.common.utils.FileWriterUtils.checkAddDirSeparator;
import static com.hartwig.hmftools.purple.PurpleCommon.PPL_LOGGER;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class SampleDataFiles
{
    public final String SampleDataDir;
    public final String SvVcfFile;
    public final String RecoveredSvVcfFile;
    public final String SomaticVcfFile;
    public final String GermlineVcfFile;
    public final String AmberDirectory;
    public final String CobaltDirectory;

    public static final String SAMPLE_DIR = "sample_dir";
    private static final String AMBER = "amber";
    private static final String COBALT = "cobalt";
    private static String STRUCTURAL_VARIANTS = "structural_vcf";
    private static String STRUCTURAL_VARIANT_RECOVERY = "sv_recovery_vcf";
    public static String GERMLINE_VARIANTS = "germline_vcf";
    private static String SOMATIC_VARIANTS = "somatic_vcf";

    static void addOptions(final Options options)
    {
        options.addOption(SAMPLE_DIR, true,"Path to the sample's directory where expect to find cobalt, amber, gridss etc directories");

        options.addOption(COBALT,true,
                "Path to COBALT output directory. Required if <run_dir> not set, otherwise defaults to <run_dir>/cobalt.");

        options.addOption(AMBER, true,
                "Path to AMBER output directory. Required if <run_dir> not set, otherwise defaults to <run_dir>/amber");

        options.addOption(STRUCTURAL_VARIANTS, true, "Optional location of structural variant vcf for more accurate segmentation");
        options.addOption(STRUCTURAL_VARIANT_RECOVERY, true, "Optional location of failing structural variants that may be recovered");
        options.addOption(GERMLINE_VARIANTS, true, "Optional location of germline variants to enrich and process in driver catalog.");
        options.addOption(SOMATIC_VARIANTS, true, "Optional location of somatic variant vcf to assist fitting in highly-diploid samples.");
    }

    public SampleDataFiles(final CommandLine cmd, final String sampleId) throws ParseException
    {
        SampleDataDir = cmd.hasOption(SAMPLE_DIR) ? checkAddDirSeparator(cmd.getOptionValue(SAMPLE_DIR)) : null;

        if(SampleDataDir != null)
        {
            AmberDirectory = SampleDataDir + "amber/";
            CobaltDirectory = SampleDataDir + "cobalt/";
        }
        else
        {
            AmberDirectory = cmd.getOptionValue(AMBER);
            CobaltDirectory = cmd.getOptionValue(COBALT);
        }

        SvVcfFile = getFilename(cmd, STRUCTURAL_VARIANTS, SampleDataDir, sampleId, ".gripss.somatic.filtered.vcf.gz", false);
        RecoveredSvVcfFile = getFilename(cmd, STRUCTURAL_VARIANT_RECOVERY, SampleDataDir, sampleId, ".gripss.somatic.vcf.gz", false);
        SomaticVcfFile = getFilename(cmd, SOMATIC_VARIANTS, SampleDataDir, sampleId, ".sage.somatic.filtered.pave.vcf.gz", false);
        GermlineVcfFile = getFilename(cmd, GERMLINE_VARIANTS, SampleDataDir, sampleId, ".sage.germline.filtered.pave.vcf.gz", false);
    }

    private String getFilename(
            final CommandLine cmd, final String config, final String sampleDataDir,
            final String sampleId, final String fileSuffix, boolean failOnNotFound) throws ParseException
    {
        if(cmd.hasOption(config))
            return cmd.getOptionValue(config);

        if(sampleDataDir == null)
        {
            if(failOnNotFound)
                throw new ParseException(String.format("missing %s or sample data directory", config));
            else
                return "";
        }

        final String filename = sampleDataDir + sampleId + fileSuffix;

        if(Files.exists(Paths.get(filename)))
            return filename;

        if(failOnNotFound)
            throw new ParseException("missing sample data file: " + filename);
        else
            return "";
    }

}
