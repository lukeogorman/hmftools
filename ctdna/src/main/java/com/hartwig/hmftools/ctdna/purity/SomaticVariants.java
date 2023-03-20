package com.hartwig.hmftools.ctdna.purity;

import static java.lang.String.format;

import static com.hartwig.hmftools.common.variant.PurpleVcfTags.SUBCLONAL_LIKELIHOOD_FLAG;
import static com.hartwig.hmftools.common.variant.SageVcfTags.LIST_SEPARATOR;
import static com.hartwig.hmftools.common.variant.SageVcfTags.RC_REALIGNED;
import static com.hartwig.hmftools.common.variant.SageVcfTags.READ_CONTEXT_QUALITY;
import static com.hartwig.hmftools.common.variant.SomaticVariantFactory.MAPPABILITY_TAG;
import static com.hartwig.hmftools.ctdna.common.CommonUtils.CT_LOGGER;
import static com.hartwig.hmftools.ctdna.common.CommonUtils.medianIntegerValue;
import static com.hartwig.hmftools.ctdna.purity.PurityConstants.MAX_REPEAT_COUNT;
import static com.hartwig.hmftools.ctdna.purity.PurityConstants.MIN_QUAL_PER_AD;
import static com.hartwig.hmftools.ctdna.purity.PurityConstants.MAX_SUBCLONAL_LIKELIHOOD;
import static com.hartwig.hmftools.ctdna.purity.SomaticVariantResult.INVALID_RESULT;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.purple.PurityContext;
import com.hartwig.hmftools.common.purple.PurityContextFile;
import com.hartwig.hmftools.common.variant.VariantContextDecorator;
import com.hartwig.hmftools.common.variant.VariantTier;
import com.hartwig.hmftools.common.variant.VariantType;

import htsjdk.tribble.AbstractFeatureReader;
import htsjdk.tribble.readers.LineIterator;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFCodec;
import htsjdk.variant.vcf.VCFHeader;

public class SomaticVariants
{
    private final PurityConfig mConfig;
    private final ResultsWriter mResultsWriter;

    private final List<String> mProcessSamples;
    private final List<SomaticVariant> mVariants;

    public SomaticVariants(final PurityConfig config, final ResultsWriter resultsWriter)
    {
        mConfig = config;
        mResultsWriter = resultsWriter;

        mProcessSamples = Lists.newArrayList();
        mVariants = Lists.newArrayList();
    }

    public boolean processVcf(final String somaticVcf)
    {
        List<String> requiredSampleIds = Lists.newArrayList();
        if(!mProcessSamples.contains(mConfig.TumorId))
            requiredSampleIds.add(mConfig.TumorId);

        mConfig.CtDnaSamples.stream().filter(x -> !mProcessSamples.contains(x)).forEach(x -> requiredSampleIds.add(x));

        if(requiredSampleIds.isEmpty())
            return true;

        CT_LOGGER.info("process somatic variant VCF: {}", somaticVcf);

        List<String> targetSampleIds = Lists.newArrayList();

        AbstractFeatureReader<VariantContext, LineIterator> reader = AbstractFeatureReader.getFeatureReader(
                somaticVcf, new VCFCodec(), false);

        VCFHeader vcfHeader = (VCFHeader)reader.getHeader();

        for(String sampleName : vcfHeader.getGenotypeSamples())
        {
            if(requiredSampleIds.contains(sampleName))
                targetSampleIds.add(sampleName);
        }

        try
        {
            int variantCount = 0;

            for(VariantContext variantContext : reader.iterator())
            {
                ++variantCount;

                if(variantContext.isFiltered())
                    continue;

                try
                {
                    processVariant(targetSampleIds, variantContext);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                    CT_LOGGER.error("error processing VCF({}): {}", somaticVcf, e.toString());
                    return false;
                }

                if(variantCount > 0 && (variantCount % 100000) == 0)
                {
                    CT_LOGGER.info("processed {} variants", variantCount);
                }
            }

            CT_LOGGER.info("process {} somatic variants from VCF({})", variantCount, somaticVcf);
        }
        catch(IOException e)
        {
            CT_LOGGER.error("error reading vcf files: {}", e.toString());
            return false;
        }

        return true;
    }

    private void processVariant(final List<String> targetSampleIds, final VariantContext variantContext)
    {
        VariantContextDecorator variant = new VariantContextDecorator(variantContext);

        double subclonalLikelihood = variant.context().getAttributeAsDouble(SUBCLONAL_LIKELIHOOD_FLAG, 0);

        if(filterVariant(variant, subclonalLikelihood))
            return;

        SomaticVariant somaticVariant = null;

        for(Genotype genotype : variantContext.getGenotypes())
        {
            if(!targetSampleIds.contains(genotype.getSampleName()))
                continue;

            if(somaticVariant == null)
            {
                somaticVariant = new SomaticVariant(variant.chromosome(), variant.position(), variant.ref(), variant.alt(), variant.tier());
                mVariants.add(somaticVariant);
            }

            if(genotype == null || genotype.getExtendedAttributes().isEmpty())
                continue;

            int depth = genotype.getDP();
            int alleleCount = genotype.getAD()[1];
            double qualPerAlleleCount = 0;

            if(alleleCount > 0)
            {
                final String[] qualCounts = genotype.getExtendedAttribute(READ_CONTEXT_QUALITY, 0).toString()
                        .split(LIST_SEPARATOR, -1);

                int qualTotal = 0;
                for(int i = 0; i <= RC_REALIGNED; ++i)
                {
                    qualTotal += Integer.parseInt(qualCounts[i]);
                }

                qualPerAlleleCount = qualTotal / (double) alleleCount;
            }

            somaticVariant.Samples.add(new GenotypeData(genotype.getSampleName(), alleleCount, depth, qualPerAlleleCount));
        }
    }

