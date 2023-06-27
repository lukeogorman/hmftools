package com.hartwig.hmftools.sage;

import static com.hartwig.hmftools.common.ensemblcache.EnsemblDataCache.addEnsemblDir;
import static com.hartwig.hmftools.common.utils.FileDelimiters.ITEM_DELIM;
import static com.hartwig.hmftools.common.utils.FileWriterUtils.checkAddDirSeparator;
import static com.hartwig.hmftools.sage.SageCommon.SAMPLE_DELIM;
import static com.hartwig.hmftools.sage.SageCommon.SG_LOGGER;
import static com.hartwig.hmftools.sage.SageConfig.registerCommonConfig;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hartwig.hmftools.common.utils.config.ConfigBuilder;
import com.hartwig.hmftools.sage.filter.FilterConfig;

import org.apache.logging.log4j.util.Strings;

public class SageCallConfig
{
    public final SageConfig Common;

    public final List<String> TumorIds;
    public final List<String> TumorBams;
    public final String HighConfidenceBed;
    public final String CoverageBed;
    public final String PanelBed;
    public final String Hotspots;
    public final boolean PanelOnly;
    public final Set<Integer> SpecificPositions;

    private final String mResourceDir;

    private static final String TUMOR = "tumor";
    private static final String TUMOR_BAM = "tumor_bam";
    private static final String COVERAGE_BED = "coverage_bed";
    private static final String RESOURCE_DIR = "resource_dir";
    private static final String HIGH_CONFIDENCE_BED = "high_confidence_bed";
    private static final String PANEL_BED = "panel_bed";
    private static final String HOTSPOTS = "hotspots";
    private static final String PANEL_ONLY = "panel_only";
    private static final String SPECIFIC_POSITIONS = "specific_positions";

    public SageCallConfig(final String version, final ConfigBuilder configBuilder)
    {
        Common = new SageConfig(version, configBuilder);

        TumorIds = Lists.newArrayList();
        if(configBuilder.hasValue(TUMOR))
        {
            TumorIds.addAll(Arrays.asList(configBuilder.getValue(TUMOR).split(SAMPLE_DELIM)));
        }

        TumorBams = Lists.newArrayList();

        if(configBuilder.hasValue(TUMOR_BAM))
        {
            Arrays.stream(configBuilder.getValue(TUMOR_BAM, Strings.EMPTY).split(SAMPLE_DELIM))
                    .forEach(x -> TumorBams.add(Common.SampleDataDir + x));
        }

        mResourceDir = checkAddDirSeparator(configBuilder.getValue(RESOURCE_DIR, ""));
        PanelBed = getReferenceFile(configBuilder, PANEL_BED);
        CoverageBed = getReferenceFile(configBuilder, COVERAGE_BED);
        HighConfidenceBed = getReferenceFile(configBuilder, HIGH_CONFIDENCE_BED);
        Hotspots = getReferenceFile(configBuilder, HOTSPOTS);

        PanelOnly = configBuilder.hasFlag(PANEL_ONLY);

        SpecificPositions = Sets.newHashSet();
        if(configBuilder.hasValue(SPECIFIC_POSITIONS))
        {
            final String positionList = configBuilder.getValue(SPECIFIC_POSITIONS, Strings.EMPTY);
            if(!positionList.isEmpty())
            {
                Arrays.stream(positionList.split(ITEM_DELIM)).forEach(x -> SpecificPositions.add(Integer.parseInt(x)));
            }
        }
    }

    public boolean isValid()
    {
        if(!Common.isValid())
            return false;

        if(TumorIds.size() != TumorBams.size())
        {
            SG_LOGGER.error("Each tumor sample must have matching bam");
            return false;
        }

        for(String tumorBam : TumorBams)
        {
            if(!new File(tumorBam).exists())
            {
                SG_LOGGER.error("Unable to locate tumor bam({})", tumorBam);
                return false;
            }
        }

        if(TumorIds.isEmpty())
        {
            SG_LOGGER.error("At least one tumor must be supplied");
            return false;
        }

        return true;
    }

    private String getReferenceFile(final ConfigBuilder configBuilder, final String config)
    {
        if(!configBuilder.hasValue(config))
            return "";

        if(mResourceDir.isEmpty())
            return configBuilder.getValue(config);

        return mResourceDir + configBuilder.getValue(config);
    }

    public static void registerConfig(final ConfigBuilder configBuilder)
    {
        configBuilder.addConfigItem(TUMOR, true, "Tumor sample, or collection separated by ','");
        configBuilder.addConfigItem(TUMOR_BAM, true, "Tumor bam file(s)");


        configBuilder.addConfigItem(HIGH_CONFIDENCE_BED, false, "High confidence regions bed file");
        configBuilder.addPath(RESOURCE_DIR, false, "Path to Sage resource files");
        configBuilder.addConfigItem(PANEL_BED, false, "Panel regions bed file");
        configBuilder.addConfigItem(PANEL_ONLY, false, "Only examine panel for variants");
        configBuilder.addConfigItem(HOTSPOTS, false, "Hotspots");
        configBuilder.addConfigItem(COVERAGE_BED, false, "Coverage is calculated for optionally supplied bed");

        registerCommonConfig(configBuilder);
        FilterConfig.registerConfig(configBuilder);
        addEnsemblDir(configBuilder);

        // debug
        configBuilder.addConfigItem(SPECIFIC_POSITIONS, "Run for specific positions(s) separated by ';', for debug purposes");
    }

    public SageCallConfig()
    {
        Common = new SageConfig();
        TumorIds = Lists.newArrayList();
        TumorBams = Lists.newArrayList();
        HighConfidenceBed = "highConf";
        CoverageBed = "coverage";
        PanelBed = "panel";
        Hotspots = "hotspots";
        PanelOnly = false;
        SpecificPositions = Sets.newHashSet();
        mResourceDir = "";
    }




}