package com.hartwig.hmftools.cider

import com.hartwig.hmftools.cider.TestUtils.createVDJ
import com.hartwig.hmftools.cider.layout.ReadLayout
import com.hartwig.hmftools.cider.layout.ReadLayoutBuilderTest
import com.hartwig.hmftools.cider.layout.TestLayoutRead
import htsjdk.samtools.SAMUtils
import org.apache.logging.log4j.Level
import org.eclipse.collections.api.factory.Lists
import org.junit.Before
import java.util.IdentityHashMap
import kotlin.test.*

// create a mock layout adaptor that just return anchor range that we give it
class MockVJReadLayoutAdaptor : IVJReadLayoutAdaptor
{
    val anchorRangeMap = IdentityHashMap<ReadLayout, IntRange>()

    /*
    override fun toReadCandidate(read: TestLayoutRead) : VJReadCandidate
    {
        // return VJReadCandidate(SAMRecord(null), )
    }
     */

    override fun getAnchorMatchMethod(layout: ReadLayout): VJReadCandidate.MatchMethod
    {
        return VJReadCandidate.MatchMethod.ALIGN
    }

    override fun getTemplateAnchorSequence(layout: ReadLayout) : String
    {
        return "TGACCC"
    }

    override fun getAnchorRange(vj: VJ, layout: ReadLayout) : IntRange?
    {
        return anchorRangeMap[layout]
    }
}

class VDJSequenceBuilderTest
{
    @Before
    fun setUp()
    {
        //org.apache.logging.log4j.core.config.Configurator.setRootLevel(Level.TRACE)
    }

    // build a VDJ from a layout with just V anchor, J anchor is found using blosum
    @Test
    fun testBuildVdjFromVLayout()
    {
        val mockVjReadLayoutAdaptor = MockVJReadLayoutAdaptor()
        val mockAnchorBlosumSearcher = MockAnchorBlosumSearcher()
        val vdjSeqBuilder = VDJSequenceBuilder(
            mockVjReadLayoutAdaptor, mockAnchorBlosumSearcher, MIN_BASE_QUALITY,
            CiderConstants.MIN_VJ_LAYOUT_JOIN_OVERLAP_BASES)

        // we create a layout that has the V anchor but not J anchor
        // 30 bases long, V anchor is position 3-10:
        // TGC-GAATACC-CACATCCTGAGAGTGTCAGA
        //    V anchor
        // We want to set the mock anchor blosum searcher to return J anchor at position 20-25:
        // TGC-GAATACC-CACATCCTGA-GAGTG-TCAGA
        //     |_____|            |___|
        //    V anchor(align)    J anchor(blosum)

        val seq = "TGC-GAATACC-CACATCCTGA-GAGTG-TCAGA".replace("-", "")
        val layout = TestUtils.createLayout(seq)

        mockVjReadLayoutAdaptor.anchorRangeMap[layout] = 3 until 10

        // create a blosum match of type IGHJ, which will match with IGHV
        mockAnchorBlosumSearcher.anchorBlosumMatch = AnchorBlosumMatch(20, 25, "CCCCC",
            Lists.immutable.of(TestUtils.ighJ1), 10)

        // now we should be able to build a VDJ sequence
        val vdjSeq = vdjSeqBuilder.tryCompleteLayoutWithBlosum(VJGeneType.IGHV, layout)

        assertNotNull(vdjSeq)
        assertNotNull(vdjSeq.vAnchor)
        assertNotNull(vdjSeq.jAnchor)
        assertEquals(VJGeneType.IGHV, vdjSeq.vAnchor!!.geneType)
        assertEquals(VJGeneType.IGHJ, vdjSeq.jAnchor!!.geneType)
        assertIs<VJAnchorByReadMatch>(vdjSeq.vAnchor)
        assertIs<VJAnchorByBlosum>(vdjSeq.jAnchor)
        assertEquals("GAATACC", vdjSeq.vAnchorSequence)
        assertEquals("GAGTG", vdjSeq.jAnchorSequence)
        assertEquals("CACATCCTGA", vdjSeq.cdr3SequenceShort)
    }

