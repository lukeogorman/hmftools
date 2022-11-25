package com.hartwig.hmftools.peach;

import com.hartwig.hmftools.peach.haplotype.NonWildTypeHaplotype;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.hartwig.hmftools.peach.PeachUtils.PCH_LOGGER;

public class HaplotypeCaller
{
    @NotNull
    private final HaplotypePanel haplotypePanel;

    public HaplotypeCaller(@NotNull HaplotypePanel haplotypePanel)
    {
        this.haplotypePanel = haplotypePanel;
    }

    public void callPossibleHaplotypes(@NotNull Map<String, Integer> eventIdToCount)
    {
        //haplotypePanel.getGenes().stream().map(g -> callPossibleHaplotypes(eventIdToCount, g));
    }

    private void callPossibleHaplotypes(Map<String, Integer> eventIdToCount, String gene)
    {
        PCH_LOGGER.info("handling gene: {}", gene);
        Map<String, Integer> relevantEventIdToCount = eventIdToCount.entrySet().stream()
                .filter(e -> haplotypePanel.isRelevantFor(e.getKey(), gene))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        PCH_LOGGER.info("events for gene '{}': {}", gene, relevantEventIdToCount);

        if (relevantEventIdToCount.values().stream().anyMatch(c -> c < 0))
        {
            String error_msg = String.format(
                    "Cannot call haplotypes for %s since some event counts are negative: %s",
                    gene,
                    relevantEventIdToCount
            );
            throw new RuntimeException(error_msg);
        }

//        getPossibleNonWildTypeHaplotypes(
//                relevantEventIdToCount,
//                new ArrayList<>(),
//                List.copyOf(haplotypePanel.getNonWildTypeHaplotypes(gene))
//        );
    }

//    private List<HaplotypeCombination> getPossibleNonWildTypeHaplotypes(
//            Map<String, Integer> eventIdToCount,
//            List<String> currentCandidateCombination,
//            List<NonWildTypeHaplotype> haplotypes)
//    {
//        if (eventIdToCount.values().stream().anyMatch(c -> c < 0))
//        {
//            return Collections.emptyList();
//        }
//        else if (eventIdToCount.values().stream().allMatch(c -> c == 0))
//        {
//            return List.of(getCombination(currentCandidateCombination));
//        }
//
//
//
//    }
//
//    private HaplotypeCombination getCombination(List<String> nonWildTypeHaplotypeNames, String gene)
//    {
//        Map<String, Integer> haplotypeNameToCount = nonWildTypeHaplotypeNames.stream()
//                .collect(Collectors.groupingBy(h -> h, Collectors.summingInt(h -> 1)));
//        if (nonWildTypeHaplotypeNames.size() == 0)
//            haplotypeNameToCount.put(haplotypePanel.getWildTypeHaplotype(gene).);
//        return new HaplotypeCombination(haplotypeNameToCount);
//    }
}