    public SomaticVariantResult processSample(final String sampleId, final PurityContext purityContext)
    {
        // only include variants which satisfy the min avg qual check in the ctDNA sample
        SomaticVariantCounts tumorCounts = new SomaticVariantCounts();
        SomaticVariantCounts sampleCounts = new SomaticVariantCounts();

        int variantCount = 0;

        for(SomaticVariant variant : mVariants)
        {
            GenotypeData sampleData = variant.findGenotypeData(sampleId);

            if(sampleData == null)
                continue;

            if(sampleData.QualPerAlleleCount < MIN_QUAL_PER_AD && sampleData.AlleleCount > 0)
                continue;

            GenotypeData tumorData = variant.findGenotypeData(mConfig.TumorId);

            if(tumorData == null)
                continue;

            ++variantCount;

            sampleCounts.QualPerAdTotal += sampleData.QualPerAlleleCount;
            sampleCounts.VariantDepths.add(sampleData.Depth);
            sampleCounts.AlleleFragmentTotal += sampleData.AlleleCount;

            tumorCounts.QualPerAdTotal += tumorData.QualPerAlleleCount;
            tumorCounts.VariantDepths.add(tumorData.Depth);
            tumorCounts.AlleleFragmentTotal += tumorData.AlleleCount;

            //mResultsWriter.writeVariant(
            //        mConfig.PatientId, sampleId, variant, subclonalLikelihood, alleleCount, depth, qualPerAlleleCount);
        }

        if(variantCount == 0)
            return INVALID_RESULT;

        double sampleDepthTotal = sampleCounts.depthTotal();
        if(sampleDepthTotal == 0)
            return INVALID_RESULT;

        double tumorDepthTotal = tumorCounts.depthTotal();
        if(tumorDepthTotal == 0)
            return INVALID_RESULT;

        double tumorPurity = purityContext.bestFit().purity();
        double tumorPloidy = purityContext.bestFit().ploidy();

        double tumorVaf = tumorCounts.AlleleFragmentTotal / tumorDepthTotal;
        double adjustedTumorVaf = tumorVaf * (tumorPloidy * tumorPurity + 2 * (1 - tumorPurity)) / tumorPurity / tumorPloidy;

        // ctDNA_TF = 2 * cfDNA_VAF / [ PLOIDY * ADJ_PRIMARY_VAF + cfDNA_VAF * ( 2 - PLOIDY)]
        // ADJ_PRIMARY_VAF= PRIMARY_VAF * [ PURITY*PLOIDY - 2*(1-PURITY)]/PURITY/PLOIDY

        double sampleVaf = sampleCounts.AlleleFragmentTotal / sampleDepthTotal;

        double samplePurity = 2 * sampleVaf / (tumorPloidy * adjustedTumorVaf + sampleVaf * (2 - tumorPloidy));

        double qualPerAllele = sampleCounts.QualPerAdTotal / sampleCounts.AlleleFragmentTotal;

        return new SomaticVariantResult(
                true, variantCount, sampleCounts.AlleleFragmentTotal, qualPerAllele, sampleCounts.medianDepth(),
                tumorVaf, adjustedTumorVaf, sampleVaf, samplePurity);
    }

    private class SomaticVariantCounts
    {
        public int AlleleFragmentTotal;
        public double QualPerAdTotal;

        public final List<Integer> VariantDepths;

        public SomaticVariantCounts()
        {
            VariantDepths = Lists.newArrayList();
            AlleleFragmentTotal = 0;
            QualPerAdTotal = 0;
        }

        public int depthTotal()
        {
            return VariantDepths.stream().mapToInt(x -> x).sum();
        }

        public double medianDepth()
        {
            return medianIntegerValue(VariantDepths);
        }

        public String toString()
        {
            return format("AF(%d) avgQualTotal(%.1f) depthCounts(%d)", AlleleFragmentTotal, QualPerAdTotal, VariantDepths.size());
        }
    }

    private boolean filterVariant(final VariantContextDecorator variant, double subclonalLikelihood)
    {
        if(variant.context().isFiltered())
            return true;

        if(variant.type() != VariantType.SNP)
            return true;

        if(variant.context().hasAttribute(MAPPABILITY_TAG) && variant.mappability() < 1)
            return true;

        if(variant.repeatCount() > MAX_REPEAT_COUNT)
            return true;

        if(variant.tier() == VariantTier.LOW_CONFIDENCE)
            return true;

        if(subclonalLikelihood > MAX_SUBCLONAL_LIKELIHOOD)
            return true;

        return false;
    }

