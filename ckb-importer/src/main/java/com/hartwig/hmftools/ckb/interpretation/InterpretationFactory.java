package com.hartwig.hmftools.ckb.interpretation;

import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.ckb.datamodel.CkbEntry;
import com.hartwig.hmftools.ckb.datamodel.common.DrugInfo;
import com.hartwig.hmftools.ckb.datamodel.common.TreatmentApproachInfo;
import com.hartwig.hmftools.ckb.datamodel.common.VariantInfo;
import com.hartwig.hmftools.ckb.datamodel.drug.Drug;
import com.hartwig.hmftools.ckb.datamodel.drugclass.DrugClass;
import com.hartwig.hmftools.ckb.datamodel.gene.Gene;
import com.hartwig.hmftools.ckb.datamodel.molecularprofile.MolecularProfile;
import com.hartwig.hmftools.ckb.datamodel.therapy.Therapy;
import com.hartwig.hmftools.ckb.datamodel.treatmentapproach.TreatmentApproach;
import com.hartwig.hmftools.ckb.datamodel.variant.Variant;
import com.hartwig.hmftools.ckb.interpretation.treatmenttree.DrugClassInterpretation;
import com.hartwig.hmftools.ckb.interpretation.treatmenttree.ImmutableDrugClassInterpretation;
import com.hartwig.hmftools.ckb.interpretation.treatmenttree.ImmutableTreatmentApprochInterpretation;
import com.hartwig.hmftools.ckb.interpretation.treatmenttree.ImmutableTreatmentInterpretation;
import com.hartwig.hmftools.ckb.interpretation.treatmenttree.TreatmentApprochInterpretation;
import com.hartwig.hmftools.ckb.interpretation.treatmenttree.TreatmentInterpretation;
import com.hartwig.hmftools.ckb.interpretation.varianttree.ImmutableVariantInterpretation;
import com.hartwig.hmftools.ckb.interpretation.varianttree.VariantInterpretation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class InterpretationFactory {

    private InterpretationFactory() {

    }

    private static final Logger LOGGER = LogManager.getLogger(InterpretationFactory.class);

    public static List<CkbEntryInterpretation> interpretationCkb(@NotNull CkbEntry ckbEntry) {
        List<CkbEntryInterpretation> CkbEntryInterpretation = Lists.newArrayList();
        for (MolecularProfile molecularProfile : ckbEntry.molecularProfile()) {
            ImmutableCkbEntryInterpretation.Builder outputBuilder = ImmutableCkbEntryInterpretation.builder();
            outputBuilder.molecularProfile(molecularProfile);

            for (VariantInfo variantInfo : molecularProfile.geneVariant()) {
                VariantInterpretation variantInterpretation = matchVariantInterpretation(ckbEntry, variantInfo.id()); //array
                outputBuilder.addVariantInterpretation(variantInterpretation);

            }

            for (TreatmentApproachInfo treatmentApproachInfo : molecularProfile.treatmentApproach()) {
                TreatmentInterpretation treatmentInterpretation =
                        matchTreatmentInterpretation(ckbEntry, treatmentApproachInfo.id()); //array
                outputBuilder.addTreatmentInterpretation(treatmentInterpretation);
                if (treatmentApproachInfo.id() == 467) {
                    LOGGER.info(treatmentInterpretation);

                }
            }

            CkbEntryInterpretation.add(outputBuilder.build());
        }
        return CkbEntryInterpretation;
    }

    @NotNull
    private static VariantInterpretation matchVariantInterpretation(@NotNull CkbEntry ckbEntry, int variantId) {
        ImmutableVariantInterpretation.Builder outputBuilder = ImmutableVariantInterpretation.builder();
        for (Variant variant : ckbEntry.variant()) {
            if (variant.id() == variantId) {
                int geneId = variant.gene().id();
                outputBuilder.variant(variant); //array
                for (Gene gene : ckbEntry.gene()) {
                    if (gene.id() == geneId) {
                        outputBuilder.gene(gene);  //object
                    }
                }
            }
        }
        return outputBuilder.build();
    }

    @NotNull
    private static TreatmentInterpretation matchTreatmentInterpretation(@NotNull CkbEntry ckbEntry, int treatmentApprochId) {
        ImmutableTreatmentInterpretation.Builder outputBuilder = ImmutableTreatmentInterpretation.builder();
        for (TreatmentApproach treatmentApproach : ckbEntry.treatmentApproach()) {
            if (treatmentApproach.id() == treatmentApprochId) {
                outputBuilder.treatmentApproach(treatmentApproach);
                outputBuilder.treatmentApproachInterpretation(matchTreatmentApprochInterpretation(ckbEntry,
                        treatmentApproach.id(),
                        treatmentApproach)); //array

            }
        }
        return outputBuilder.build();
    }

    @NotNull
    private static TreatmentApprochInterpretation matchTreatmentApprochInterpretation(@NotNull CkbEntry ckbEntry, int treatmentApprochId,
            @NotNull TreatmentApproach treatmentApproach) {
        ImmutableTreatmentApprochInterpretation.Builder outputBuilder = ImmutableTreatmentApprochInterpretation.builder();

        for (DrugClass drugClass : ckbEntry.drugClass()) {
            if (treatmentApproach.drugClass() != null) {
                outputBuilder.drugClassInterpretation(matchDrugClassInterpretation(drugClass, treatmentApproach, ckbEntry));
            }
        }

        for (Therapy therapy : ckbEntry.therapy()) {
            if (treatmentApproach.therapy() != null) {
                if (therapy.id() == treatmentApproach.therapy().id()) {
                    outputBuilder.therapy(therapy); //object
                }
            }
        }
        return outputBuilder.build();
    }

    @NotNull
    private static DrugClassInterpretation matchDrugClassInterpretation(@NotNull DrugClass drugClass,
            @NotNull TreatmentApproach treatmentApproach, @NotNull CkbEntry ckbEntry) {
        ImmutableDrugClassInterpretation.Builder outputBuilder = ImmutableDrugClassInterpretation.builder();
        if (drugClass.id() == treatmentApproach.drugClass().id()) {
            outputBuilder.addDrugClass(drugClass);  //object
            for (DrugInfo drugInfo : drugClass.drug()) { //array
                for (Drug drug : ckbEntry.drug()) {
                    if (drugInfo.id() == drug.id()) {
                        outputBuilder.drug(drug);
                    }
                }
            }
        }
        return outputBuilder.build();
    }

}
