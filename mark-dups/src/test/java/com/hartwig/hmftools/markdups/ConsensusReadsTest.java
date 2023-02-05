package com.hartwig.hmftools.markdups;

import static java.lang.String.format;

import static com.hartwig.hmftools.common.test.GeneTestUtils.CHR_1;
import static com.hartwig.hmftools.common.test.MockRefGenome.generateRandomBases;
import static com.hartwig.hmftools.markdups.TestUtils.DEFAULT_QUAL;
import static com.hartwig.hmftools.markdups.TestUtils.REF_BASES;
import static com.hartwig.hmftools.markdups.TestUtils.REF_BASES_A;
import static com.hartwig.hmftools.markdups.TestUtils.REF_BASES_C;
import static com.hartwig.hmftools.markdups.TestUtils.REF_BASES_G;
import static com.hartwig.hmftools.markdups.TestUtils.REF_BASES_T;
import static com.hartwig.hmftools.markdups.TestUtils.setBaseQualities;
import static com.hartwig.hmftools.markdups.umi.ConsensusOutcome.ALIGNMENT_ONLY;
import static com.hartwig.hmftools.markdups.umi.ConsensusOutcome.INDEL_MATCH;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static htsjdk.samtools.CigarOperator.D;
import static htsjdk.samtools.CigarOperator.I;
import static htsjdk.samtools.CigarOperator.M;
import static htsjdk.samtools.CigarOperator.S;

import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.test.MockRefGenome;
import com.hartwig.hmftools.common.test.ReadIdGenerator;
import com.hartwig.hmftools.markdups.umi.ConsensusReadInfo;
import com.hartwig.hmftools.markdups.umi.ConsensusReads;
import com.hartwig.hmftools.markdups.umi.ReadParseState;
import com.hartwig.hmftools.markdups.umi.UmiConfig;

import org.junit.Test;

import htsjdk.samtools.SAMRecord;

public class ConsensusReadsTest
{
    private final MockRefGenome mRefGenome;
    private final UmiConfig mConfig;
    private final ConsensusReads mConsensusReads;
    private final ReadIdGenerator mReadIdGen;

    public static final String UMI_ID_1 = "TAGTAG";

    public ConsensusReadsTest()
    {
        mConfig = new UmiConfig(true);
        mRefGenome = new MockRefGenome();
        mRefGenome.RefGenomeMap.put(CHR_1, REF_BASES);
        mConsensusReads = new ConsensusReads(mConfig, mRefGenome);
        mReadIdGen = new ReadIdGenerator();
    }

