package com.hartwig.hmftools.knowledgebasegenerator.cnv;

import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.knowledgebasegenerator.actionability.gene.ActionableGene;
import com.hartwig.hmftools.knowledgebasegenerator.actionability.gene.ImmutableActionableGene;
import com.hartwig.hmftools.knowledgebasegenerator.eventtype.EventType;
import com.hartwig.hmftools.knowledgebasegenerator.sourceknowledgebase.Source;
import com.hartwig.hmftools.vicc.datamodel.KbSpecificObject;
import com.hartwig.hmftools.vicc.datamodel.ViccEntry;
import com.hartwig.hmftools.vicc.datamodel.brca.Brca;
import com.hartwig.hmftools.vicc.datamodel.cgi.Cgi;
import com.hartwig.hmftools.vicc.datamodel.civic.Civic;
import com.hartwig.hmftools.vicc.datamodel.jax.Jax;
import com.hartwig.hmftools.vicc.datamodel.jaxtrials.JaxTrials;
import com.hartwig.hmftools.vicc.datamodel.molecularmatch.MolecularMatch;
import com.hartwig.hmftools.vicc.datamodel.molecularmatchtrials.MolecularMatchTrials;
import com.hartwig.hmftools.vicc.datamodel.oncokb.OncoKb;
import com.hartwig.hmftools.vicc.datamodel.pmkb.Pmkb;
import com.hartwig.hmftools.vicc.datamodel.sage.Sage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

public class CnvExtractor {

    private static final Logger LOGGER = LogManager.getLogger(CnvExtractor.class);

    private static final List<String> AMPLIFICATION =
            Lists.newArrayList("Amplification", "Overexpression", "amp", "OVEREXPRESSION", "Transcript Amplification");
    private static final List<String> DELETION = Lists.newArrayList("Copy Number Loss", "Deletion", "del", "DELETION", "UNDEREXPRESSION");

    @NotNull
    public static ActionableGene extractingCNVs(@NotNull ViccEntry viccEntries, @NotNull EventType type) {
        Source source = Source.sourceFromKnowledgebase(viccEntries.source());
        KbSpecificObject kbSpecificObject = viccEntries.KbSpecificObject();
        String gene = Strings.EMPTY;
        String typeEvent = Strings.EMPTY;
        String drug = Strings.EMPTY;
        String drugType = Strings.EMPTY;
        String cancerType = Strings.EMPTY;
        String level = Strings.EMPTY;
        String direction = Strings.EMPTY;
        String link = Strings.EMPTY;

        if (AMPLIFICATION.contains(type.eventType())) {
            // extract evidence items

            switch (source) {
                case ONCOKB:
                    OncoKb kbOncoKb = (OncoKb) kbSpecificObject;


                    LOGGER.info("AMP oncokb");
                    break;
                case CGI:
                    Cgi kbCgi = (Cgi) kbSpecificObject;

                    LOGGER.info("AMP cgi");
                    break;
                case CIVIC:
                    Civic kbCivic = (Civic) kbSpecificObject;

                    LOGGER.info("AMP civic");
                    break;
                case JAX:
                    Jax kbJax = (Jax) kbSpecificObject;

                    break;
                case JAX_TRIALS:
                    JaxTrials kbJaxTrials = (JaxTrials) kbSpecificObject;

                    break;
                case BRCA:
                    Brca kbBrca = (Brca) kbSpecificObject;

                    break;
                case SAGE:
                    Sage kbSage = (Sage) kbSpecificObject;

                    break;
                case PMKB:
                    Pmkb kbPmkb = (Pmkb) kbSpecificObject;

                    break;
                case MOLECULARMATCH:
                    MolecularMatch kbMolecularMatch = (MolecularMatch) kbSpecificObject;

                    break;
                case MOLECULARMATCH_TRIALS:
                    MolecularMatchTrials kbMolecularMatchTrials = (MolecularMatchTrials) kbSpecificObject;

                    break;
                default:
                    LOGGER.warn("Unknown knowledgebase");
            }
            gene = type.gene();
            typeEvent = "Amplification";

        } else if (DELETION.contains(type.eventType())) {
            // extract evidence items

            switch (source) {
                case ONCOKB:
                    OncoKb kbOncoKb = (OncoKb) kbSpecificObject;

                    LOGGER.info("DEL oncokb");
                    break;
                case CGI:
                    Cgi kbCgi = (Cgi) kbSpecificObject;
                    LOGGER.info("DEL cgi");
                    break;
                case CIVIC:
                    Civic kbCivic = (Civic) kbSpecificObject;

                    LOGGER.info("DEL civic");
                    break;
                case JAX:
                    Jax kbJax = (Jax) kbSpecificObject;

                    break;
                case JAX_TRIALS:
                    JaxTrials kbJaxTrials = (JaxTrials) kbSpecificObject;

                    break;
                case BRCA:
                    Brca kbBrca = (Brca) kbSpecificObject;

                    break;
                case SAGE:
                    Sage kbSage = (Sage) kbSpecificObject;

                    break;
                case PMKB:
                    Pmkb kbPmkb = (Pmkb) kbSpecificObject;

                    break;
                case MOLECULARMATCH:
                    MolecularMatch kbMolecularMatch = (MolecularMatch) kbSpecificObject;

                    break;
                case MOLECULARMATCH_TRIALS:
                    MolecularMatchTrials kbMolecularMatchTrials = (MolecularMatchTrials) kbSpecificObject;

                    break;
                default:
                    LOGGER.warn("Unknown knowledgebase");
            }
            gene = type.gene();
            typeEvent = "Deletion";
        }
        return  ImmutableActionableGene.builder()
                .gene(gene)
                .type(typeEvent)
                .source(source.toString())
                .drug(drug)
                .drugType(drugType)
                .cancerType(cancerType)
                .level(level)
                .direction(direction)
                .link(link)
                .build();
    }
}
