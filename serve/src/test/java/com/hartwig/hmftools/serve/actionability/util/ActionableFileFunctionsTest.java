package com.hartwig.hmftools.serve.actionability.util;

import static com.hartwig.hmftools.serve.actionability.util.ActionableFileFunctions.FIELD_DELIMITER;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Sets;
import com.hartwig.hmftools.common.serve.Knowledgebase;
import com.hartwig.hmftools.common.serve.actionability.EvidenceDirection;
import com.hartwig.hmftools.common.serve.actionability.EvidenceLevel;
import com.hartwig.hmftools.serve.actionability.ActionabilityTestUtil;
import com.hartwig.hmftools.serve.actionability.ActionableEvent;
import com.hartwig.hmftools.serve.tumorlocation.ImmutableTumorLocation;

import org.junit.Test;

public class ActionableFileFunctionsTest {

    @Test
    public void canConvertActionableEvents() {
        ActionableEvent event =
                ActionabilityTestUtil.create(Knowledgebase.VICC_CGI,
                        "rawInput",
                        Sets.newHashSet(),
                        "treatment",
                        ImmutableTumorLocation.builder()
                                .cancerType("whitlist cancertype")
                                .doid("whitlist doid")
                                .build(),
                        Sets.newHashSet(ImmutableTumorLocation.builder()
                                .cancerType("blacklist cancertype")
                                .doid("blacklist doid")
                                .build()),
                        EvidenceLevel.C,
                        EvidenceDirection.RESISTANT,
                        Sets.newHashSet("url1", "url2"));

        String line = ActionableFileFunctions.toLine(event);
        ActionableEvent convertedEvent = ActionableFileFunctions.fromLine(line.split(FIELD_DELIMITER), 0);

        assertEquals(Knowledgebase.VICC_CGI, convertedEvent.source());
        assertEquals("treatment", convertedEvent.treatment());
        assertEquals("whitlist cancertype", convertedEvent.whiteList().cancerType());
        assertEquals("whitlist doid", convertedEvent.whiteList().doid());
        assertEquals(EvidenceLevel.C, convertedEvent.level());
        assertEquals(EvidenceDirection.RESISTANT, convertedEvent.direction());
        assertEquals(Sets.newHashSet("url1", "url2"), convertedEvent.evidenceUrls());
    }
}