    // build a VDJ from a layout with just J anchor
    @Test
    fun testBuildVdjFromJLayout()
    {
        val mockVjReadLayoutAdaptor = MockVJReadLayoutAdaptor()
        val mockAnchorBlosumSearcher = MockAnchorBlosumSearcher()
        val vdjSeqBuilder = VDJSequenceBuilder(
            mockVjReadLayoutAdaptor, mockAnchorBlosumSearcher, MIN_BASE_QUALITY,
            CiderConstants.MIN_VJ_LAYOUT_JOIN_OVERLAP_BASES)

        // we create a layout that has the J anchor but not V anchor
        // 30 bases long, J anchor is position 20-25:
        // We want to set the mock anchor blosum searcher to return V anchor at position 3-10:
        // TGC-GAATACC-CACATCCTGA-GAGTG-TCAGA
        //     |_____|            |___|
        //    V anchor(blosum)    J anchor(align)

        val seq = "TGC-GAATACC-CACATCCTGA-GAGTG-TCAGA".replace("-", "")
        val layout = TestUtils.createLayout(seq)

        mockVjReadLayoutAdaptor.anchorRangeMap[layout] = 20 until 25

        // create a blosum match of type IGHV, which will match with IGHJ
        mockAnchorBlosumSearcher.anchorBlosumMatch = AnchorBlosumMatch(3, 10, "CCCCC",
            Lists.immutable.of(TestUtils.ighV1_18), 10)

        // now we should be able to build a VDJ sequence
        val vdjSeq = vdjSeqBuilder.tryCompleteLayoutWithBlosum(VJGeneType.IGHJ, layout)

        assertNotNull(vdjSeq)
        assertNotNull(vdjSeq.vAnchor)
        assertNotNull(vdjSeq.jAnchor)
        assertEquals(VJGeneType.IGHV, vdjSeq.vAnchor!!.geneType)
        assertEquals(VJGeneType.IGHJ, vdjSeq.jAnchor!!.geneType)
        assertIs<VJAnchorByBlosum>(vdjSeq.vAnchor)
        assertIs<VJAnchorByReadMatch>(vdjSeq.jAnchor)
        assertEquals("GAATACC", vdjSeq.vAnchorSequence)
        assertEquals("GAGTG", vdjSeq.jAnchorSequence)
        assertEquals("CACATCCTGA", vdjSeq.cdr3SequenceShort)
    }

    // test building VDJ sequence by merging overlapping V layout and J layout
    @Test
    fun testBuildVdjFromOverlapLayouts()
    {
        val mockVjReadLayoutAdaptor = MockVJReadLayoutAdaptor()
        val mockAnchorBlosumSearcher = MockAnchorBlosumSearcher()
        val vdjSeqBuilder = VDJSequenceBuilder(
            mockVjReadLayoutAdaptor, mockAnchorBlosumSearcher, MIN_BASE_QUALITY,
            10)

        // we create two layouts, a V layout and a J layout, and they overlap each other by 13 bases:
        // v layout:   TGC-GAATACC-CACATCCTGA-G
        // j layout:            CC-CACATCCTGA-GAGTG-TCAGA
        //                 |_____|            |___|
        //            V anchor(align)    J anchor(align)
        //
        // V anchor is position 3-10 in the V layout, J anchor is position 12-17 in the J layout.
        // The final VDJ is 30 bases long, V anchor at position 3-10, J anchor is position 20-25

        val seq = "TGC-GAATACC-CACATCCTGA-GAGTG-TCAGA".replace("-", "")
        val vLayout = TestUtils.createLayout(seq.substring(0, 21))
        val jLayout = TestUtils.createLayout(seq.substring(8))

        mockVjReadLayoutAdaptor.anchorRangeMap[vLayout] = 3 until 10
        mockVjReadLayoutAdaptor.anchorRangeMap[jLayout] = 12 until 17

        // now we should be able to build a VDJ sequence
        val vdjSeq = vdjSeqBuilder.tryOverlapVJ(vLayout, jLayout, VJGeneType.TRAV, VJGeneType.TRAJ)

        assertNotNull(vdjSeq)
        assertNotNull(vdjSeq.vAnchor)
        assertNotNull(vdjSeq.jAnchor)
        assertEquals(VJGeneType.TRAV, vdjSeq.vAnchor!!.geneType)
        assertEquals(VJGeneType.TRAJ, vdjSeq.jAnchor!!.geneType)
        assertIs<VJAnchorByReadMatch>(vdjSeq.vAnchor)
        assertIs<VJAnchorByReadMatch>(vdjSeq.jAnchor)
        assertEquals("GAATACC", vdjSeq.vAnchorSequence)
        assertEquals("GAGTG", vdjSeq.jAnchorSequence)
        assertEquals("CACATCCTGA", vdjSeq.cdr3SequenceShort)
    }

