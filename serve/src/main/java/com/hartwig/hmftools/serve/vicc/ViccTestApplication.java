package com.hartwig.hmftools.serve.vicc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import com.hartwig.hmftools.serve.hotspot.ProteinResolverFactory;
import com.hartwig.hmftools.serve.vicc.fusion.FusionAnnotation;
import com.hartwig.hmftools.serve.vicc.range.GeneRangeAnnotation;
import com.hartwig.hmftools.vicc.datamodel.Feature;
import com.hartwig.hmftools.vicc.datamodel.ViccEntry;
import com.hartwig.hmftools.vicc.datamodel.ViccSource;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.jetbrains.annotations.NotNull;

public class ViccTestApplication {

    private static final Logger LOGGER = LogManager.getLogger(ViccTestApplication.class);

    private static final Set<ViccSource> VICC_SOURCES_TO_INCLUDE =
            Sets.newHashSet(ViccSource.CIVIC, ViccSource.JAX, ViccSource.ONCOKB, ViccSource.CGI);
    private static final Integer MAX_VICC_ENTRIES = null;

    public static void main(String[] args) throws IOException {
        Configurator.setRootLevel(Level.DEBUG);

        String hostname = InetAddress.getLocalHost().getHostName();
        LOGGER.debug("Running on '{}'", hostname);

        String viccJsonPath;
        String rangesTsv = null;
        String fusionTsv = null;
        String featureTypeTsv;

        if (hostname.toLowerCase().contains("datastore")) {
            viccJsonPath = "/data/common/dbs/serve/vicc/all.json";
            rangesTsv = System.getProperty("user.home") + "/tmp/rangesVicc.tsv";
            fusionTsv = System.getProperty("user.home") + "/tmp/fusionVicc.tsv";
            featureTypeTsv = System.getProperty("user.home") + "/tmp/featureTypesVicc.tsv";
        } else {
            viccJsonPath = System.getProperty("user.home") + "/hmf/projects/serve/vicc/all.json";
            featureTypeTsv = System.getProperty("user.home") + "/hmf/tmp/featureTypesVicc.tsv";
        }

        LOGGER.debug("Configured '{}' as the VICC json path", viccJsonPath);
        LOGGER.debug("Configured '{}' as the ranges output TSV", rangesTsv);
        LOGGER.debug("Configured '{}' as the fusion output TSV", fusionTsv);
        LOGGER.debug("Configured '{}' as the feature type output TSV", featureTypeTsv);

        List<ViccEntry> viccEntries = ViccReader.readAndCurateRelevantEntries(viccJsonPath, VICC_SOURCES_TO_INCLUDE, MAX_VICC_ENTRIES);
        ViccExtractor viccExtractor = ViccExtractorFactory.buildViccExtractor(ProteinResolverFactory.dummy());
        Map<ViccEntry, ViccExtractionResult> resultsPerEntry = viccExtractor.extractFromViccEntries(viccEntries);

        ViccUtil.writeFeatureTypesToTsv(featureTypeTsv, resultsPerEntry);

        if (rangesTsv != null) {
            writeRanges(rangesTsv, resultsPerEntry);
        }

        if (fusionTsv != null) {
            writeFusion(fusionTsv, resultsPerEntry);
        }
    }

    private static void writeFusion(@NotNull String fusionTsv, @NotNull Map<ViccEntry, ViccExtractionResult> resultsPerEntry)
            throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(fusionTsv));
        writer.write("Fusion" + "\t" + "Fusion_event" + "\n");

        for (Map.Entry<ViccEntry, ViccExtractionResult> entry : resultsPerEntry.entrySet()) {

            for (Map.Entry<Feature, FusionAnnotation> featureResult : entry.getValue().fusionsPerFeature().entrySet()) {
                FusionAnnotation geneFusionForFeature = featureResult.getValue();

                writer.write(geneFusionForFeature.fusion() + "\t" + geneFusionForFeature.fusionEvent() + "\n");
            }
        }
        writer.close();
    }

    private static void writeRanges(@NotNull String rangesTsv, @NotNull Map<ViccEntry, ViccExtractionResult> resultsPerEntry)
            throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(rangesTsv));
        writer.write("Gene" + "\t" + "chromosome" + "\t" + "start" + "\t" + "end" + "\t" + "event" + "\n");

        for (Map.Entry<ViccEntry, ViccExtractionResult> entry : resultsPerEntry.entrySet()) {
            for (Map.Entry<Feature, List<GeneRangeAnnotation>> featureResult : entry.getValue().geneRangesPerFeature().entrySet()) {

                List<GeneRangeAnnotation> geneRangeForFeature = featureResult.getValue();
                for (GeneRangeAnnotation geneRangeAnnotation : geneRangeForFeature) {
                    writer.write(
                            geneRangeAnnotation.gene() + "\t" + geneRangeAnnotation.chromosome() + "\t" + geneRangeAnnotation.start() + "\t"
                                    + geneRangeAnnotation.end() + "\t" + geneRangeAnnotation.event() + "\n");
                }
            }
        }
        writer.close();
    }
}
