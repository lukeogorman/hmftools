package com.hartwig.hmftools.patientdb.curators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.io.Resources;
import com.hartwig.hmftools.patientdb.data.CuratedTreatment;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

public class TreatmentCuratorTest {
    private static final Logger LOGGER = LogManager.getLogger(TreatmentCurator.class);
    private static final String TREATMENT_MAPPING_CSV = Resources.getResource("treatment_mapping.csv").getPath();
    private static final TreatmentCurator MATCHER;

    static {
        try {
            MATCHER = new TreatmentCurator(new FileInputStream(TREATMENT_MAPPING_CSV));
        } catch (IOException e) {
            LOGGER.error(e);
            throw new RuntimeException(e);
        }
    }

    @Test
    public void matchesExactSingleWord() throws IOException {
        final Optional<CuratedTreatment> matchedTreatment = MATCHER.matchSingle("Zocor");
        assertTrue(matchedTreatment.isPresent());
        assertEquals("Zocor", matchedTreatment.get().name());
    }

    @Test
    public void removesSearchTermsFromUnused() throws IOException {
        TreatmentCurator curator = new TreatmentCurator(new FileInputStream(TREATMENT_MAPPING_CSV));
        assertEquals(22, curator.unusedSearchTerms().size());

        curator.search("Zocor");
        assertEquals(21, curator.unusedSearchTerms().size());

        curator.search("amlodipine besylate");
        assertEquals(20, curator.unusedSearchTerms().size());

        curator.search("Avastine");
        assertEquals(19, curator.unusedSearchTerms().size());

        curator.search("This does not match at all!");
        assertEquals(19, curator.unusedSearchTerms().size());
    }

    @Test
    public void matchesIgnoringCaseSingleWord() throws IOException {
        final Optional<CuratedTreatment> matchedTreatment = MATCHER.matchSingle("zocor");
        assertTrue(matchedTreatment.isPresent());
        assertEquals("Zocor", matchedTreatment.get().name());
    }

    @Test
    public void matchesSingleWordWithTypo() throws IOException {
        final Optional<CuratedTreatment> matchedTreatment = MATCHER.matchSingle("lisinoprill");
        assertTrue(matchedTreatment.isPresent());
        assertEquals("Lisinopril", matchedTreatment.get().name());
    }

    @Test
    public void matchesExactMultiWord() throws IOException {
        final Optional<CuratedTreatment> matchedTreatment = MATCHER.matchSingle("amlodipine besylate");
        assertTrue(matchedTreatment.isPresent());
        assertEquals("Norvasc", matchedTreatment.get().name());
    }

    @Test
    public void matchesIgnoringCaseMultiWord() throws IOException {
        final Optional<CuratedTreatment> matchedTreatment = MATCHER.matchSingle("Amlodipine Besylate");
        assertTrue(matchedTreatment.isPresent());
        assertEquals("Norvasc", matchedTreatment.get().name());
    }

    @Test
    public void matchesMultiWordWithTypo() throws IOException {
        final Optional<CuratedTreatment> matchedTreatment = MATCHER.matchSingle("Amlodipin besylat");
        assertTrue(matchedTreatment.isPresent());
        assertEquals("Norvasc", matchedTreatment.get().name());
    }

    @Test
    public void matchesTermWithSpecialChars() throws IOException {
        final Optional<CuratedTreatment> matchedTreatment = MATCHER.matchSingle("z-pak");
        assertTrue(matchedTreatment.isPresent());
        assertEquals("Azithromycin", matchedTreatment.get().name());
    }

    @Test
    public void matchMultipleTreatments() throws IOException {
        final List<CuratedTreatment> matchedTreatments = MATCHER.matchMultiple("Prinivil,Zithromax/amlodipine besylate");
        assertEquals(3, matchedTreatments.size());
        final List<String> matches = matchedTreatments.stream().map(CuratedTreatment::name).collect(Collectors.toList());
        assertTrue(matches.contains("Lisinopril"));
        assertTrue(matches.contains("Norvasc"));
        assertTrue(matches.contains("Azithromycin"));
    }

    @Test
    public void matchMultipleTreatmentsWithTypos() throws IOException {
        final List<CuratedTreatment> matchedTreatments = MATCHER.matchMultiple("Prinivyl,Zithromaxx/amlodipin Besylate");
        assertEquals(3, matchedTreatments.size());
        final List<String> matches = matchedTreatments.stream().map(CuratedTreatment::name).collect(Collectors.toList());
        assertTrue(matches.contains("Lisinopril"));
        assertTrue(matches.contains("Norvasc"));
        assertTrue(matches.contains("Azithromycin"));
    }

    @Test
    public void matchMultipleSimilarTreatments() throws IOException {
        final List<CuratedTreatment> matchedTreatments = MATCHER.matchMultiple("amlodipine besylate, amlodipine acetate");
        assertEquals(2, matchedTreatments.size());
        final List<String> matches = matchedTreatments.stream().map(CuratedTreatment::name).collect(Collectors.toList());
        assertTrue(matches.contains("Norvasc"));
        assertTrue(matches.contains("Norvinopril"));
    }

    @Test
    public void doesNotMatchAmbiguousTerm() throws IOException {
        final List<CuratedTreatment> acidmatchedTreatments = MATCHER.search("acid");
        assertEquals(0, acidmatchedTreatments.size());
        final List<CuratedTreatment> amlodipineMatchedTreatments = MATCHER.search("amlodipine");
        assertEquals(0, amlodipineMatchedTreatments.size());
    }

    @Test
    public void doesNotMatchAmbiguousMultiTerm() throws IOException {
        final List<CuratedTreatment> matchedTreatments = MATCHER.search("pain therapy");
        assertEquals(0, matchedTreatments.size());
    }

    @Test
    public void doesNotMatchNonExistentComposedTerm() throws IOException {
        final List<CuratedTreatment> matchedTreatments = MATCHER.search("amlodipine phosphate");
        assertEquals(0, matchedTreatments.size());
    }

    @Test
    public void matchesTermWithAlias() throws IOException {
        final List<CuratedTreatment> matchedTreatments = MATCHER.search("Zocor (simvastatin)");
        assertEquals(2, matchedTreatments.size());
        assertEquals("Zocor", matchedTreatments.get(0).name());
        assertEquals("Zocor", matchedTreatments.get(1).name());
    }

    @Test
    public void matchesTermWithNumbers() throws IOException {
        final List<CuratedTreatment> matchedTreatments = MATCHER.search("TNT 101");
        assertEquals(1, matchedTreatments.size());
        assertEquals("TNT-101", matchedTreatments.get(0).name());
    }

    @Test
    public void doesNotMatchSingleOccurrenceOfAmbiguousTerm() throws IOException {
        final List<CuratedTreatment> matchedTreatments = MATCHER.search("acetate");
        assertEquals(0, matchedTreatments.size());
    }
}
