package com.hartwig.hmftools.bamtools.compare;

import static java.lang.Math.min;
import static java.lang.String.format;

import static com.hartwig.hmftools.bamtools.common.CommonUtils.BT_LOGGER;
import static com.hartwig.hmftools.bamtools.common.PartitionTask.partitionChromosome;
import static com.hartwig.hmftools.common.utils.ConfigUtils.setLogLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.bamtools.common.PartitionTask;
import com.hartwig.hmftools.common.genome.chromosome.HumanChromosome;
import com.hartwig.hmftools.common.utils.sv.ChrBaseRegion;
import com.hartwig.hmftools.common.utils.version.VersionInfo;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jetbrains.annotations.NotNull;

public class BamCompare
{
    private final CompareConfig mConfig;

    public BamCompare(final CommandLine cmd)
    {
        mConfig = new CompareConfig(cmd);
    }

    public void run()
    {
        BT_LOGGER.info("starting bam comparison", mConfig.OutputFile);

        long startTimeMs = System.currentTimeMillis();

        List<ChrBaseRegion> allRegions = Lists.newArrayList();

        for(HumanChromosome chromosome : HumanChromosome.values())
        {
            String chromosomeStr = mConfig.RefGenVersion.versionedChromosome(chromosome.toString());

            if(!mConfig.SpecificChromosomes.isEmpty() && !mConfig.SpecificChromosomes.contains(chromosomeStr))
                continue;

            allRegions.addAll(partitionChromosome(chromosomeStr, mConfig.RefGenVersion, mConfig.SpecificRegions, mConfig.PartitionSize));
        }

        ReadWriter readWriter = new ReadWriter(mConfig);

        if(!readWriter.initialised())
            System.exit(1);

        BT_LOGGER.info("splitting {} regions across {} threads", allRegions.size(), mConfig.Threads);

        Queue<PartitionTask> partitions = new ConcurrentLinkedQueue<>();

        int taskId = 0;
        for(int i = 0; i < allRegions.size(); ++i)
        {
            partitions.add(new PartitionTask(allRegions.get(i), taskId++));
        }

        List<Thread> workers = new ArrayList<>();

        for(int i = 0; i < min(allRegions.size(), mConfig.Threads); ++i)
        {
            workers.add(new PartitionThread(mConfig, partitions, readWriter));
        }

        for(Thread worker : workers)
        {
            try
            {
                worker.join();
            }
            catch(InterruptedException e)
            {
                BT_LOGGER.error("task execution error: {}", e.toString());
                e.printStackTrace();
            }
        }

        readWriter.close();

        long timeTakenMs = System.currentTimeMillis() - startTimeMs;
        double timeTakeMins = timeTakenMs / 60000.0;

        BT_LOGGER.info("BamCompare complete, mins({})", format("%.3f", timeTakeMins));
    }

    public static void main(@NotNull final String[] args)
    {
        final VersionInfo version = new VersionInfo("bam-tools.version");
        BT_LOGGER.info("BamTools version: {}", version.version());

        final Options options = CompareConfig.createCmdLineOptions();

        try
        {
            final CommandLine cmd = createCommandLine(args, options);

            setLogLevel(cmd);

            BamCompare bamCompare = new BamCompare(cmd);
            bamCompare.run();
        }
        catch(ParseException e)
        {
            BT_LOGGER.warn(e);
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("BamCompare", options);
            System.exit(1);
        }
    }

    @NotNull
    private static CommandLine createCommandLine(@NotNull final String[] args, @NotNull final Options options) throws ParseException
    {
        final CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }
}