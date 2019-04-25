package com.hartwig.hmftools.svanalysis.annotators;

import static java.lang.Math.max;

import static com.hartwig.hmftools.common.io.FileWriterUtils.closeBufferedWriter;
import static com.hartwig.hmftools.common.io.FileWriterUtils.createBufferedWriter;
import static com.hartwig.hmftools.svanalysis.analysis.SvUtilities.CHROMOSOME_ARM_P;
import static com.hartwig.hmftools.svanalysis.analysis.SvUtilities.CHROMOSOME_ARM_Q;
import static com.hartwig.hmftools.svanalysis.types.SvBreakend.DIRECTION_CENTROMERE;
import static com.hartwig.hmftools.svanalysis.types.SvBreakend.DIRECTION_TELOMERE;
import static com.hartwig.hmftools.svanalysis.types.SvCluster.RESOLVED_TYPE_LOW_QUALITY;
import static com.hartwig.hmftools.svanalysis.types.SvCluster.RESOLVED_TYPE_SIMPLE_SV;
import static com.hartwig.hmftools.svanalysis.types.SvCluster.isSpecificCluster;
import static com.hartwig.hmftools.svanalysis.types.SvVarData.SVI_END;
import static com.hartwig.hmftools.svanalysis.types.SvVarData.SVI_START;
import static com.hartwig.hmftools.svanalysis.types.SvVarData.isSpecificSV;
import static com.hartwig.hmftools.svanalysis.types.SvVarData.isStart;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.variant.structural.annotation.TranscriptExonData;
import com.hartwig.hmftools.svanalysis.types.SvBreakend;
import com.hartwig.hmftools.svanalysis.types.SvChain;
import com.hartwig.hmftools.svanalysis.types.SvCluster;
import com.hartwig.hmftools.svanalysis.types.SvLinkedPair;
import com.hartwig.hmftools.svanalysis.types.SvVarData;
import com.hartwig.hmftools.svanalysis.types.VisGeneData;
import com.hartwig.hmftools.svannotation.SvGeneTranscriptCollection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VisualiserWriter
{
    private BufferedWriter mSvsFileWriter;
    private BufferedWriter mSegmentsFileWriter;
    private BufferedWriter mGenesFileWriter;

    // references only
    SvGeneTranscriptCollection mGeneTranscriptCollection;

    private List<VisGeneData> mGeneData;

    private boolean mEnabled;
    private final String mOutputDir;
    private String mSampleId;

    private static final Logger LOGGER = LogManager.getLogger(VisualiserWriter.class);


    public VisualiserWriter(final String outputDir, boolean enabled)
    {
        mEnabled = enabled;
        mOutputDir = outputDir;
        mSampleId = "";

        mGeneTranscriptCollection = null;

        mSvsFileWriter = null;
        mSegmentsFileWriter = null;
        mGenesFileWriter = null;
        mGeneData = Lists.newArrayList();
    }

    public void setGeneDataCollection(SvGeneTranscriptCollection geneTranscriptCollection)
    {
        mGeneTranscriptCollection = geneTranscriptCollection;
    }

    public void setSampleId(final String sampleId)
    {
        mSampleId = sampleId;
        mGeneData.clear();
    }

    public void writeOutput(final List<SvCluster> clusters, final List<SvVarData> variants)
    {
        if(!mEnabled)
            return;

        writeVisualSvData(variants);
        writeVisualSegmentData(clusters);
        writeGeneExonData();
    }

    private void writeVisualSvData(final List<SvVarData> variants)
    {
        try
        {
            if (mSvsFileWriter == null)
            {
                String outputFileName = mOutputDir;

                outputFileName += "SVA_VIS_SVS.csv";

                mSvsFileWriter = createBufferedWriter(outputFileName, false);

                // definitional fields
                mSvsFileWriter.write("SampleId,ClusterId,ChainId,SvId,Type,ResolvedType");
                mSvsFileWriter.write(",ChrStart,PosStart,OrientStart,InfoStart,ChrEnd,PosEnd,OrientEnd,InfoEnd,TraverseCount");

                mSvsFileWriter.newLine();
            }

            BufferedWriter writer = mSvsFileWriter;

            for(final SvVarData var : variants)
            {
                final List<SvChain> chains = var.getCluster().findChains(var);

                // repeat an SV for every time it appears in a chain
                int chainCount = chains.isEmpty() ? 1 : chains.size();
                int unchainedChainId = chains.isEmpty() ? var.getCluster().getChainId(var) : -1;

                // int repeatCount = !chains.isEmpty() ? max(var.getReplicatedCount(), 1) : 1;
                int repeatCount = max((int)var.getRoundedCNChange(),1);

                for(int i = 0; i < chainCount; ++i)
                {
                    int chainId = chains.isEmpty() ? unchainedChainId : chains.get(i).id();

                    writer.write(
                            String.format("%s,%d,%d,%s,%s,%s",
                                    mSampleId, var.getCluster().id(), chainId, var.id(),
                                    var.type(), var.getCluster().getResolvedType()));

                    for (int be = SVI_START; be <= SVI_END; ++be)
                    {
                        boolean isStart = isStart(be);

                        if (!isStart && var.isNullBreakend())
                        {
                            writer.write(",-1,0,0,NULL");
                            continue;
                        }

                        final SvBreakend breakend = var.getBreakend(isStart);

                        writer.write(
                                String.format(",%s,%d,%d,%s",
                                        breakend.chromosome(), breakend.position(), breakend.orientation(),
                                        breakend.getSV().getFoldbackLink(isStart).isEmpty() ? "NORMAL" : "FOLDBACK"));
                    }

                    writer.write(String.format(",%d", repeatCount));

                    writer.newLine();
                }
            }
        }
        catch (final IOException e)
        {
            LOGGER.error("error writing to visual SVs file: {}", e.toString());
        }
    }

    private void writeVisualSegmentData(final List<SvCluster> clusters)
    {
        try
        {
            if (mSegmentsFileWriter == null)
            {
                String outputFileName = mOutputDir;

                outputFileName += "SVA_VIS_SEGMENTS.csv";

                mSegmentsFileWriter = createBufferedWriter(outputFileName, false);
                mSegmentsFileWriter.write("SampleId,ClusterId,ChainId,Chr,PosStart,PosEnd,TraverseCount,Reason");
                mSegmentsFileWriter.newLine();
            }

            BufferedWriter writer = mSegmentsFileWriter;

            for(final SvCluster cluster : clusters)
            {
                if(cluster.isResolved()
                        && (cluster.getResolvedType() == RESOLVED_TYPE_SIMPLE_SV || cluster.getResolvedType() == RESOLVED_TYPE_LOW_QUALITY))
                {
                    continue;
                }

                // isSpecificCluster(cluster);

                // for any linked pair which is repeated in a separate chain, skip writing it for subsequent chains
                List<SvLinkedPair> uniquePairs = Lists.newArrayList();

                for (final SvChain chain : cluster.getChains())
                {
                    // log the start of the chain
                    SvBreakend breakend = chain.getOpenBreakend(true);
                    boolean startsOnEnd = chain.getFirstSV().equals(chain.getLastSV(), true);

                    if(breakend != null)
                    {
                        writer.write(String.format("%s,%d,%d,%s,%s,%s,%d",
                                mSampleId, cluster.id(), chain.id(), breakend.chromosome(), getPositionValue(breakend, true),
                                getPositionValue(breakend, false), startsOnEnd ? 2 : 1));

                        writer.newLine();
                    }

                    for (final SvLinkedPair pair : chain.getLinkedPairs())
                    {
                        boolean isRepeat = false;

                        // only log each chain link once, and log how many times the link has been used
                        for (final SvLinkedPair existingPair : uniquePairs)
                        {
                            if(pair.matches(existingPair))
                            {
                                isRepeat = true;
                                break;
                            }
                        }

                        if(isRepeat)
                            continue;

                        uniquePairs.add(pair);

                        writer.write(String.format("%s,%d,%d",
                                mSampleId, cluster.id(), chain.id()));

                        int pairRepeatCount = 0;

                        for (final SvChain otherChain : cluster.getChains())
                        {
                            pairRepeatCount += otherChain.getLinkedPairs().stream().filter(x -> x.matches(pair)).count();
                        }

                        final SvBreakend beStart = pair.getBreakend(true);
                        final SvBreakend beEnd= pair.getBreakend(false);

                        writer.write(String.format(",%s,%d,%d,%d,%s",
                                beStart.chromosome(), beStart.position(), beEnd.position(), pairRepeatCount, pair.getLinkReason()));

                        writer.newLine();
                    }

                    // log the end of the chain out to centromere or telomere
                    breakend = chain.getOpenBreakend(false);

                    if(breakend != null && !startsOnEnd)
                    {
                        writer.write(String.format("%s,%d,%d,%s,%s,%s,%d",
                                mSampleId, cluster.id(), chain.id(), breakend.chromosome(), getPositionValue(breakend, true),
                                getPositionValue(breakend, false), 1));

                        writer.newLine();
                    }
                }

                // finally write out all unchained SVs
                for(final SvVarData var : cluster.getUnlinkedSVs())
                {
                    if(var.isReplicatedSv())
                        continue;

                    int chainId = cluster.getChainId(var);

                    for(int be = SVI_START; be <= SVI_END; ++be)
                    {
                        final SvBreakend breakend = var.getBreakend(isStart(be));

                        if(breakend == null)
                            continue;

                        writer.write(String.format("%s,%d,%d",
                                mSampleId, cluster.id(), chainId));

                        writer.write(String.format(",%s,%s,%s,%d",
                                breakend.chromosome(), getPositionValue(breakend, true),
                                getPositionValue(breakend, false), 1));

                        writer.newLine();
                    }
                }
            }
        }
        catch (final IOException e)
        {
            LOGGER.error("error writing to visual segments file: {}", e.toString());
        }
    }

    private static final String getPositionValue(final SvBreakend breakend, boolean isChainEnd)
    {
        if(breakend.orientation() == 1 && breakend.arm().equals(CHROMOSOME_ARM_P))
        {
            return isChainEnd ? DIRECTION_TELOMERE : Long.toString(breakend.position());
        }
        else if(breakend.orientation() == -1 && breakend.arm().equals(CHROMOSOME_ARM_P))
        {
            return isChainEnd ? Long.toString(breakend.position()) : DIRECTION_CENTROMERE;
        }
        else if(breakend.orientation() == -1 && breakend.arm().equals(CHROMOSOME_ARM_Q))
        {
            return isChainEnd ? Long.toString(breakend.position()) : DIRECTION_TELOMERE;
        }
        else
        {
            return isChainEnd ? DIRECTION_CENTROMERE : Long.toString(breakend.position());
        }
    }


    public void addGeneExonData(int clusterId, final String geneId, final String geneName, final String transcriptId,
            final String chromosome, final String annotationType)
    {
        mGeneData.add(new VisGeneData(clusterId, geneId, geneName, transcriptId, chromosome, annotationType));
    }

    public void writeGeneExonData()
    {
        if(!mEnabled)
            return;

        try
        {
            if (mGenesFileWriter == null)
            {
                String outputFileName = mOutputDir;

                outputFileName += "SVA_VIS_GENE_EXONS.csv";

                mGenesFileWriter = createBufferedWriter(outputFileName, false);
                mGenesFileWriter.write("SampleId,ClusterId,Gene,Transcript,Chromosome,AnnotationType,ExonRank,ExonStart,ExonEnd");
                mGenesFileWriter.newLine();

            }

            // first remove duplicates from amongst the genes
            List<String> loggedGenes = Lists.newArrayList();

            for(final VisGeneData geneData : mGeneData)
            {
                if(loggedGenes.contains(geneData.GeneId))
                    continue;

                loggedGenes.add(geneData.GeneId);

                // log relevant exons
                final List<TranscriptExonData> exonDataLst = mGeneTranscriptCollection.getTranscriptExons(geneData.GeneId, geneData.TranscriptId);

                for (final TranscriptExonData exonData : exonDataLst)
                {
                    mGenesFileWriter.write(String.format("%s,%d,%s,%s,%s,%s",
                            mSampleId, geneData.ClusterId, geneData.GeneName, exonData.TransName, geneData.Chromosome, geneData.AnnotationType));

                    mGenesFileWriter.write(String.format(",%d,%d,%d", exonData.ExonRank, exonData.ExonStart, exonData.ExonEnd));

                    mGenesFileWriter.newLine();
                }
            }
        }
        catch (final IOException e)
        {
            LOGGER.error("error writing to visual gene-exons file: {}", e.toString());
        }
    }

    public void close()
    {
        closeBufferedWriter(mSvsFileWriter);
        closeBufferedWriter(mSegmentsFileWriter);
        closeBufferedWriter(mGenesFileWriter);

    }

}