    @Test
    public void testBasicConsensusReads()
    {
        final List<SAMRecord> reads = Lists.newArrayList();

        int posStart = 11;
        String consensusBases = REF_BASES.substring(posStart, 21);
        reads.add(createSamRecord(nextReadId(), posStart, consensusBases, "10M", false));
        reads.add(createSamRecord(nextReadId(), posStart, consensusBases, "10M", false));
        reads.add(createSamRecord(nextReadId(), posStart, consensusBases, "10M", false));

        ConsensusReadInfo readInfo = mConsensusReads.createConsensusRead(reads, UMI_ID_1);
        assertEquals(ALIGNMENT_ONLY, readInfo.Outcome);
        assertEquals(consensusBases, readInfo.ConsensusRead.getReadString());
        assertEquals("10M", readInfo.ConsensusRead.getCigarString());
        assertEquals(posStart, readInfo.ConsensusRead.getAlignmentStart());

        // mismatch at some of the bases and so determined by qual
        reads.clear();

        SAMRecord read1 = createSamRecord(nextReadId(), posStart, consensusBases, "10M", false);
        setBaseQualities(read1, DEFAULT_QUAL - 1);
        reads.add(read1);

        String bases2 = "AATAATAATA";
        SAMRecord read2 = createSamRecord(nextReadId(), posStart + 2, bases2, "2S8M", false);
        reads.add(read2);

        String bases3 = "AATAAGAAAA";
        SAMRecord read3 = createSamRecord(nextReadId(), posStart + 4, bases3, "4S6M", false);
        setBaseQualities(read3, DEFAULT_QUAL - 1);
        reads.add(read3);

        // 3rd base is 'T' due to max qual of 2nd and 3rd reads
        // 6th base is 'T' due to max qual of 2nd read
        // 9th base is 'A' due to max qual of 1st and 3rd reads

        readInfo = mConsensusReads.createConsensusRead(reads, UMI_ID_1);
        assertEquals(ALIGNMENT_ONLY, readInfo.Outcome);
        assertEquals(posStart, readInfo.ConsensusRead.getAlignmentStart());
        assertEquals("10M", readInfo.ConsensusRead.getCigarString());

        assertEquals("AATAATAAAA", readInfo.ConsensusRead.getReadString());
        assertEquals(19, readInfo.ConsensusRead.getBaseQualities()[2]);
        assertEquals(0, readInfo.ConsensusRead.getBaseQualities()[5]);
        assertEquals(18, readInfo.ConsensusRead.getBaseQualities()[8]);

        // test preference for reference base when quals are qual
        reads.clear();
        posStart = 16;
        read1 = createSamRecord(nextReadId(), posStart, REF_BASES_A, "10M", false);
        reads.add(read1);

        read2 = createSamRecord(nextReadId(), posStart, REF_BASES_C, "10M", false);
        reads.add(read2);

        readInfo = mConsensusReads.createConsensusRead(reads, UMI_ID_1);
        assertEquals("AAAAACCCCC", readInfo.ConsensusRead.getReadString());

        // soft-clips at both ends
        reads.clear();
        posStart = 11;
        read1 = createSamRecord(nextReadId(), posStart + 3, REF_BASES_A, "3S6M1S", false);
        reads.add(read1);

        read2 = createSamRecord(nextReadId(), posStart + 2, REF_BASES_A, "2S5M3S", false);
        reads.add(read2);

        readInfo = mConsensusReads.createConsensusRead(reads, UMI_ID_1);

        assertEquals(posStart + 2, readInfo.ConsensusRead.getAlignmentStart());
        assertEquals("2S7M1S", readInfo.ConsensusRead.getCigarString());
        assertEquals(REF_BASES_A, readInfo.ConsensusRead.getReadString());

        // longer reads at the non-fragment start end
        reads.clear();
        posStart = 11;
        read1 = createSamRecord(nextReadId(), posStart, REF_BASES_A, "8M2S", false);
        reads.add(read1);

        read2 = createSamRecord(nextReadId(), posStart, REF_BASES_A + "CCC", "7M6S", false);
        reads.add(read2);

        readInfo = mConsensusReads.createConsensusRead(reads, UMI_ID_1);

        assertEquals(posStart, readInfo.ConsensusRead.getAlignmentStart());
        assertEquals("8M5S", readInfo.ConsensusRead.getCigarString());
        assertEquals(REF_BASES_A + "CCC", readInfo.ConsensusRead.getReadString());

        // same again on the reverse strand
        reads.clear();
        posStart = 11;
        read1 = createSamRecord(nextReadId(), posStart + 2, REF_BASES_A, "2S6M2S", true);
        reads.add(read1);

        read2 = createSamRecord(nextReadId(), posStart, "CCCC" + REF_BASES_A, "4S7M3S", true);
        reads.add(read2);

        readInfo = mConsensusReads.createConsensusRead(reads, UMI_ID_1);

        assertEquals(posStart, readInfo.ConsensusRead.getAlignmentStart());
        assertEquals("4S8M2S", readInfo.ConsensusRead.getCigarString());
        assertEquals("CCCC" + REF_BASES_A, readInfo.ConsensusRead.getReadString());
    }

    @Test
    public void testMatchingIndelReads()
    {
        // indels with no CIGAR differences use the standard consensus routines
        final List<SAMRecord> reads = Lists.newArrayList();

        int posStart = 13;

        String consensusBases = REF_BASES_A.substring(0, 5) + "C" + REF_BASES_A.substring(5, 10);
        String indelCigar = "2S3M1I3M2S";
        SAMRecord read1 = createSamRecord(nextReadId(), posStart, consensusBases, indelCigar, false);
        reads.add(read1);

        SAMRecord read2 = createSamRecord(nextReadId(), posStart, consensusBases, indelCigar, false);
        reads.add(read2);

        SAMRecord read3 = createSamRecord(nextReadId(), posStart, consensusBases, indelCigar, false);
        reads.add(read3);

        ConsensusReadInfo readInfo = mConsensusReads.createConsensusRead(reads, UMI_ID_1);
        assertEquals(INDEL_MATCH, readInfo.Outcome);
        assertEquals(consensusBases, readInfo.ConsensusRead.getReadString());
        assertEquals(indelCigar, readInfo.ConsensusRead.getCigarString());
        assertEquals(posStart, readInfo.ConsensusRead.getAlignmentStart());
    }

