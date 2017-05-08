package com.hartwig.hmftools.purple;

import com.hartwig.hmftools.common.copynumber.CopyNumber;
import com.hartwig.hmftools.common.copynumber.cnv.CNVFileLoader;
import com.hartwig.hmftools.common.copynumber.cnv.CNVFileLoaderHelper;
import com.hartwig.hmftools.common.exception.EmptyFileException;
import com.hartwig.hmftools.common.exception.HartwigException;
import com.hartwig.hmftools.common.ratio.Ratio;
import com.hartwig.hmftools.common.ratio.txt.RatioFileLoader;
import com.hartwig.hmftools.common.variant.GermlineVariant;
import com.hartwig.hmftools.common.variant.vcf.VCFFileLoader;
import com.hartwig.hmftools.common.variant.vcf.VCFGermlineFile;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

import static com.hartwig.hmftools.common.slicing.SlicerFactory.sortedSlicer;
import static java.util.stream.Collectors.toList;

public class PurityPloidyEstimateApplication {

    private static final Logger LOGGER = LogManager.getLogger(PurityPloidyEstimateApplication.class);

    // Options
    private static final String RUN_DIRECTORY = "run_dir";
    private static final String VCF_EXTENSION = "vcf_extension";
    private static final String VCF_EXTENSION_DEFAULT = ".annotation.vcf";
    private static final String BED_FILE = "bed";
    private static final String FREEC_DIRECTORY = "freec_dir";


    public static void main(final String... args) throws ParseException, IOException, HartwigException {
        final Options options = createOptions();
        final CommandLine cmd = createCommandLine(options, args);

        final String runDirectory = cmd.getOptionValue(RUN_DIRECTORY);

        if (runDirectory == null) {
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Purity Ploidy Estimator (PURPLE)", options);
            System.exit(1);
        }

        LOGGER.info("Loading variant data");
        final String vcfExtention = defaultValue(cmd, VCF_EXTENSION, VCF_EXTENSION_DEFAULT);
        final VCFGermlineFile vcfFile = VCFFileLoader.loadGermlineVCF(runDirectory, vcfExtention);
        final List<GermlineVariant> variants = variants(cmd, vcfFile);
        final String refSample = vcfFile.refSample();
        final String tumorSample = vcfFile.tumorSample();

        LOGGER.info("Loading {} CopyNumber", tumorSample);
        final String freecDirectory = freecDirectory(cmd, runDirectory, refSample, tumorSample);
        final List<CopyNumber> copyNumbers = CNVFileLoader.loadCNV(freecDirectory, tumorSample);

        LOGGER.info("Loading {} Ratio data", tumorSample);
        final List<Ratio> tumorRatio = RatioFileLoader.loadTumorRatios(freecDirectory, tumorSample);
        final List<Ratio> normalRatio = RatioFileLoader.loadNormalRatios(freecDirectory, tumorSample);

    }

    private static String defaultValue(CommandLine cmd, String opt, String defaultValue) {
        return cmd.hasOption(opt) ? cmd.getOptionValue(opt) : defaultValue;
    }

    private static List<GermlineVariant> variants(CommandLine cmd, VCFGermlineFile file) throws IOException, EmptyFileException {
        if (cmd.hasOption(BED_FILE)) {
            final String bedFile = cmd.getOptionValue(BED_FILE);
            LOGGER.info("Slicing variants with bed file: " + bedFile);
            List<GermlineVariant> variants = file.variants().stream().filter(sortedSlicer(bedFile)).collect(toList());
            LOGGER.info("Slicing complete");
            return variants;
        } else {
            return file.variants();
        }
    }

    private static String freecDirectory(CommandLine cmd, String runDirectory, String refSample, String tumorSample) {
        return cmd.hasOption(FREEC_DIRECTORY)
                ? cmd.getOptionValue(FREEC_DIRECTORY)
                : CNVFileLoaderHelper.getFreecBasePath(runDirectory, refSample, tumorSample);
    }

    @NotNull
    private static Options createOptions() {
        final Options options = new Options();

        options.addOption(RUN_DIRECTORY, true, "The path containing the data for a single run");
        options.addOption(FREEC_DIRECTORY, true, "The path to the freec data. Defaults to ../copyNumber/sampleR_sampleT/freec/");
        options.addOption(VCF_EXTENSION, true, "VCF file extension. Defaults to " + VCF_EXTENSION_DEFAULT);
        options.addOption(BED_FILE, true, "Optionally apply slicing to VCF with specified bed file");

        return options;
    }

    @NotNull
    private static CommandLine createCommandLine(@NotNull final Options options, @NotNull final String... args)
            throws ParseException {
        final CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }
}