    // building of overlapping layout where the J layout is on the upstream
    // of V layout
    @Test
    fun testBuildVdjFromOverlapLayouts1()
    {
        val mockVjReadLayoutAdaptor = MockVJReadLayoutAdaptor()
        val mockAnchorBlosumSearcher = MockAnchorBlosumSearcher()
        val vdjSeqBuilder = VDJSequenceBuilder(
            mockVjReadLayoutAdaptor, mockAnchorBlosumSearcher, MIN_BASE_QUALITY,
            10)

        // we create two layouts, a V layout and a J layout, and they overlap each other by 25 bases,
        // and the J layout is actually on the "left" of V layout
        // v layout:    GC-GAATACC-CACATCCTGA-GAGTG-TCAGA
        // j layout: GATGC-GAATACC-CACATCCTGA-GAGTG-T
        //                 |_____|            |___|
        //            V anchor(align)    J anchor(align)
        //
        // V anchor is position 2-9 in the V layout, J anchor is position 22-27 in the J layout.
        // The final VDJ is 32 bases long, V anchor at position 5-12, J anchor is position 22-27

        val seq = "GATGC-GAATACC-CACATCCTGA-GAGTG-TCAGA".replace("-", "")
        val vLayout = TestUtils.createLayout(seq.substring(3))
        val jLayout = TestUtils.createLayout(seq.take(28))

        mockVjReadLayoutAdaptor.anchorRangeMap[vLayout] = 2 until 9
        mockVjReadLayoutAdaptor.anchorRangeMap[jLayout] = 22 until 27

        // now we should be able to build a VDJ sequence
        val vdjSeq = vdjSeqBuilder.tryOverlapVJ(vLayout, jLayout, VJGeneType.TRAV, VJGeneType.TRAJ)

        assertNotNull(vdjSeq)
        assertNotNull(vdjSeq.vAnchor)
        assertNotNull(vdjSeq.jAnchor)
        assertEquals(seq, vdjSeq.layout.consensusSequence())
        assertEquals(VJGeneType.TRAV, vdjSeq.vAnchor!!.geneType)
        assertEquals(VJGeneType.TRAJ, vdjSeq.jAnchor!!.geneType)
        assertIs<VJAnchorByReadMatch>(vdjSeq.vAnchor)
        assertIs<VJAnchorByReadMatch>(vdjSeq.jAnchor)
        assertEquals("GAATACC", vdjSeq.vAnchorSequence)
        assertEquals("GAGTG", vdjSeq.jAnchorSequence)
        assertEquals("CACATCCTGA", vdjSeq.cdr3SequenceShort)
    }

    // building of overlapping layout where the V layout contains J layout
    @Test
    fun testBuildVdjFromOverlapLayouts3()
    {
        val mockVjReadLayoutAdaptor = MockVJReadLayoutAdaptor()
        val mockAnchorBlosumSearcher = MockAnchorBlosumSearcher()
        val vdjSeqBuilder = VDJSequenceBuilder(
            mockVjReadLayoutAdaptor, mockAnchorBlosumSearcher, MIN_BASE_QUALITY,
            10)

        // we create two layouts, a V layout and a J layout, and they overlap each other by 25 bases,
        // and the J layout is actually on the "left" of V layout
        // v layout: GATGC-GAATACC-CACATCCTGA-GAGTG-TCAGA
        // j layout:         ATACC-CACATCCTGA-GAGTG-T
        //                 |_____|            |___|
        //            V anchor(align)    J anchor(align)
        //
        // V anchor is position 2-9 in the V layout, J anchor is position 22-27 in the J layout.
        // The final VDJ is 32 bases long, V anchor at position 5-12, J anchor is position 22-27

        val seq = "GATGC-GAATACC-CACATCCTGA-GAGTG-TCAGA".replace("-", "")
        val vLayout = TestUtils.createLayout(seq)
        val jLayout = TestUtils.createLayout(seq.substring(7, 28))

        mockVjReadLayoutAdaptor.anchorRangeMap[vLayout] = 5 until 12
        mockVjReadLayoutAdaptor.anchorRangeMap[jLayout] = 15 until 20

        // now we should be able to build a VDJ sequence
        val vdjSeq = vdjSeqBuilder.tryOverlapVJ(vLayout, jLayout, VJGeneType.TRAV, VJGeneType.TRAJ)

        assertNotNull(vdjSeq)
        assertNotNull(vdjSeq.vAnchor)
        assertNotNull(vdjSeq.jAnchor)
        assertEquals(seq, vdjSeq.layout.consensusSequence())
        assertEquals(VJGeneType.TRAV, vdjSeq.vAnchor!!.geneType)
        assertEquals(VJGeneType.TRAJ, vdjSeq.jAnchor!!.geneType)
        assertIs<VJAnchorByReadMatch>(vdjSeq.vAnchor)
        assertIs<VJAnchorByReadMatch>(vdjSeq.jAnchor)
        assertEquals("GAATACC", vdjSeq.vAnchorSequence)
        assertEquals("GAGTG", vdjSeq.jAnchorSequence)
        assertEquals("CACATCCTGA", vdjSeq.cdr3SequenceShort)
    }

