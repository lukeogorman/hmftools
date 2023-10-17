package com.hartwig.hmftools.orange;

import static com.hartwig.hmftools.common.pipeline.PipelineToolDirectories.CHORD_DIR;
import static com.hartwig.hmftools.common.pipeline.PipelineToolDirectories.CUPPA_DIR;
import static com.hartwig.hmftools.common.pipeline.PipelineToolDirectories.FLAGSTAT_DIR;
import static com.hartwig.hmftools.common.pipeline.PipelineToolDirectories.LINX_GERMLINE_DIR;
import static com.hartwig.hmftools.common.pipeline.PipelineToolDirectories.METRICS_DIR;
import static com.hartwig.hmftools.common.pipeline.PipelineToolDirectories.PEACH_DIR;
import static com.hartwig.hmftools.common.pipeline.PipelineToolDirectories.SAGE_GERMLINE_DIR;
import static com.hartwig.hmftools.common.pipeline.PipelineToolDirectories.SAGE_SOMATIC_DIR;
import static com.hartwig.hmftools.common.pipeline.PipelineToolDirectories.SIGS_DIR;
import static com.hartwig.hmftools.common.pipeline.PipelineToolDirectories.VIRUS_INTERPRETER_DIR;
import static com.hartwig.hmftools.common.utils.config.CommonConfig.CHORD_DIR_CFG;
import static com.hartwig.hmftools.common.utils.config.CommonConfig.CUPPA_DIR_CFG;
import static com.hartwig.hmftools.common.utils.config.CommonConfig.LINX_GERMLINE_DIR_CFG;
import static com.hartwig.hmftools.common.utils.config.CommonConfig.PEACH_DIR_CFG;
import static com.hartwig.hmftools.common.utils.config.CommonConfig.PIPELINE_SAMPLE_ROOT_DIR;
import static com.hartwig.hmftools.common.utils.config.CommonConfig.SAGE_DIR_CFG;
import static com.hartwig.hmftools.common.utils.config.CommonConfig.SAGE_GERMLINE_DIR_CFG;
import static com.hartwig.hmftools.common.utils.config.CommonConfig.SAMPLE_DATA_DIR_CFG;
import static com.hartwig.hmftools.common.utils.config.CommonConfig.SIGS_DIR_CFG;
import static com.hartwig.hmftools.common.utils.config.CommonConfig.VIRUS_DIR_CFG;
import static com.hartwig.hmftools.common.utils.file.FileWriterUtils.checkAddDirSeparator;
import static com.hartwig.hmftools.orange.OrangeConfig.REFERENCE_SAMPLE_ID;
import static com.hartwig.hmftools.orange.OrangeConfig.REF_SAMPLE_FLAGSTAT_FILE;
import static com.hartwig.hmftools.orange.OrangeConfig.REF_SAMPLE_WGS_METRICS_FILE;
import static com.hartwig.hmftools.orange.OrangeConfig.TUMOR_SAMPLE_ID;
import static com.hartwig.hmftools.orange.OrangeConfig.getMetricsFile;
import static com.hartwig.hmftools.orange.OrangeConfig.getToolDirectory;
import static com.hartwig.hmftools.orange.util.Config.fileIfExists;
import static com.hartwig.hmftools.orange.util.Config.optionalFileIfExists;

