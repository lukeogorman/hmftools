package com.hartwig.hmftools.amber;

import static com.hartwig.hmftools.amber.AmberConstants.*;
import static com.hartwig.hmftools.common.genome.refgenome.RefGenomeSource.REF_GENOME;
import static com.hartwig.hmftools.common.genome.refgenome.RefGenomeSource.REF_GENOME_CFG_DESC;
import static com.hartwig.hmftools.common.genome.refgenome.RefGenomeSource.addRefGenomeVersion;
import static com.hartwig.hmftools.common.samtools.BamUtils.addValidationStringencyOption;
import static com.hartwig.hmftools.common.utils.TaskExecutor.addThreadOptions;
import static com.hartwig.hmftools.common.utils.TaskExecutor.parseThreads;
import static com.hartwig.hmftools.common.utils.config.CommonConfig.REFERENCE;
import static com.hartwig.hmftools.common.utils.config.CommonConfig.REFERENCE_BAM;
import static com.hartwig.hmftools.common.utils.config.CommonConfig.REFERENCE_BAM_DESC;
import static com.hartwig.hmftools.common.utils.config.CommonConfig.REFERENCE_DESC;
import static com.hartwig.hmftools.common.utils.config.CommonConfig.TUMOR;
import static com.hartwig.hmftools.common.utils.config.CommonConfig.TUMOR_BAM;
import static com.hartwig.hmftools.common.utils.config.CommonConfig.TUMOR_BAM_DESC;
import static com.hartwig.hmftools.common.utils.config.CommonConfig.TUMOR_DESC;
import static com.hartwig.hmftools.common.utils.config.ConfigUtils.addLoggingOptions;
import static com.hartwig.hmftools.common.utils.file.FileWriterUtils.addOutputDir;
import static com.hartwig.hmftools.common.utils.file.FileWriterUtils.checkCreateOutputDir;
import static com.hartwig.hmftools.common.utils.file.FileWriterUtils.parseOutputDir;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.genome.refgenome.RefGenomeVersion;
import com.hartwig.hmftools.common.samtools.BamUtils;
import com.hartwig.hmftools.common.utils.config.ConfigBuilder;

import htsjdk.samtools.ValidationStringency;

public class AmberConfig
{
    public final String TumorId;
    public final String TumorBam;
    public final List<String> ReferenceIds;
    public final List<String> ReferenceBams;

    public final String BafLociPath;
    public final RefGenomeVersion RefGenVersion;
    public final String RefGenomeFile;

    public final int TumorOnlyMinSupport;
    public final double TumorOnlyMinVaf;
    public final int TumorOnlyMinDepth;
    public final int MinBaseQuality;
    public final int MinMappingQuality;
    public final double MinDepthPercent;
    public final double MaxDepthPercent;
    public final double MinHetAfPercent;
    public final double MaxHetAfPercent;
    public final boolean WriteUnfilteredGermline;

    public final String OutputDir;
    public final ValidationStringency BamStringency;
    public final int Threads;

    public static final Logger AMB_LOGGER = LogManager.getLogger(AmberConfig.class);

    private static final String SAMPLE_DELIM = ",";
    private static final String LOCI_FILE = "loci";

    private static final String TUMOR_ONLY_MIN_SUPPORT = "tumor_only_min_support";
    private static final String TUMOR_ONLY_MIN_VAF = "tumor_only_min_vaf";
    private static final String TUMOR_ONLY_MIN_DEPTH = "tumor_only_min_depth";
    private static final String MIN_BASE_QUALITY = "min_base_quality";
    private static final String MIN_MAP_QUALITY = "min_map_quality";
    private static final String MIN_DEPTH_PERC = "min_depth_percent";
    private static final String MAX_DEPTH_PERC = "max_depth_percent";
    private static final String MIN_HIT_AT_PERC = "min_het_af_percent";
    private static final String MAX_HIT_AT_PERC = "max_het_af_percent";
    private static final String WRITE_UNFILTERED_GERMLINE = "write_unfiltered_germline";

    public AmberConfig(final ConfigBuilder configBuilder)
    {
        TumorId = configBuilder.getValue(TUMOR);
        TumorBam = configBuilder.getValue(TUMOR_BAM);

        ReferenceIds = Lists.newArrayList();
        ReferenceBams = Lists.newArrayList();

        ReferenceIds.addAll(Arrays.asList(configBuilder.getValue(REFERENCE).split(SAMPLE_DELIM)));
        ReferenceBams.addAll(Arrays.asList(configBuilder.getValue(REFERENCE_BAM).split(SAMPLE_DELIM)));

        BafLociPath = configBuilder.getValue(LOCI_FILE);

        RefGenVersion = RefGenomeVersion.from(configBuilder);
        RefGenomeFile = configBuilder.getValue(REF_GENOME);

        TumorOnlyMinSupport = configBuilder.getInteger(TUMOR_ONLY_MIN_SUPPORT);
        TumorOnlyMinVaf = configBuilder.getDecimal(TUMOR_ONLY_MIN_VAF);
        TumorOnlyMinDepth = configBuilder.getInteger(TUMOR_ONLY_MIN_DEPTH);
        MinBaseQuality = configBuilder.getInteger(MIN_BASE_QUALITY);
        MinMappingQuality = configBuilder.getInteger(MIN_MAP_QUALITY);
        MinDepthPercent = configBuilder.getDecimal(MIN_DEPTH_PERC);
        MaxDepthPercent = configBuilder.getDecimal(MAX_DEPTH_PERC);
        MinHetAfPercent = configBuilder.getDecimal(MIN_HIT_AT_PERC);
        MaxHetAfPercent = configBuilder.getDecimal(MAX_HIT_AT_PERC);

        WriteUnfilteredGermline = configBuilder.hasFlag(WRITE_UNFILTERED_GERMLINE);

        OutputDir = parseOutputDir(configBuilder);
        Threads = parseThreads(configBuilder);
        BamStringency = BamUtils.validationStringency(configBuilder);
    }

