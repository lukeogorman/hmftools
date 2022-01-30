package com.hartwig.hmftools.cup.ref;

import static com.hartwig.hmftools.common.utils.FileWriterUtils.OUTPUT_DIR;
import static com.hartwig.hmftools.common.utils.FileWriterUtils.parseOutputDir;
import static com.hartwig.hmftools.cup.CuppaConfig.LOG_DEBUG;
import static com.hartwig.hmftools.cup.CuppaConfig.REF_SAMPLE_DATA_FILE;
import static com.hartwig.hmftools.cup.CuppaConfig.REF_SNV_COUNTS_FILE;
import static com.hartwig.hmftools.cup.CuppaConfig.REF_SNV_SAMPLE_POS_FREQ_FILE;
import static com.hartwig.hmftools.patientdb.dao.DatabaseAccess.addDatabaseCmdLineArgs;
import static com.hartwig.hmftools.patientdb.dao.DatabaseAccess.createDatabaseAccess;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.hartwig.hmftools.cup.rna.RefGeneExpression;
import com.hartwig.hmftools.cup.somatics.RefSomatics;
import com.hartwig.hmftools.patientdb.dao.DatabaseAccess;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

public class RefDataConfig
{
    public final String OutputDir;

    // reference data
    public final String SampleDataFile;
    public final String CohortSampleTraitsFile;
    public final String CohortSampleSvDataFile;
    public final String CohortSigContribsFile;
    public final String SnvPositionDataFile;
    public final String SnvCountsFile;
    public final String CohortFeaturesFile;
    public final String GeneExpFile;
    public final String AltSjFile;
    public final String FeatureOverrideFile;

    public final String SampleFeaturesDir;
    public final String SampleSomaticVcf;
    public final String SampleSvVcf;

    public final DatabaseAccess DbAccess;

    public final boolean WriteCohortFiles; // re-write data sourced from database or flat files into single cohort files

    // config strings

    // cohort files instead of extracting data from database
    private static final String REF_SAMPLE_TRAITS_FILE = "cohort_sample_traits_file";
    private static final String REF_SIG_CONTRIBS_FILE = "cohort_sig_contribs_file";
    private static final String REF_SV_DATA_FILE = "cohort_sv_data_file";
    private static final String REF_FEATURES_FILE = "cohort_features_file";

    private static final String REF_GENE_EXP_DATA_FILE = "ref_gene_exp_file";
    private static final String REF_ALT_SJ_DATA_FILE = "ref_alt_sj_file";

    private static final String SAMPLE_FEATURES_DIR = "sample_features_dir";
    private static final String SAMPLE_SOMATIC_VCF = "sample_somatic_vcf";
    private static final String SAMPLE_SV_VCF = "sample_sv_vcf";

    private static final String REF_FEATURE_OVERRIDE_FILE = "feature_override_file";
    public static final String GENDER_RATES = "gender_rates";
    private static final String WRITE_COHORT_FILES = "write_cohort_files";

    private static final String FILE_DELIM = ",";

    public RefDataConfig(final CommandLine cmd)
    {
        SampleDataFile = cmd.getOptionValue(REF_SAMPLE_DATA_FILE, "");
        CohortSampleTraitsFile = cmd.getOptionValue(REF_SAMPLE_TRAITS_FILE, "");
        CohortSigContribsFile = cmd.getOptionValue(REF_SIG_CONTRIBS_FILE, "");
        CohortSampleSvDataFile = cmd.getOptionValue(REF_SV_DATA_FILE, "");
        CohortFeaturesFile = cmd.getOptionValue(REF_FEATURES_FILE, "");

        SampleFeaturesDir = cmd.getOptionValue(SAMPLE_FEATURES_DIR, "");
        SampleSomaticVcf = cmd.getOptionValue(SAMPLE_SOMATIC_VCF, "");
        SampleSvVcf = cmd.getOptionValue(SAMPLE_SV_VCF, "");

        SnvPositionDataFile = cmd.getOptionValue(REF_SNV_SAMPLE_POS_FREQ_FILE, "");
        SnvCountsFile = cmd.getOptionValue(REF_SNV_COUNTS_FILE, "");

        GeneExpFile = cmd.getOptionValue(REF_GENE_EXP_DATA_FILE, "");
        AltSjFile = cmd.getOptionValue(REF_ALT_SJ_DATA_FILE, "");

        FeatureOverrideFile = cmd.getOptionValue(REF_FEATURE_OVERRIDE_FILE, "");

        DbAccess = createDatabaseAccess(cmd);

        OutputDir = parseOutputDir(cmd);

        WriteCohortFiles = cmd.hasOption(WRITE_COHORT_FILES);
    }

    public static final List<String> parseFileSet(final String filenames)
    {
        return Arrays.stream(filenames.split(FILE_DELIM, -1)).collect(Collectors.toList());
    }

    public static void addCmdLineArgs(Options options)
    {
        options.addOption(REF_SAMPLE_DATA_FILE, true, "Ref sample data file");
        options.addOption(REF_SAMPLE_TRAITS_FILE, true, "Ref sample cohort traits file");
        options.addOption(REF_SIG_CONTRIBS_FILE, true, "Ref sample cohort signature contributions file");
        options.addOption(REF_SV_DATA_FILE, true, "Ref sample cohort SV file");
        options.addOption(REF_SNV_SAMPLE_POS_FREQ_FILE, true, "Ref SNV position frequency matrix data file");
        options.addOption(REF_SNV_COUNTS_FILE, true, "Ref SNV trinucleotide matrix data file");
        options.addOption(REF_FEATURES_FILE, true, "Ref sample features data file");

        options.addOption(SAMPLE_FEATURES_DIR, true, "Ref sample directory containing features files");
        options.addOption(SAMPLE_SV_VCF, true, "Ref sample SV VCF path, wildcards allowed");
        options.addOption(SAMPLE_SOMATIC_VCF, true, "Ref sample somatic VCF path, wildcards allowed");

        options.addOption(REF_GENE_EXP_DATA_FILE, true, "Ref sample RNA gene expression cohort data file");
        options.addOption(REF_ALT_SJ_DATA_FILE, true, "Ref sample RNA alternate splice junction cohort data file");

        options.addOption(REF_FEATURE_OVERRIDE_FILE, true, "Ref feature override data file");

        options.addOption(GENDER_RATES, true, "Gender-rate overrides - format CancerType;MalePerc;FemalePerc, etc");
        options.addOption(WRITE_COHORT_FILES, false, "Re-write ref data as cohort files");

        addDatabaseCmdLineArgs(options);

        options.addOption(OUTPUT_DIR, true, "Path to output files");
        options.addOption(LOG_DEBUG, false, "Sets log level to Debug, off by default");

        RefGeneExpression.addCmdLineArgs(options);
        RefSomatics.addCmdLineArgs(options);
    }

}