import com.hartwig.hmftools.common.chord.ChordDataFile;
import com.hartwig.hmftools.common.cuppa.CuppaDataFile;
import com.hartwig.hmftools.common.sage.SageCommon;
import com.hartwig.hmftools.common.sigs.SignatureAllocationFile;
import com.hartwig.hmftools.common.utils.config.ConfigBuilder;
import com.hartwig.hmftools.common.virus.AnnotatedVirusFile;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public interface OrangeWGSConfig
{
    @NotNull
    String sageGermlineGeneCoverageTsv();

    @NotNull
    String sageSomaticRefSampleBQRPlot();

    @NotNull
    String linxGermlineDataDirectory();

    @NotNull
    String annotatedVirusTsv();

    @NotNull
    String chordPredictionTxt();

    @NotNull
    String cuppaResultCsv();

    @NotNull
    String cuppaSummaryPlot();

    @Nullable
    String cuppaFeaturePlot();

    @Nullable
    String cuppaChartPlot();

    @NotNull
    String peachGenotypeTsv();

    @NotNull
    String sigsAllocationTsv();

    @NotNull
    String refSampleWGSMetricsFile();

    @NotNull
    String refSampleFlagstatFile();

    @Nullable
    static OrangeWGSConfig createConfig(@NotNull ConfigBuilder configBuilder)
    {
        ImmutableOrangeWGSConfig.Builder builder = ImmutableOrangeWGSConfig.builder();
        String tumorSampleId = configBuilder.getValue(TUMOR_SAMPLE_ID);
        String refSampleId = configBuilder.getValue(REFERENCE_SAMPLE_ID);

        // TODO it would seem to be better more direct to use experimentType here
        //  but that is currently determined based on Purple output.
        if(refSampleId != null)
        {
            return null;
        }

        // TODO share these from elsewhere?
        String pipelineSampleRootDir = checkAddDirSeparator(configBuilder.getValue(PIPELINE_SAMPLE_ROOT_DIR));
        String sampleDataDir = checkAddDirSeparator(configBuilder.getValue(SAMPLE_DATA_DIR_CFG));
        String sageSomaticDir = getToolDirectory(configBuilder, pipelineSampleRootDir, sampleDataDir, SAGE_DIR_CFG, SAGE_SOMATIC_DIR);

        // TODO sageGermline should be here or in WGS?
        String sageGermlineDir = getToolDirectory(
                configBuilder, pipelineSampleRootDir, sampleDataDir, SAGE_GERMLINE_DIR_CFG, SAGE_GERMLINE_DIR);

        builder.sageGermlineGeneCoverageTsv(fileIfExists(SageCommon.generateGeneCoverageFilename(sageGermlineDir, refSampleId)));

        // TODO why are somatic dir and ref sample id together here?
        builder.sageSomaticRefSampleBQRPlot(fileIfExists(SageCommon.generateBqrPlotFilename(sageSomaticDir, refSampleId)));

        String linxGermlineDir = getToolDirectory(
                configBuilder, pipelineSampleRootDir, sampleDataDir, LINX_GERMLINE_DIR_CFG, LINX_GERMLINE_DIR);
        if(linxGermlineDir == null)
        {
            // TODO want warnings here? also below
            return null;
        }

        builder.linxGermlineDataDirectory(linxGermlineDir);

        String virusDir = getToolDirectory(configBuilder, pipelineSampleRootDir, sampleDataDir, VIRUS_DIR_CFG, VIRUS_INTERPRETER_DIR);

        if(virusDir == null)
        {
            return null;
        }
        builder.annotatedVirusTsv(fileIfExists(AnnotatedVirusFile.generateFileName(virusDir, tumorSampleId)));

        String chordDir = getToolDirectory(configBuilder, pipelineSampleRootDir, sampleDataDir, CHORD_DIR_CFG, CHORD_DIR);
        if(chordDir == null)
        {
            return null;
        }
        builder.chordPredictionTxt(fileIfExists(ChordDataFile.generateFilename(chordDir, tumorSampleId)));

        String cuppaDir = getToolDirectory(configBuilder, pipelineSampleRootDir, sampleDataDir, CUPPA_DIR_CFG, CUPPA_DIR);

        if(cuppaDir == null)
        {
            return null;
        }

        builder.cuppaResultCsv(fileIfExists(CuppaDataFile.generateFilename(cuppaDir, tumorSampleId)));
        builder.cuppaSummaryPlot(fileIfExists(CuppaDataFile.generateReportSummaryPlotFilename(cuppaDir, tumorSampleId)));
        builder.cuppaFeaturePlot(optionalFileIfExists(CuppaDataFile.generateReportFeaturesPlotFilename(cuppaDir, tumorSampleId)));
        builder.cuppaChartPlot(fileIfExists(CuppaDataFile.generateChartPlotFilename(cuppaDir, tumorSampleId)));

        String sigsDir = getToolDirectory(configBuilder, pipelineSampleRootDir, sampleDataDir, SIGS_DIR_CFG, SIGS_DIR);

        if(sigsDir == null)
        {
            return null;
        }
        builder.sigsAllocationTsv(fileIfExists(SignatureAllocationFile.generateFilename(sigsDir, tumorSampleId)));

        String peachDir = getToolDirectory(configBuilder, pipelineSampleRootDir, sampleDataDir, PEACH_DIR_CFG, PEACH_DIR);
        if(peachDir == null)
        {
            return null;
        }
        builder.peachGenotypeTsv(fileIfExists(checkAddDirSeparator(peachDir) + tumorSampleId + ".peach.genotype.tsv"));

        builder.refSampleWGSMetricsFile(getMetricsFile(
                configBuilder, REF_SAMPLE_WGS_METRICS_FILE, refSampleId, pipelineSampleRootDir, METRICS_DIR));

        builder.refSampleFlagstatFile(getMetricsFile(
                configBuilder, REF_SAMPLE_FLAGSTAT_FILE, refSampleId, pipelineSampleRootDir, FLAGSTAT_DIR));

        return builder.build();

    }
}