    public static void registerConfig(final ConfigBuilder configBuilder)
    {
        configBuilder.addConfigItem(TUMOR, TUMOR_DESC);
        configBuilder.addPath(TUMOR_BAM, false, TUMOR_BAM_DESC);

        configBuilder.addConfigItem(REFERENCE, REFERENCE_DESC);
        configBuilder.addPath(REFERENCE_BAM, false, REFERENCE_BAM_DESC);

        configBuilder.addPath(LOCI_FILE, true, "Path to BAF loci vcf file");

        addRefGenomeVersion(configBuilder);
        configBuilder.addPath(REF_GENOME, false, REF_GENOME_CFG_DESC + ", required when using CRAM files");

        configBuilder.addInteger(
                TUMOR_ONLY_MIN_SUPPORT, "Min support in ref and alt in tumor only mode", DEFAULT_TUMOR_ONLY_MIN_SUPPORT);

        configBuilder.addDecimal(TUMOR_ONLY_MIN_VAF, "Min VAF in ref and alt in tumor only mode", DEFAULT_TUMOR_ONLY_MIN_VAF);

        configBuilder.addInteger(
                TUMOR_ONLY_MIN_DEPTH, "Min depth in tumor only mode", DEFAULT_TUMOR_ONLY_MIN_DEPTH);

        configBuilder.addInteger(
                MIN_BASE_QUALITY, "Minimum quality for a base to be considered", DEFAULT_MIN_BASE_QUALITY);

        configBuilder.addInteger(
                MIN_MAP_QUALITY, "Minimum mapping quality for an alignment to be used", DEFAULT_MIN_MAPPING_QUALITY);

        configBuilder.addDecimal(MIN_DEPTH_PERC, "Min percentage of median depth", DEFAULT_MIN_DEPTH_PERCENTAGE);
        configBuilder.addDecimal(MAX_DEPTH_PERC, "Max percentage of median depth", DEFAULT_MAX_DEPTH_PERCENTAGE);
        configBuilder.addDecimal(MIN_HIT_AT_PERC, "Max heterozygous AF%", DEFAULT_MIN_HET_AF_PERCENTAGE);
        configBuilder.addDecimal(MAX_HIT_AT_PERC, "Max heterozygous AF%", DEFAULT_MAX_HET_AF_PERCENTAGE);

        configBuilder.addFlag(WRITE_UNFILTERED_GERMLINE, "Write all (unfiltered) germline points");

        addOutputDir(configBuilder);
        addThreadOptions(configBuilder);
        addValidationStringencyOption(configBuilder);
        addLoggingOptions(configBuilder);
    }

    public String primaryReference()
    {
        return ReferenceIds.get(0);
    }

    public List<String> allSamples()
    {
        List<String> samples = new ArrayList<>(ReferenceIds);
        samples.add(TumorId);
        return samples;
    }

    public boolean isTumorOnly() { return ReferenceBams.isEmpty() && TumorBam != null; }

    public boolean isGermlineOnly()
    {
        return !ReferenceBams.isEmpty() && TumorBam == null;
    }

    // use the tumor id if it is not null, otherwise primary reference Id
    public String getSampleId()
    {
        return TumorId != null ? TumorId : primaryReference();
    }

    public boolean isValid()
    {
        if(ReferenceIds.size() != ReferenceBams.size())
        {
            AMB_LOGGER.error("Each reference sample must have matching bam");
            return false;
        }

        if ((TumorId == null) != (TumorBam == null))
        {
            AMB_LOGGER.error("Unmatched: TumorId: {} and TumorBamPath: {}", TumorId, TumorBam);
            return false;
        }

        checkCreateOutputDir(OutputDir);

        if(!new File(BafLociPath).exists())
        {
            AMB_LOGGER.error("Unable to locate vcf file {}", BafLociPath);
            return false;
        }

        if(TumorBam != null && !new File(TumorBam).exists())
        {
            AMB_LOGGER.error("Unable to locate tumor bam file {}", TumorBam);
            return false;
        }

        if(!isTumorOnly())
        {
            for(String referenceBam : ReferenceBams)
            {
                if(!new File(referenceBam).exists())
                {
                    AMB_LOGGER.error("Unable to locate reference bam {}", referenceBam);
                    return false;
                }
            }
        }

        return true;
    }
}
