package com.hartwig.hmftools.linx.chaining;

import static com.hartwig.hmftools.linx.types.LinkType.DELETION_BRIDGE;
import static com.hartwig.hmftools.linx.types.LinkType.TEMPLATED_INSERTION;
import static com.hartwig.hmftools.linx.utils.SvTestUtils.createDel;
import static com.hartwig.hmftools.linx.utils.SvTestUtils.createDup;

import static org.junit.Assert.assertEquals;

import com.hartwig.hmftools.linx.types.SvLinkedPair;
import com.hartwig.hmftools.linx.types.SvVarData;

import org.junit.Test;

public class LinkingTest
{
    @Test
    public void testLinkedPairs()
    {
        // test linked pair switching
        final SvVarData var1 = createDup(1, "1", 100, 200);
        final SvVarData var2 = createDup(2, "1", 300, 400);
        SvLinkedPair lp1 = SvLinkedPair.from(var1, var2, TEMPLATED_INSERTION, false, true);

        assertEquals(lp1.first(), var1);
        assertEquals(lp1.second(), var2);
        assertEquals(lp1.firstLinkOnStart(), false);
        assertEquals(lp1.secondLinkOnStart(), true);
        assertEquals(lp1.firstUnlinkedOnStart(), true);
        assertEquals(lp1.secondUnlinkedOnStart(), false);

        lp1.switchSVs();
        assertEquals(lp1.first(), var2);
        assertEquals(lp1.second(), var1);
        assertEquals(lp1.firstLinkOnStart(), true);
        assertEquals(lp1.secondLinkOnStart(), false);

        // test short TIs converted to DBs
        final SvVarData var3 = createDel(3, "1", 100, 200);
        final SvVarData var4 = createDel(4, "1", 210, 400);
        SvLinkedPair lp2 = SvLinkedPair.from(var3, var4, TEMPLATED_INSERTION, false, true);
        assertEquals(lp2.linkType(), DELETION_BRIDGE);
        assertEquals(lp2.length(), -11);

        final SvVarData var5 = createDel(4, "1", 250, 400);

        SvLinkedPair lp3 = SvLinkedPair.from(var3, var5, TEMPLATED_INSERTION, false, true);

        lp3.sameVariants(lp2);
        lp3.hasLinkClash(lp2);
    }

}