    @Test
    public void testReadParseState()
    {
        String bases = "AGGCGGA";
        String indelCigar = "1S2M1I2M1S";

        SAMRecord read1 = createSamRecord(nextReadId(), 100, bases, indelCigar, false);

        ReadParseState readState = new ReadParseState(read1, true);
        assertEquals((byte)'A', readState.currentBase());
        assertEquals(DEFAULT_QUAL, readState.currentBaseQual());
        assertEquals(S, readState.elementType());
        assertEquals(1, readState.elementLength());

        readState.moveNext();
        assertEquals((byte)'G', readState.currentBase());
        assertEquals(M, readState.elementType());
        assertEquals(2, readState.elementLength());

        readState.moveNext();
        readState.moveNext();
        assertEquals((byte)'C', readState.currentBase());
        assertEquals(I, readState.elementType());
        assertEquals(1, readState.elementLength());

        readState.moveNext();
        readState.moveNext();
        assertFalse(readState.exhausted());
        assertEquals((byte)'G', readState.currentBase());
        assertEquals(M, readState.elementType());

        readState.moveNext();
        assertFalse(readState.exhausted());
        assertEquals((byte)'A', readState.currentBase());
        assertEquals(S, readState.elementType());

        readState.moveNext();
        assertTrue(readState.exhausted());

        // and in reverse
        readState = new ReadParseState(read1, false);
        assertEquals((byte)'A', readState.currentBase());
        assertEquals(DEFAULT_QUAL, readState.currentBaseQual());
        assertEquals(S, readState.elementType());
        assertEquals(1, readState.elementLength());

        readState.moveNext();
        readState.moveNext();
        readState.moveNext();

        assertEquals((byte)'C', readState.currentBase());
        assertEquals(I, readState.elementType());
        assertEquals(1, readState.elementLength());

        readState.moveNext();
        readState.moveNext();
        readState.moveNext();
        assertFalse(readState.exhausted());
        assertEquals((byte)'A', readState.currentBase());
        assertEquals(S, readState.elementType());

        readState.moveNext();
        assertTrue(readState.exhausted());

        // with a delete
        bases = "ACGT";
        indelCigar = "2M3D2M";

        read1 = createSamRecord(nextReadId(), 100, bases, indelCigar, false);

        readState = new ReadParseState(read1, true);

        assertEquals((byte)'A', readState.currentBase());
        assertEquals(M, readState.elementType());

        readState.moveNext();
        assertEquals((byte)'C', readState.currentBase());
        assertEquals(M, readState.elementType());

        readState.moveNext();
        assertEquals((byte)'C', readState.currentBase());
        assertEquals(D, readState.elementType());

        readState.moveNext();
        assertEquals((byte)'C', readState.currentBase());
        assertEquals(D, readState.elementType());

        readState.moveNext();
        assertEquals((byte)'C', readState.currentBase());
        assertEquals(D, readState.elementType());

        readState.moveNext();
        assertEquals((byte)'G', readState.currentBase());
        assertEquals(M, readState.elementType());

        readState.moveNext();
        assertEquals((byte)'T', readState.currentBase());
        assertEquals(M, readState.elementType());

        readState.moveNext();
        assertTrue(readState.exhausted());
    }

    private static SAMRecord createSamRecord(
            final String readId, int readStart, final String readBases, final String cigar, boolean isReversed)
    {
        return TestUtils.createSamRecord(
                readId, CHR_1, readStart, readBases, cigar, CHR_1, 5000, isReversed, false, null);
    }

    private String nextReadId() { return nextUmiReadId(UMI_ID_1, mReadIdGen); }

    public static String nextUmiReadId(final String umiId, final ReadIdGenerator readIdGen)
    {
        String readId = readIdGen.nextId();
        return format("ABCD:%s:%s", readId, umiId);
    }
}