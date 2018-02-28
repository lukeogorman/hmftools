package com.hartwig.hmftools.svannotation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.svannotation.analysis.SvClusterData;
import com.hartwig.hmftools.svannotation.analysis.SvPON;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SvPONAnnotator {

    private List<SvPON> mPonList;
    private static final int REGION_DISTANCE = 500;

    private static final Logger LOGGER = LogManager.getLogger(SvPONAnnotator.class);

    private Map<String, Integer> mChrIndexMap;

    public SvPONAnnotator()
    {
        mPonList = Lists.newArrayList();
        mChrIndexMap = new HashMap<>();
    }

    public void loadPonFile(final String ponFilename)
    {
        if(ponFilename.isEmpty())
            return;

        try {

             BufferedReader fileReader = new BufferedReader(new FileReader(ponFilename));

            String line;
            while ((line = fileReader.readLine()) != null) {

                if(line.contains("ChrStart"))
                    continue;

                // parse CSV data
                String[] items = line.split(",");

                if(items.length != 8)
                    continue;

                SvPON svPon = new SvPON(items[0], items[1], Long.parseLong(items[2]), Long.parseLong(items[3]), Byte.parseByte(items[4]), Byte.parseByte(items[5]),
                        items[6], Integer.parseInt(items[7]));

                mPonList.add(svPon);

//                LOGGER.debug("loaded PON: chr({}-{}) pos({}-{}) orient({}-{}) with count({})",
//                        svPon.chrStart(), svPon.chrStart(), svPon.posStart(), svPon.posEnd(), svPon.orientStart(), svPon.orientEnd(), svPon.count());
            }

            LOGGER.debug("loaded {} PON records", mPonList.size());

            setIndexes();
        }
        catch(IOException exception)
        {
            LOGGER.error("Failed to read PON CSV file({})", ponFilename);
        }
    }

    private void setIndexes()
    {
        // assign each SV PON to a map of type to chromosome and index into the array
        String currentChr = "";
        for(int index = 0; index < mPonList.size(); ++index)
        {
            final SvPON svPon = mPonList.get(index);

            if(!svPon.chrStart().equals(currentChr))
            {
                currentChr = svPon.chrStart();
                mChrIndexMap.put(currentChr, index);

                // LOGGER.debug("newChr({}) at index({})", currentChr, index);
            }
        }
    }

    public void setPonOccurenceCount(SvClusterData svData)
    {
        if(mPonList.isEmpty())
            return;

        // use CRMS to find start index
        if(!mChrIndexMap.containsKey(svData.chromosome(true)))
            return;

        int index = mChrIndexMap.get(svData.chromosome(true));

        for(; index < mPonList.size(); ++index)
        {
            final SvPON svPon = mPonList.get(index);

            if(!svPon.chrStart().equals(svData.chromosome(true)))
            {
                return;
            }

            if(isMatch(svData, svPon,0))
            {
                LOGGER.debug("var({}) found in PON with count({})", svData.posId(), svPon.count());
                svData.setPonCount(svPon.count());
                return;
            }
            else if(svData.getPonRegionCount() == 0 && isMatch(svData, svPon, REGION_DISTANCE))
            {
                LOGGER.debug("var({}) found in PON region({}->{}) with count({})",
                        svData.posId(), svPon.posStart(), svPon.posEnd(), svPon.count());

                svData.setPonRegionCount(svPon.count());
            }
        }
    }

    private boolean isMatch(final SvClusterData svData, final SvPON svPon, final int permittedDiff)
    {
        if(!svPon.chrStart().equals(svData.chromosome(true))
        || !svPon.chrEnd().equals(svData.chromosome(false))
        || svPon.orientStart() != svData.orientation(true)
        || svPon.orientEnd() != svData.orientation(false)
        || !svPon.type().equals(svData.type().toString()))
        {
            return false;
        }

        return Math.abs(svPon.posStart() - svData.position(true)) <= permittedDiff
            && Math.abs(svPon.posEnd() - svData.position(false)) <= permittedDiff;
    }
}
