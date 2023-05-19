package com.hartwig.hmftools.compar.mutation;

import static com.hartwig.hmftools.common.variant.CodingEffect.UNDEFINED;
import static com.hartwig.hmftools.common.variant.CommonVcfTags.PASS;
import static com.hartwig.hmftools.common.variant.PurpleVcfTags.PURPLE_BIALLELIC_FLAG;
import static com.hartwig.hmftools.common.variant.PurpleVcfTags.SUBCLONAL_LIKELIHOOD_FLAG;
import static com.hartwig.hmftools.common.variant.SageVcfTags.LOCAL_PHASE_SET;
import static com.hartwig.hmftools.common.variant.CommonVcfTags.REPORTED_FLAG;
import static com.hartwig.hmftools.common.variant.impact.VariantImpactSerialiser.VAR_IMPACT;
import static com.hartwig.hmftools.compar.Category.SOMATIC_VARIANT;
import static com.hartwig.hmftools.compar.CommonUtils.FLD_QUAL;
import static com.hartwig.hmftools.compar.CommonUtils.FLD_REPORTED;
import static com.hartwig.hmftools.compar.DiffFunctions.FILTER_DIFF;
import static com.hartwig.hmftools.compar.DiffFunctions.checkDiff;
import static com.hartwig.hmftools.compar.DiffFunctions.checkFilterDiffs;
import static com.hartwig.hmftools.compar.MismatchType.VALUE;
import static com.hartwig.hmftools.compar.mutation.VariantCommon.FLD_BIALLELIC;
import static com.hartwig.hmftools.compar.mutation.VariantCommon.FLD_CANON_EFFECT;
import static com.hartwig.hmftools.compar.mutation.VariantCommon.FLD_CODING_EFFECT;
import static com.hartwig.hmftools.compar.mutation.VariantCommon.FLD_GENE;
import static com.hartwig.hmftools.compar.mutation.VariantCommon.FLD_HGVS_CODING;
import static com.hartwig.hmftools.compar.mutation.VariantCommon.FLD_HGVS_PROTEIN;
import static com.hartwig.hmftools.compar.mutation.VariantCommon.FLD_HOTSPOT;
import static com.hartwig.hmftools.compar.mutation.VariantCommon.FLD_OTHER_REPORTED;
import static com.hartwig.hmftools.compar.mutation.VariantCommon.FLD_TIER;
import static com.hartwig.hmftools.patientdb.database.hmfpatients.Tables.SOMATICVARIANT;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.variant.CodingEffect;
import com.hartwig.hmftools.common.variant.Hotspot;
import com.hartwig.hmftools.common.variant.VariantTier;
import com.hartwig.hmftools.common.variant.VariantType;
import com.hartwig.hmftools.common.variant.impact.VariantImpact;
import com.hartwig.hmftools.common.variant.impact.VariantImpactSerialiser;
import com.hartwig.hmftools.compar.Category;
import com.hartwig.hmftools.compar.ComparableItem;
import com.hartwig.hmftools.compar.DiffThresholds;
import com.hartwig.hmftools.compar.MatchLevel;
import com.hartwig.hmftools.compar.Mismatch;
import com.hartwig.hmftools.patientdb.database.hmfpatients.Tables;

import org.jooq.Record;

import htsjdk.variant.variantcontext.VariantContext;

public class SomaticVariantData implements ComparableItem
{
    public final String Chromosome;
    public final int Position;
    public final String Ref;
    public final String Alt;

    public final VariantType Type;
    public final String Gene;

    public final boolean Reported;
    public final Hotspot HotspotStatus;
    public final VariantTier Tier;
    public final boolean Biallelic;
    public final String CanonicalEffect;
    public final String CanonicalCodingEffect;
    public final String CanonicalHgvsCodingImpact;
    public final String CanonicalHgvsProteinImpact;
    public final String OtherReportedEffects;
    public final boolean HasLPS;
    public final int Qual;
    public final double SubclonalLikelihood;
    public final Set<String> Filters;

    private String mComparisonChromosome;
    private int mComparisonPosition;

    protected static final String FLD_SUBCLONAL_LIKELIHOOD = "SubclonalLikelihood";
    protected static final String FLD_LPS = "HasLPS";

    public SomaticVariantData(
            final String chromosome, final int position, final String ref, final String alt, final VariantType type,
            final String gene, final boolean reported, final Hotspot hotspotStatus, final VariantTier tier, final boolean biallelic,
            final String canonicalEffect, final String canonicalCodingEffect, final String canonicalHgvsCodingImpact,
            final String canonicalHgvsProteinImpact, final String otherReportedEffects, final boolean hasLPS, final int qual,
            final double subclonalLikelihood, final Set<String> filters)
    {
        Chromosome = chromosome;
        Position = position;
        Ref = ref;
        Alt = alt;
        Type = type;
        Gene = gene;
        Reported = reported;
        HotspotStatus = hotspotStatus;
        Tier = tier;
        Biallelic = biallelic;
        CanonicalEffect = canonicalEffect;
        CanonicalCodingEffect = canonicalCodingEffect;
        CanonicalHgvsCodingImpact = canonicalHgvsCodingImpact;
        CanonicalHgvsProteinImpact = canonicalHgvsProteinImpact;
        OtherReportedEffects = otherReportedEffects != null ? otherReportedEffects : "";
        HasLPS = hasLPS;
        Qual = qual;
        SubclonalLikelihood = subclonalLikelihood;
        Filters = filters;

        mComparisonChromosome = chromosome;
        mComparisonPosition = position;
    }

