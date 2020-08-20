package com.hartwig.hmftools.cup.sample;

import static com.hartwig.hmftools.common.sigs.Percentiles.getPercentile;
import static com.hartwig.hmftools.cup.common.CategoryType.SAMPLE_CLASS;
import static com.hartwig.hmftools.cup.common.CategoryType.SAMPLE_TRAIT;
import static com.hartwig.hmftools.cup.common.ResultType.LIKELIHOOD;
import static com.hartwig.hmftools.cup.common.ResultType.PERCENTILE;
import static com.hartwig.hmftools.cup.common.ResultType.PREVALENCE;
import static com.hartwig.hmftools.cup.sample.SampleTraitType.GENDER;
import static com.hartwig.hmftools.cup.sample.SampleTraitsDataLoader.loadTraitsFromCohortFile;
import static com.hartwig.hmftools.cup.sample.SampleTraitsDataLoader.loadTraitsFromDatabase;
import static com.hartwig.hmftools.cup.sample.SampleTraitsDataLoader.loadRefPercentileData;
import static com.hartwig.hmftools.cup.sample.SampleTraitsDataLoader.loadRefRateData;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.purple.gender.Gender;
import com.hartwig.hmftools.cup.SampleAnalyserConfig;
import com.hartwig.hmftools.cup.common.SampleData;
import com.hartwig.hmftools.cup.common.SampleDataCache;
import com.hartwig.hmftools.cup.common.SampleResult;

import org.apache.commons.compress.utils.Lists;

public class SampleTraits
{
    private final SampleAnalyserConfig mConfig;
    private final SampleDataCache mSampleDataCache;

    private final Map<String,SampleTraitsData> mSampleTraitsData;

    private final Map<SampleTraitType,Map<String,double[]>> mRefTraitPercentiles;
    private final Map<SampleTraitType,Map<String,Double>> mRefTraitRates;

    private boolean mIsValid;

    public SampleTraits(final SampleAnalyserConfig config, final SampleDataCache sampleDataCache)
    {
        mConfig = config;
        mSampleDataCache = sampleDataCache;

        mSampleTraitsData = Maps.newHashMap();
        mRefTraitPercentiles = Maps.newHashMap();
        mRefTraitRates = Maps.newHashMap();
        mIsValid = true;

        mIsValid &= loadRefPercentileData(mConfig.RefTraitPercFile, mRefTraitPercentiles);
        mIsValid &= loadRefRateData(mConfig.RefTraitRateFile, mRefTraitRates);
        mIsValid &= loadSampleTraitsData();
    }

    public boolean isValid() { return mIsValid; }

    private boolean loadSampleTraitsData()
    {
        if(!mConfig.SampleTraitsFile.isEmpty())
        {
            if(!loadTraitsFromCohortFile(mConfig.SampleTraitsFile, mSampleTraitsData))
                return false;
        }
        else if(mConfig.DbAccess != null)
        {
            if(!loadTraitsFromDatabase(mConfig.DbAccess, mSampleDataCache.SampleIds, mSampleTraitsData))
                return false;
        }

        for(SampleData sample : mSampleDataCache.SampleDataList)
        {
            final SampleTraitsData sampleTraits = mSampleTraitsData.get(sample.Id);

            if(sampleTraits != null)
                sample.setGender(sampleTraits.GenderType);
        }

        return true;
    }

    public List<SampleResult> processSample(final SampleData sample)
    {
        final List<SampleResult> results = Lists.newArrayList();

        final SampleTraitsData sampleTraits = mSampleTraitsData.get(sample.Id);

        if(sampleTraits == null)
        {
            mIsValid = false;
            return results;
        }

        if(mConfig.runCategory(SAMPLE_CLASS))
        {
            for(Map.Entry<SampleTraitType, Map<String, Double>> entry : mRefTraitRates.entrySet())
            {
                final SampleTraitType traitType = entry.getKey();
                Map<String, Double> cancerRates = entry.getValue();

                // reverse the prevalence for MALE since gender is currently IsFemale
                if(traitType == GENDER && sampleTraits.GenderType != Gender.FEMALE)
                {
                    Map<String, Double> oppGenderRates = Maps.newHashMap();
                    cancerRates.entrySet().forEach(x -> oppGenderRates.put(x.getKey(), 1 - x.getValue()));
                    cancerRates = oppGenderRates;
                }

                SampleResult result = new SampleResult(
                        sample.Id, SAMPLE_CLASS, PREVALENCE, traitType.toString(), sampleTraits.getStrValue(traitType), cancerRates);

                results.add(result);
            }
        }

        if(mConfig.runCategory(SAMPLE_TRAIT))
        {
            for(Map.Entry<SampleTraitType, Map<String, double[]>> entry : mRefTraitPercentiles.entrySet())
            {
                final SampleTraitType traitType = entry.getKey();
                double traitValue = sampleTraits.getDoubleValue(traitType);

                final Map<String, Double> cancerTypeValues = Maps.newHashMap();

                for(Map.Entry<String, double[]> cancerPercentiles : entry.getValue().entrySet())
                {
                    final String cancerType = cancerPercentiles.getKey();
                    double percentile = getPercentile(cancerPercentiles.getValue(), traitValue, true);
                    cancerTypeValues.put(cancerType, percentile);
                }

                SampleResult result = new SampleResult(
                        sample.Id, SAMPLE_TRAIT, PERCENTILE, traitType.toString(), traitValue, cancerTypeValues);

                results.add(result);
            }
        }

        return results;
    }


}
