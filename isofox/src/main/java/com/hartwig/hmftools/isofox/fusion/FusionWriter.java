package com.hartwig.hmftools.isofox.fusion;

import static com.hartwig.hmftools.common.utils.FileWriterUtils.closeBufferedWriter;
import static com.hartwig.hmftools.common.utils.FileWriterUtils.createBufferedWriter;
import static com.hartwig.hmftools.common.utils.sv.StartEndIterator.SE_END;
import static com.hartwig.hmftools.common.utils.sv.StartEndIterator.SE_START;
import static com.hartwig.hmftools.isofox.IsofoxConfig.ISF_LOGGER;
import static com.hartwig.hmftools.isofox.fusion.FusionReadData.fusionId;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.hartwig.hmftools.isofox.IsofoxConfig;
import com.hartwig.hmftools.isofox.common.ReadRecord;

public class FusionWriter
{
    private final IsofoxConfig mConfig;
    private BufferedWriter mFusionWriter;
    private BufferedWriter mPassingFusionWriter;
    private BufferedWriter mFragmentWriter;
    private final ChimericReadCache mChimericReadCache;
    private final boolean mWriteReads;
    private final boolean mWriteFragments;

    private int mNextFusionId;

    public static final String RAW_FUSION_FILE_ID = "fusions.csv";
    public static final String PASS_FUSION_FILE_ID = "pass_fusions.csv";

    public FusionWriter(final IsofoxConfig config)
    {
        mConfig = config;
        mWriteReads = mConfig.Fusions.WriteChimericReads;
        mWriteFragments = mConfig.Fusions.WriteChimericFragments;

        mFusionWriter = null;
        mFragmentWriter = null;
        mChimericReadCache = new ChimericReadCache(config);
        mNextFusionId = 0;

        initialiseFusionWriters();
        initialiseFragmentWriter();
    }

    public synchronized int getNextFusionId() { return mNextFusionId++; }

    public void close()
    {
        closeBufferedWriter(mFusionWriter);
        closeBufferedWriter(mPassingFusionWriter);
        closeBufferedWriter(mFragmentWriter);
        mChimericReadCache.close();
    }

    private void initialiseFusionWriters()
    {
        if(mConfig.OutputDir == null)
            return;

        try
        {
            mFusionWriter = createBufferedWriter(mConfig.formOutputFile(RAW_FUSION_FILE_ID), false);
            mFusionWriter.write(FusionData.csvHeader(false));
            mFusionWriter.newLine();

            mPassingFusionWriter = createBufferedWriter(mConfig.formOutputFile(PASS_FUSION_FILE_ID), false);
            mPassingFusionWriter.write(FusionData.csvHeader(true));
            mPassingFusionWriter.newLine();
        }
        catch(IOException e)
        {
            ISF_LOGGER.error("failed to create fusions file: {}", e.toString());
        }
    }

    public synchronized void writeFusionData(
            final List<FusionData> fusions, final List<FusionData> passingFusions, final Map<String,List<FusionReadData>> fusionCandidates)
    {
        if(mConfig.OutputDir == null)
            return;

        try
        {
            for(FusionData fusionData : fusions)
            {
                mFusionWriter.write(fusionData.toCsv(false));
                mFusionWriter.newLine();
                // mFusionWriter.write(fusion.toCsv());
            }

            for(FusionData fusionData : passingFusions)
            {
                mPassingFusionWriter.write(fusionData.toCsv(true));
                mPassingFusionWriter.newLine();
            }

            if(mWriteReads || mWriteFragments)
            {
                for(List<FusionReadData> fusionCandidate : fusionCandidates.values())
                {
                    for(FusionReadData fusion : fusionCandidate)
                    {
                        for(Map.Entry<FusionFragmentType,List<FusionFragment>> entry : fusion.getFragments().entrySet())
                        {
                            for(FusionFragment fragment : entry.getValue())
                            {
                                if(mWriteFragments)
                                    writeFragmentData(fragment, fusionId(fusion.id()), entry.getKey());

                                if(mWriteReads)
                                    writeReadData(fragment.reads(), fusionId(fusion.id()));
                            }
                        }
                    }
                }
            }
        }
        catch(IOException e)
        {
            ISF_LOGGER.error("failed to write fusions file: {}", e.toString());
        }
    }

    public synchronized void writeReadData(final List<ReadRecord> reads, final String groupStatus)
    {
        if(mWriteReads)
            mChimericReadCache.writeReadData(reads, groupStatus);
    }