    @Override
    public Category category() { return SOMATIC_VARIANT; }

    @Override
    public String key()
    {
        return String.format("%s:%d %s>%s %s", Chromosome, Position, Ref, Alt, Type);
    }

    @Override
    public List<String> displayValues()
    {
        List<String> values = Lists.newArrayList();
        values.add(String.format("%s", Reported));
        values.add(String.format("%s", HotspotStatus));
        values.add(String.format("%s", Tier));
        values.add(String.format("%s", Biallelic));
        values.add(String.format("%s", Gene));
        values.add(String.format("%s", CanonicalEffect));
        values.add(String.format("%s", CanonicalCodingEffect));
        values.add(String.format("%s", CanonicalHgvsCodingImpact));
        values.add(String.format("%s", CanonicalHgvsProteinImpact));
        values.add(String.format("%s", OtherReportedEffects));
        values.add(String.format("%d", Qual));

        values.add(String.format("%.2f", SubclonalLikelihood));
        values.add(String.format("%s", HasLPS));

        return values;
    }

    @Override
    public boolean reportable() { return Reported; }

    @Override
    public boolean matches(final ComparableItem other)
    {
        final SomaticVariantData otherVar = (SomaticVariantData) other;

        if(!mComparisonChromosome.equals(otherVar.comparisonChromosome()) || mComparisonPosition != otherVar.comparisonPosition())
            return false;

        if(!Ref.equals(otherVar.Ref) || !Alt.equals(otherVar.Alt))
            return false;

        if(Type != otherVar.Type)
            return false;

        return true;
    }

    public String comparisonChromosome() { return mComparisonChromosome; }
    public int comparisonPosition() { return mComparisonPosition; }

    public void setComparisonCoordinates(final String chromosome, final int position)
    {
        mComparisonChromosome = chromosome;
        mComparisonPosition = position;
    }

    @Override
    public Mismatch findMismatch(final ComparableItem other, final MatchLevel matchLevel, final DiffThresholds thresholds)
    {
        return findDiffs(other, thresholds, false);
    }

    protected Mismatch findDiffs(final ComparableItem other, final DiffThresholds thresholds, boolean isUnfiltered)
    {
        final SomaticVariantData otherVar = (SomaticVariantData) other;

        final List<String> diffs = Lists.newArrayList();

        checkDiff(diffs, FLD_QUAL, Qual, otherVar.Qual, thresholds);
        checkDiff(diffs, FLD_REPORTED, Reported, otherVar.Reported);
        checkDiff(diffs, FLD_HOTSPOT, HotspotStatus.toString(), otherVar.HotspotStatus.toString());
        checkDiff(diffs, FLD_TIER, Tier.toString(), otherVar.Tier.toString());

        if(!isUnfiltered)
        {
            checkDiff(diffs, FLD_BIALLELIC, Biallelic, otherVar.Biallelic);
            checkDiff(diffs, FLD_GENE, Gene, otherVar.Gene);
            checkDiff(diffs, FLD_CANON_EFFECT, CanonicalEffect, otherVar.CanonicalEffect);
            checkDiff(diffs, FLD_CODING_EFFECT, CanonicalCodingEffect, otherVar.CanonicalCodingEffect);
            checkDiff(diffs, FLD_HGVS_CODING, CanonicalHgvsCodingImpact, otherVar.CanonicalHgvsCodingImpact);
            checkDiff(diffs, FLD_HGVS_PROTEIN, CanonicalHgvsProteinImpact, otherVar.CanonicalHgvsProteinImpact);
            checkDiff(diffs, FLD_OTHER_REPORTED, OtherReportedEffects, otherVar.OtherReportedEffects);

            checkDiff(diffs, FLD_SUBCLONAL_LIKELIHOOD, SubclonalLikelihood, otherVar.SubclonalLikelihood, thresholds);
        }

        checkDiff(diffs, FLD_LPS, HasLPS, otherVar.HasLPS);

        // compare filters
        checkFilterDiffs(Filters, otherVar.Filters, diffs);

        if(isUnfiltered && !diffs.contains(FILTER_DIFF))
        {
            // suggest was filtered downstream of Sage , eg Pave or Purple so indicate this
            if(Filters.isEmpty() && otherVar.Filters.isEmpty())
                diffs.add(String.format("%s(%s/%s)", FILTER_DIFF, PASS, "FILTERED"));
        }

        return !diffs.isEmpty() ? new Mismatch(this, other, VALUE, diffs) : null;
    }