    // building of overlapping layout where the J layout contains V layout
    @Test
    fun testBuildVdjFromOverlapLayouts4()
    {
        val mockVjReadLayoutAdaptor = MockVJReadLayoutAdaptor()
        val mockAnchorBlosumSearcher = MockAnchorBlosumSearcher()
        val vdjSeqBuilder = VDJSequenceBuilder(
            mockVjReadLayoutAdaptor, mockAnchorBlosumSearcher, MIN_BASE_QUALITY,
            10)

        // we create two layouts, a V layout and a J layout, and they overlap each other by 25 bases,
        // and the J layout is actually on the "left" of V layout
        // v layout:     C-GAATACC-CACATCCTGA-GAG
        // j layout: GATGC-GAATACC-CACATCCTGA-GAGTG-TCAGA
        //                 |_____|            |___|
        //            V anchor(align)    J anchor(align)
        //
        // V anchor is position 2-9 in the V layout, J anchor is position 22-27 in the J layout.
        // The final VDJ is 32 bases long, V anchor at position 5-12, J anchor is position 22-27

        val seq = "GATGC-GAATACC-CACATCCTGA-GAGTG-TCAGA".replace("-", "")
        val vLayout = TestUtils.createLayout(seq.substring(4, 25))
        val jLayout = TestUtils.createLayout(seq)

        mockVjReadLayoutAdaptor.anchorRangeMap[vLayout] = 1 until 8
        mockVjReadLayoutAdaptor.anchorRangeMap[jLayout] = 22 until 27

        // now we should be able to build a VDJ sequence
        val vdjSeq = vdjSeqBuilder.tryOverlapVJ(vLayout, jLayout, VJGeneType.TRAV, VJGeneType.TRAJ)

        assertNotNull(vdjSeq)
        assertNotNull(vdjSeq.vAnchor)
        assertNotNull(vdjSeq.jAnchor)
        assertEquals(seq, vdjSeq.layout.consensusSequence())
        assertEquals(VJGeneType.TRAV, vdjSeq.vAnchor!!.geneType)
        assertEquals(VJGeneType.TRAJ, vdjSeq.jAnchor!!.geneType)
        assertIs<VJAnchorByReadMatch>(vdjSeq.vAnchor)
        assertIs<VJAnchorByReadMatch>(vdjSeq.jAnchor)
        assertEquals("GAATACC", vdjSeq.vAnchorSequence)
        assertEquals("GAGTG", vdjSeq.jAnchorSequence)
        assertEquals("CACATCCTGA", vdjSeq.cdr3SequenceShort)
    }

    // build one sided V only VDJ
    @Test
    fun testBuildOneSidedVdjVOnly()
    {
        val mockVjReadLayoutAdaptor = MockVJReadLayoutAdaptor()
        val mockAnchorBlosumSearcher = MockAnchorBlosumSearcher()
        val vdjSeqBuilder = VDJSequenceBuilder(
            mockVjReadLayoutAdaptor, mockAnchorBlosumSearcher, MIN_BASE_QUALITY,
            CiderConstants.MIN_VJ_LAYOUT_JOIN_OVERLAP_BASES)

        // we create a layout that has the V anchor but not J anchor
        // 30 bases long, V anchor is position 3-10:
        // TGC-GAATACC-CACATCCTGAGAGTGTCAGA
        //    V anchor

        val seq = "TGC-GAATACC-CACATCCTGAGAGTGTCAGA".replace("-", "")
        val layout = TestUtils.createLayout(seq)

        mockVjReadLayoutAdaptor.anchorRangeMap[layout] = 3 until 10

        // create one sided v only VDJ
        val vdjSeq = vdjSeqBuilder.tryCreateOneSidedVdj(VJGeneType.IGHV, layout)

        assertNotNull(vdjSeq)
        assertNotNull(vdjSeq.vAnchor)
        assertNull(vdjSeq.jAnchor)
        assertEquals(VJGeneType.IGHV, vdjSeq.vAnchor!!.geneType)
        assertIs<VJAnchorByReadMatch>(vdjSeq.vAnchor)
        assertEquals("GAATACC-CACATCCTGAGAGTGTCAGA", vdjSeq.sequenceFormatted)
        assertEquals("GAATACC", vdjSeq.vAnchorSequence)
        assertEquals("CACATCCTGAGAGTGTCAGA", vdjSeq.cdr3SequenceShort)
    }