    public synchronized void writeUnfusedFragments(final Map<String,List<FusionFragment>> unfusedFragments)
    {
        if(!mWriteFragments)
            return;

        for(List<FusionFragment> fragments : unfusedFragments.values())
        {
            for(FusionFragment fragment : fragments)
            {
                if(!fragment.assignedFusions().isEmpty())
                    continue;

                writeFragmentData(fragment, "UNFUSED", fragment.type());
                writeReadData(fragment.reads(), "UNFUSED");
            }
        }
    }

    private void initialiseFragmentWriter()
    {
        if(!mWriteFragments)
            return;

        try
        {
            final String outputFileName = mConfig.formOutputFile("chimeric_frags.csv");

            mFragmentWriter = createBufferedWriter(outputFileName, false);
            mFragmentWriter.write("ReadId,ReadCount,FusionGroup,Type,SameGeneSet,ScCount,HasSupp");

            for(int se = SE_START; se <= SE_END; ++se)
            {
                final String prefix = se == SE_START ? "Start" : "End";
                mFragmentWriter.write(",Chr" + prefix);
                mFragmentWriter.write(",Orient" + prefix);
                mFragmentWriter.write(",JuncPos" + prefix);
                mFragmentWriter.write(",JuncOrient" + prefix);
                mFragmentWriter.write(",JuncType" + prefix);
                mFragmentWriter.write(",GeneSet" + prefix);
                mFragmentWriter.write(",Region" + prefix);
            }

            mFragmentWriter.newLine();
        }
        catch (IOException e)
        {
            ISF_LOGGER.error("failed to write chimeric fragment data: {}", e.toString());
            return;
        }
    }

    public void writeIncompleteGroupReads(final List<ReadGroup> incompleteGroups)
    {
        try
        {
            final String outputFileName = mConfig.formOutputFile("incomplete_chim_group_reads.csv");

            BufferedWriter writer = createBufferedWriter(outputFileName, false);
            writer.write("ReadId,ReadGroupSize,Chromosome,PosStart,PosEnd,Cigar,InsertSize");
            writer.write(",MateChr,MatePosStart,FirstInPair,ReadReversed,SuppData,GeneCollectionStart,GeneCollectionEnd");
            writer.newLine();

            for(ReadGroup readGroup : incompleteGroups)
            {
                for(ReadRecord read : readGroup.Reads)
                {
                    if(mConfig.Filters.excludeChromosome(read.mateChromosome()))
                        continue;

                    writer.write(String.format("%s,%d,%s,%d,%d,%s,%d",
                            read.Id, readGroup.size(), read.Chromosome, read.PosStart, read.PosEnd, read.Cigar.toString(),
                            read.fragmentInsertSize()));

                    writer.write(String.format(",%s,%d,%s,%s,%s,%d,%d",
                            read.mateChromosome(), read.mateStartPosition(), read.isFirstOfPair(), read.isReadReversed(),
                            read.getSuppAlignmentCsv(), read.getGeneCollectons()[SE_START], read.getGeneCollectons()[SE_END]));

                    writer.newLine();
                }
            }

            writer.close();
        }
        catch (IOException e)
        {
            ISF_LOGGER.error("failed to write incomplete chimeric fragment data: {}", e.toString());
            return;
        }
    }

    public synchronized void writeFragmentData(final FusionFragment fragment, final String fusionId, FusionFragmentType type)
    {
        if(!mWriteFragments)
            return;

        try
        {
            mFragmentWriter.write(String.format("%s,%d,%s,%s,%s,%d,%s",
                    fragment.readId(), fragment.reads().size(), fusionId, type,
                    fragment.isSingleGeneCollection(), fragment.reads().stream().filter(x -> x.containsSoftClipping()).count(),
                    fragment.hasSuppAlignment()));

            for(int se = SE_START; se <= SE_END; ++se)
            {
                mFragmentWriter.write(String.format(",%s,%d,%d,%d,%s,%d,%s",
                        fragment.chromosomes()[se], fragment.orientations()[se],
                        fragment.junctionPositions()[se], fragment.junctionOrientations()[se], fragment.junctionTypes()[se],
                        fragment.geneCollections()[se], fragment.regionMatchTypes()[se]));
            }

            mFragmentWriter.newLine();
        }
        catch (IOException e)
        {
            ISF_LOGGER.error("failed to write chimeric fragment data: {}", e.toString());
            return;
        }

    }

}