    public static SomaticVariantData fromContext(final VariantContext context)
    {
        int position = context.getStart();
        String chromosome = context.getContig();
        String ref = context.getReference().getBaseString();
        String alt = !context.getAlternateAlleles().isEmpty() ? context.getAlternateAlleles().get(0).toString() : ref;

        VariantImpact variantImpact;

        if(context.hasAttribute(VAR_IMPACT))
            variantImpact = VariantImpactSerialiser.fromVariantContext(context);
        else
            variantImpact = fromSnpEffAttributes(context);

        return new SomaticVariantData(
                chromosome, position, ref, alt, VariantType.type(context),
                variantImpact.CanonicalGeneName,
                context.getAttributeAsBoolean(REPORTED_FLAG, false),
                Hotspot.fromVariant(context),
                VariantTier.fromContext(context),
                context.getAttributeAsBoolean(PURPLE_BIALLELIC_FLAG, false),
                variantImpact.CanonicalEffect,
                variantImpact.CanonicalCodingEffect.toString(),
                variantImpact.CanonicalHgvsCoding,
                variantImpact.CanonicalHgvsProtein,
                variantImpact.OtherReportableEffects,
                context.hasAttribute(LOCAL_PHASE_SET),
                (int)context.getPhredScaledQual(),
                context.getAttributeAsDouble(SUBCLONAL_LIKELIHOOD_FLAG, 0),
                context.getFilters());
    }

    public static SomaticVariantData fromRecord(final Record record)
    {
        Set<String> filters = Arrays.stream(record.getValue(SOMATICVARIANT.FILTER).split(";", -1)).collect(Collectors.toSet());
        String localPhaseSets = record.get(SOMATICVARIANT.LOCALPHASESET);
        double qual = record.getValue(Tables.SOMATICVARIANT.QUAL);

        return new SomaticVariantData(
                record.getValue(Tables.SOMATICVARIANT.CHROMOSOME),
                record.getValue(Tables.SOMATICVARIANT.POSITION),
                record.getValue(Tables.SOMATICVARIANT.REF),
                record.getValue(Tables.SOMATICVARIANT.ALT),
                VariantType.valueOf(record.getValue(SOMATICVARIANT.TYPE)),
                record.getValue(Tables.SOMATICVARIANT.GENE),
                record.getValue(SOMATICVARIANT.REPORTED).intValue() == 1,
                Hotspot.valueOf(record.getValue(SOMATICVARIANT.HOTSPOT)),
                VariantTier.fromString(record.get(SOMATICVARIANT.TIER)),
                record.getValue(SOMATICVARIANT.BIALLELIC).intValue() == 1,
                record.getValue(SOMATICVARIANT.CANONICALEFFECT),
                record.getValue(SOMATICVARIANT.CANONICALCODINGEFFECT),
                record.getValue(SOMATICVARIANT.CANONICALHGVSCODINGIMPACT),
                record.getValue(SOMATICVARIANT.CANONICALHGVSPROTEINIMPACT),
                record.getValue(SOMATICVARIANT.OTHERTRANSCRIPTEFFECTS),
                localPhaseSets != null && !localPhaseSets.isEmpty(),
                (int)qual, record.getValue(SOMATICVARIANT.SUBCLONALLIKELIHOOD),
                filters);
    }

    private static final String SNPEFF_WORST = "SEW";
    private static final String SNPEFF_CANONICAL = "SEC";

    private static final VariantImpact INVALID_IMPACT = new VariantImpact(
            "", "", "", UNDEFINED, "", "",
            false, "", UNDEFINED, 0);

    private static VariantImpact fromSnpEffAttributes(final VariantContext context)
    {
        if(!context.hasAttribute(SNPEFF_WORST) || !context.hasAttribute(SNPEFF_CANONICAL))
            return INVALID_IMPACT;

        final List<String> worst = context.getAttributeAsStringList(SNPEFF_WORST, "");
        final List<String> canonical = context.getAttributeAsStringList(SNPEFF_CANONICAL, "");

        String canonicalGeneName = "";
        String canonicalEffect = "";
        String canonicalTranscript = "";
        CodingEffect canonicalCodingEffect = UNDEFINED;
        String canonicalHgvsCodingImpact = "";
        String canonicalHgvsProteinImpact = "";
        boolean canonicalSpliceRegion = false;
        String otherReportableEffects = "";
        CodingEffect worstCodingEffect = UNDEFINED;
        int genesAffected = 0;

        if(worst.size() == 5)
        {
            worstCodingEffect = CodingEffect.valueOf(worst.get(3));
            genesAffected = Integer.parseInt(worst.get(4));
        }

        if(canonical.size() == 6)
        {
            canonicalGeneName = canonical.get(0);
            canonicalTranscript = canonical.get(1);
            canonicalEffect = canonical.get(2);
            canonicalCodingEffect = CodingEffect.valueOf(canonical.get(3));
            canonicalHgvsCodingImpact = canonical.get(4);
            canonicalHgvsProteinImpact = canonical.get(5);

            canonicalSpliceRegion = canonicalEffect.contains("splice");
        }

        return new VariantImpact(
                canonicalGeneName, canonicalTranscript, canonicalEffect, canonicalCodingEffect, canonicalHgvsCodingImpact,
                canonicalHgvsProteinImpact, canonicalSpliceRegion, otherReportableEffects, worstCodingEffect, genesAffected);
    }
}