    // build one sided J only VDJ
    @Test
    fun testBuildOneSidedVdjJOnly()
    {
        val mockVjReadLayoutAdaptor = MockVJReadLayoutAdaptor()
        val mockAnchorBlosumSearcher = MockAnchorBlosumSearcher()
        val vdjSeqBuilder = VDJSequenceBuilder(
            mockVjReadLayoutAdaptor, mockAnchorBlosumSearcher, MIN_BASE_QUALITY,
            CiderConstants.MIN_VJ_LAYOUT_JOIN_OVERLAP_BASES)

        // we create a layout that has J anchor but no V
        // TGCGAATACCCACAT-CTGAGTG-TCAGA
        //                J anchor

        val seq = "TGCGAATACCCACAT-CTGAGTG-TCAGA".replace("-", "")
        val layout = TestUtils.createLayout(seq)

        mockVjReadLayoutAdaptor.anchorRangeMap[layout] = 15 until 22

        // create one sided j only VDJ
        val vdjSeq = vdjSeqBuilder.tryCreateOneSidedVdj(VJGeneType.IGHJ, layout)

        assertNotNull(vdjSeq)
        assertNull(vdjSeq.vAnchor)
        assertNotNull(vdjSeq.jAnchor)
        assertEquals(VJGeneType.IGHJ, vdjSeq.jAnchor!!.geneType)
        assertIs<VJAnchorByReadMatch>(vdjSeq.jAnchor)
        assertEquals("TGCGAATACCCACAT-CTGAGTG", vdjSeq.sequenceFormatted)
        assertEquals("CTGAGTG", vdjSeq.jAnchorSequence)
        assertEquals("TGCGAATACCCACAT", vdjSeq.cdr3SequenceShort)
    }

    companion object
    {
        const val MIN_BASE_QUALITY = 30.toByte()

        /*
        // a simple function to create VDJ sequence
        fun createVDJ1(sequence: String, support: String, vAnchorBoundary: Int, jAnchorBoundary: Int, readIdPrefix: String) : VDJSequence
        {
            assertEquals(sequence.length, support.length)
            assertTrue(vAnchorBoundary < jAnchorBoundary)
            assertTrue(vAnchorBoundary < sequence.length)
            assertTrue(jAnchorBoundary <= sequence.length)

            val vdjSupport = ArrayList<VDJSequence.BaseSupport>()

            // we want to somehow intelligently create some reads to get the given support
            for (i in sequence.indices)
            {
                vdjSupport.add(VDJSequence.BaseSupport(sequence[i], createReadSet(support[i], readIdPrefix)))
            }

            // create VJ anchor
            val vAnchor = VJAnchorByReadMatch(
                type = VJAnchor.Type.V,
                geneType = VJGeneType.IGHV,
                anchorBoundary = vAnchorBoundary,
                matchMethod = "exact"
            )

            val jAnchor = VJAnchorByReadMatch(
                type = VJAnchor.Type.J,
                geneType = VJGeneType.IGHJ,
                anchorBoundary = jAnchorBoundary,
                matchMethod = "exact"
            )

            val vdj = VDJSequence("id", vdjSupport.toTypedArray(), vAnchor, jAnchor)
            return vdj
        }

        fun createReadSet(sizeCode: Char, readIdPrefix: String) : Set<ReadKey>
        {
            val readSet = HashSet<ReadKey>()

            // convert the size Code to int
            val size = sizeCode.digitToInt(radix = RADIX)
            repeat(size, { i -> readSet.add(ReadKey("${readIdPrefix}${i}", true)) })
            return readSet
        }*/
    }
}