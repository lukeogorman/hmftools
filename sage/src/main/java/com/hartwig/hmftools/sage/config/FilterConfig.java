package com.hartwig.hmftools.sage.config;

import static com.hartwig.hmftools.common.cli.Configs.defaultIntValue;

import com.hartwig.hmftools.sage.variant.SageVariantTier;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public interface FilterConfig {

    String HARD_FILTER = "hard_filter";
    String HARD_MIN_TUMOR_QUAL = "hard_min_tumor_qual";
    String HARD_MIN_TUMOR_ALT_SUPPORT = "hard_min_tumor_raw_alt_support";
    String HARD_MIN_TUMOR_BASE_QUALITY = "hard_min_tumor_raw_base_quality";
    String MIN_GERMLINE_VAF = "min_germline_vaf";

    int DEFAULT_HARD_MIN_TUMOR_QUAL_FILTERED = 30;
    int DEFAULT_HARD_MIN_TUMOR_BASE_QUALITY = 0;
    int DEFAULT_HARD_MIN_TUMOR_QUAL = 1;
    int DEFAULT_HARD_MIN_TUMOR_ALT_SUPPORT = 2;
    int DEFAULT_HARD_MAX_NORMAL_ALT_SUPPORT = 3;

    SoftFilterConfig NO_FILTER = ImmutableSoftFilterConfig.builder()
            .minTumorQual(0)
            .minTumorVaf(0)
            .minGermlineReadContextCoverage(0)
            .minGermlineReadContextCoverageAllosome(0)
            .maxGermlineVaf(1d)
            .maxGermlineRelativeQual(1d)
            .build();

    SoftFilterConfig DEFAULT_HOTSPOT_FILTER =
            ImmutableSoftFilterConfig.builder().from(NO_FILTER).minTumorQual(35).minTumorVaf(0.005).maxGermlineVaf(0.1).build();

    SoftFilterConfig DEFAULT_PANEL_FILTER = ImmutableSoftFilterConfig.builder()
            .from(NO_FILTER)
            .minTumorQual(100)
            .minTumorVaf(0.015)
            .maxGermlineVaf(0.04)
            .maxGermlineRelativeQual(0.04)
            .build();

    SoftFilterConfig DEFAULT_WIDE_FILTER = ImmutableSoftFilterConfig.builder()
            .from(NO_FILTER)
            .minTumorQual(150)
            .minTumorVaf(0.025)
            .minGermlineReadContextCoverage(10)
            .minGermlineReadContextCoverageAllosome(6)
            .maxGermlineVaf(0.04)
            .maxGermlineRelativeQual(0.04)
            .build();

    boolean hardFilter();

    int hardMinTumorQual();

    int hardMinTumorRawAltSupport();

    int hardMinTumorRawBaseQuality();

    default int hardMinTumorQualFiltered() {
        return DEFAULT_HARD_MIN_TUMOR_QUAL_FILTERED;
    }

    //TODO: Rename this... it isn't a hard filter
    default int hardMaxNormalAltSupport() {
        return DEFAULT_HARD_MAX_NORMAL_ALT_SUPPORT;
    }

    default int hotspotMinRawTumorVafToSkipQualCheck() {
        return 5;
    }

    default double minGermlineVaf() {
        return 0;
    }


    @NotNull
    SoftFilterConfig softHotspotFilter();

    @NotNull
    SoftFilterConfig softPanelFilter();

    @NotNull
    SoftFilterConfig softWideFilter();

    @NotNull
    static Options createOptions() {
        final Options options = new Options();

        options.addOption(HARD_FILTER, false, "Soft filters become hard");
        options.addOption(HARD_MIN_TUMOR_QUAL, true, "Hard minimum tumor quality [" + DEFAULT_HARD_MIN_TUMOR_QUAL + "]");
        options.addOption(HARD_MIN_TUMOR_ALT_SUPPORT, true, "Hard minimum tumor raw alt support [" + DEFAULT_HARD_MIN_TUMOR_ALT_SUPPORT + "]");
        options.addOption(HARD_MIN_TUMOR_BASE_QUALITY, true, "Hard minimum tumor raw base quality [" + DEFAULT_HARD_MIN_TUMOR_BASE_QUALITY + "]");

        SoftFilterConfig.createOptions("hotspot", DEFAULT_HOTSPOT_FILTER).getOptions().forEach(options::addOption);
        SoftFilterConfig.createOptions("panel", DEFAULT_PANEL_FILTER).getOptions().forEach(options::addOption);
        SoftFilterConfig.createOptions("wide", DEFAULT_WIDE_FILTER).getOptions().forEach(options::addOption);

        return options;
    }

    @NotNull
    static FilterConfig createConfig(@NotNull final CommandLine cmd) throws ParseException {
        return ImmutableFilterConfig.builder()
                .hardFilter(cmd.hasOption(HARD_FILTER))
                .hardMinTumorQual(defaultIntValue(cmd, HARD_MIN_TUMOR_QUAL, DEFAULT_HARD_MIN_TUMOR_QUAL))
                .hardMinTumorRawAltSupport(defaultIntValue(cmd, HARD_MIN_TUMOR_ALT_SUPPORT, DEFAULT_HARD_MIN_TUMOR_ALT_SUPPORT))
                .hardMinTumorRawBaseQuality(defaultIntValue(cmd, HARD_MIN_TUMOR_BASE_QUALITY, DEFAULT_HARD_MIN_TUMOR_BASE_QUALITY))
                .softHotspotFilter(SoftFilterConfig.createConfig(cmd, "hotspot", DEFAULT_HOTSPOT_FILTER))
                .softPanelFilter(SoftFilterConfig.createConfig(cmd, "panel", DEFAULT_PANEL_FILTER))
                .softWideFilter(SoftFilterConfig.createConfig(cmd, "wide", DEFAULT_WIDE_FILTER))
                .build();

    }

    @NotNull
    default SoftFilterConfig softConfig(@NotNull final SageVariantTier tier) {
        switch (tier) {
            case HOTSPOT:
                return softHotspotFilter();
            case PANEL:
                return softPanelFilter();
            default:
                return softWideFilter();
        }
    }

}