    private class GenotypeData
    {
        public final String SampleName;
        public final int AlleleCount;
        public final int Depth;
        public final double QualPerAlleleCount;

        public GenotypeData(final String sampleName, final int alleleCount, final int depth, final double qualPerAlleleCount)
        {
            SampleName = sampleName;
            AlleleCount = alleleCount;
            Depth = depth;
            QualPerAlleleCount = qualPerAlleleCount;
        }
    }

    private class SomaticVariant
    {
        public final String Chromosome;
        public final int Position;
        public final String Ref;
        public final String Alt;
        public final VariantTier Tier;

        public final List<GenotypeData> Samples;

        public SomaticVariant(
                final String chromosome, final int position, final String ref, final String alt, final VariantTier tier)
        {
            Chromosome = chromosome;
            Position = position;
            Ref = ref;
            Alt = alt;
            Tier = tier;
            Samples = Lists.newArrayList();
        }

        public GenotypeData findGenotypeData(final String sampleId)
        {
            return Samples.stream().filter(x -> x.SampleName.equals(sampleId)).findFirst().orElse(null);
        }
    }

    /*
        public SomaticVariantResult processPatientVcf(final String sampleId, final String somaticVcf)
    {
        CT_LOGGER.info("sampleId({}) reading somatic VCF: {}", sampleId, somaticVcf);

        AbstractFeatureReader<VariantContext, LineIterator> reader = AbstractFeatureReader.getFeatureReader(
                somaticVcf, new VCFCodec(), false);

        VCFHeader vcfHeader = (VCFHeader)reader.getHeader();

        int genotypeIndex = 0;
        for(int i = 0; i < vcfHeader.getGenotypeSamples().size(); ++i)
        {
            if(vcfHeader.getGenotypeSamples().get(i).equals(sampleId))
            {
                genotypeIndex = i;
                break;
            }
        }

        SomaticVariantResult somaticVariantResult = new SomaticVariantResult();

        try
        {
            for(VariantContext variantContext : reader.iterator())
            {
                if(variantContext.isFiltered())
                    continue;

                try
                {
                    processVariant(sampleId, genotypeIndex, somaticVariantResult, variantContext);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                    CT_LOGGER.error("patient({}) error processing variant", sampleId, e.toString());
                    return INVALID_RESULT;
                }

                if(somaticVariantResult.TotalVariantCount > 0 && (somaticVariantResult.TotalVariantCount % 100000) == 0)
                {
                    CT_LOGGER.info("processed {} variants", somaticVariantResult.TotalVariantCount);
                }
            }

        }
        catch(IOException e)
        {
            CT_LOGGER.error("error reading vcf files: {}", e.toString());
            return INVALID_RESULT;
        }

        CT_LOGGER.info("sample({}) processed {} somatic variants",
                sampleId, somaticVariantResult.TotalVariantCount);

        return somaticVariantResult;
    }

    private void processVariant(
            final String patientId, final int genotypeIndex, final SomaticVariantResult somaticVariantResult, final VariantContext variantContext)
    {
        VariantContextDecorator variant = new VariantContextDecorator(variantContext);

        Genotype genotype = variantContext.getGenotype(genotypeIndex);

        if(genotype == null || genotype.getExtendedAttributes().isEmpty())
        {
            //CT_LOGGER.warn("patientId({}) genotypeInfo({}) missing for variant({}:{}:{}>{})",
            //        patientId, genotypeInfo, variant.chromosome(), variant.position(), variant.ref(), variant.alt());
            return;
        }

        ++somaticVariantResult.TotalVariantCount;

        double subclonalLikelihood = variant.context().getAttributeAsDouble(SUBCLONAL_LIKELIHOOD_FLAG, 0);

        if(filterVariant(variant, subclonalLikelihood))
            return;

        int alleleCount = genotype.getAD()[1];
        int depth = genotype.getDP();
        double qualPerAlleleCount = 0;

        if(alleleCount > 0)
        {
            final String[] qualCounts = genotype.getExtendedAttribute(READ_CONTEXT_QUALITY, 0).toString()
                    .split(LIST_SEPARATOR, -1);

            int qualTotal = 0;
            for(int i = 0; i <= RC_REALIGNED; ++i)
            {
                qualTotal += Integer.parseInt(qualCounts[i]);
            }

            qualPerAlleleCount = qualTotal / (double) alleleCount;

            if(qualPerAlleleCount < MIN_QUAL_PER_AD)
                return;
        }

        ++somaticVariantResult.VariantCount;

        somaticVariantResult.QualPerAdTotal += qualPerAlleleCount;
        somaticVariantResult.VariantDepths.add(depth);
        somaticVariantResult.AlleleFragmentTotal += alleleCount;

        mResultsWriter.writeVariant(
                patientId, genotype.getSampleName(), variant, subclonalLikelihood, alleleCount, depth, qualPerAlleleCount);
    }

     */

}