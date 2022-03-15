package com.hartwig.hmftools.serve.sources.vicc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hartwig.hmftools.common.serve.actionability.EvidenceDirection;
import com.hartwig.hmftools.common.serve.actionability.EvidenceLevel;
import com.hartwig.hmftools.serve.actionability.ActionableEvent;
import com.hartwig.hmftools.serve.cancertype.ImmutableCancerType;
import com.hartwig.hmftools.serve.curation.DoidLookupTestFactory;
import com.hartwig.hmftools.serve.sources.vicc.curation.DrugCurator;
import com.hartwig.hmftools.serve.sources.vicc.curation.EvidenceLevelCurator;
import com.hartwig.hmftools.vicc.datamodel.Association;
import com.hartwig.hmftools.vicc.datamodel.ImmutableViccEntry;
import com.hartwig.hmftools.vicc.datamodel.ViccEntry;

import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class ActionableEvidenceFactoryTest {

    @Test
    public void canResolveActionableEventWithMultipleCancerTypes() {
        String cancerTypeA = "cancerTypeA";
        String cancerTypeB = "cancerTypeB";

        Map<String, Set<String>> doidLookupMap = Maps.newHashMap();
        doidLookupMap.put(cancerTypeA, Sets.newHashSet("1"));
        doidLookupMap.put(cancerTypeB, Sets.newHashSet("162"));
        ActionableEvidenceFactory factory =
                new ActionableEvidenceFactory(DoidLookupTestFactory.test(doidLookupMap), new DrugCurator(), new EvidenceLevelCurator());

        Association actionable = ViccTestFactory.testActionableAssociation("Treatment",
                cancerTypeA + ";" + cancerTypeB,
                "DOID:162",
                "A",
                "Responsive",
                "url");

        ViccEntry entry = ViccTestFactory.testEntryWithGeneEventAndAssociation("gene", "event", actionable);
        Set<ActionableEvent> events = factory.toActionableEvents(entry, Strings.EMPTY);
        assertEquals(2, events.size());

        ActionableEvent eventA = findByCancerType(events, cancerTypeA);
        assertEquals("Treatment", eventA.treatment());
        assertEquals(cancerTypeA, eventA.applicableCancerType().name());
        assertEquals("1", eventA.applicableCancerType().doid());
        assertTrue(eventA.blacklistCancerTypes().isEmpty());
        assertEquals(EvidenceLevel.A, eventA.level());
        assertEquals(EvidenceDirection.RESPONSIVE, eventA.direction());
        assertEquals(Sets.newHashSet("url"), eventA.evidenceUrls());

        ActionableEvent eventB = findByCancerType(events, cancerTypeB);
        assertEquals("Treatment", eventB.treatment());
        assertEquals(cancerTypeB, eventB.applicableCancerType().name());
        assertEquals("162", eventB.applicableCancerType().doid());
        assertEquals(EvidenceLevel.A, eventB.level());
        assertEquals(EvidenceDirection.RESPONSIVE, eventB.direction());
        assertEquals(Sets.newHashSet("url"), eventB.evidenceUrls());
        assertEquals(Sets.newHashSet(ImmutableCancerType.builder().name("Hematologic cancer").doid("2531").build()),
                eventB.blacklistCancerTypes());
        factory.evaluateCuration();
    }

    @NotNull
    private static ActionableEvent findByCancerType(@NotNull Iterable<ActionableEvent> events, @NotNull String cancerType) {
        for (ActionableEvent event : events) {
            if (event.applicableCancerType().name().equals(cancerType)) {
                return event;
            }
        }
        throw new IllegalStateException("Could not resolve event with cancer type: " + cancerType);
    }

    @Test
    public void canReformatDrugs() {
        assertEquals("Imatinib,Imatinib", ActionableEvidenceFactory.reformatDrugLabels("IMATINIB,IMATINIB"));
        assertEquals("Fluorouracil,Irinotecan,Bevacizumab,Lysergide",
                ActionableEvidenceFactory.reformatDrugLabels("FLUOROURACIL,Irinotecan,BEVACIZUMAB,Lysergide"));

        assertNull(ActionableEvidenceFactory.reformatDrugLabels(null));
    }

    @Test
    public void canReformatField() {
        assertEquals("Field", ActionableEvidenceFactory.reformatField("Field"));
        assertEquals("Field", ActionableEvidenceFactory.reformatField("field"));
        assertEquals("Field", ActionableEvidenceFactory.reformatField("FIELD"));

        assertEquals("F", ActionableEvidenceFactory.reformatField("F"));
        assertEquals("F", ActionableEvidenceFactory.reformatField("f"));
        assertEquals("", ActionableEvidenceFactory.reformatField(""));
        assertNull(ActionableEvidenceFactory.reformatField(null));
    }

    @Test
    public void canResolveDirection() {
        assertEquals(EvidenceDirection.RESPONSIVE, ActionableEvidenceFactory.resolveDirection("Responsive"));
        assertEquals(EvidenceDirection.RESPONSIVE, ActionableEvidenceFactory.resolveDirection("Sensitive"));
        assertEquals(EvidenceDirection.RESISTANT, ActionableEvidenceFactory.resolveDirection("Resistant"));

        assertNull(ActionableEvidenceFactory.resolveDirection(null));
        assertNull(ActionableEvidenceFactory.resolveDirection("Conflicting"));
        assertNull(ActionableEvidenceFactory.resolveDirection("This is no direction"));
    }

    @Test
    public void canResolveLevel() {
        assertEquals(EvidenceLevel.A, ActionableEvidenceFactory.resolveLevel("A"));

        assertNull(ActionableEvidenceFactory.resolveLevel(null));
        assertNull(ActionableEvidenceFactory.resolveLevel("XXX"));
    }

    @Test
    public void canExtractDoid() {
        assertEquals("123", ActionableEvidenceFactory.extractDoid("DOID:123"));
        assertNull(ActionableEvidenceFactory.extractDoid("SNOMED:123"));
        assertNull(ActionableEvidenceFactory.extractDoid("DOID"));

        assertNull(ActionableEvidenceFactory.extractDoid(null));
    }

    @Test
    public void canFilterNonSupportiveEvidence() {
        ViccEntry actionable = ViccTestFactory.testEntryWithGeneEventAndAssociation("gene",
                "event",
                ViccTestFactory.testActionableAssociation("Treatment", "Cancer", "DOID:162", "A", "Responsive", "url"));

        Map<String, Set<String>> doidLookupMap = Maps.newHashMap();
        doidLookupMap.put("Cancer", Sets.newHashSet("162"));
        ActionableEvidenceFactory factory =
                new ActionableEvidenceFactory(DoidLookupTestFactory.test(doidLookupMap), new DrugCurator(), new EvidenceLevelCurator());

        ViccEntry doesNotSupport = ImmutableViccEntry.builder()
                .from(actionable)
                .kbSpecificObject(ViccTestFactory.testEntryWithCivicEvidenceDirection("Does Not Support").kbSpecificObject())
                .build();

        assertEquals(0, factory.toActionableEvents(doesNotSupport, Strings.EMPTY).size());

        ViccEntry supports = ImmutableViccEntry.builder()
                .from(actionable)
                .kbSpecificObject(ViccTestFactory.testEntryWithCivicEvidenceDirection("Supports").kbSpecificObject())
                .build();

        assertEquals(1, factory.toActionableEvents(supports, Strings.EMPTY).size());

        ViccEntry undefined = ImmutableViccEntry.builder()
                .from(actionable)
                .kbSpecificObject(ViccTestFactory.testEntryWithCivicEvidenceDirection(null).kbSpecificObject())
                .build();

        assertEquals(1, factory.toActionableEvents(undefined, Strings.EMPTY).size());

        ViccEntry notRecognized = ImmutableViccEntry.builder()
                .from(actionable)
                .kbSpecificObject(ViccTestFactory.testEntryWithCivicEvidenceDirection("Not a direction").kbSpecificObject())
                .build();

        assertEquals(1, factory.toActionableEvents(notRecognized, Strings.EMPTY).size());
    }
}