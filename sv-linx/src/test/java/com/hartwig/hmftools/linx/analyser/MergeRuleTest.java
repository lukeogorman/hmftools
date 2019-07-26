package com.hartwig.hmftools.linx.analyser;

import static com.hartwig.hmftools.linx.analyser.SvTestHelper.createBnd;
import static com.hartwig.hmftools.linx.analyser.SvTestHelper.createDel;
import static com.hartwig.hmftools.linx.analyser.SvTestHelper.createDup;
import static com.hartwig.hmftools.linx.analyser.SvTestHelper.createIns;
import static com.hartwig.hmftools.linx.analyser.SvTestHelper.createInv;
import static com.hartwig.hmftools.linx.analyser.SvTestHelper.createSgl;
import static com.hartwig.hmftools.linx.analysis.SvClusteringMethods.CLUSTER_REASON_FOLDBACKS;
import static com.hartwig.hmftools.linx.analysis.SvClusteringMethods.CLUSTER_REASON_HOM_LOSS;
import static com.hartwig.hmftools.linx.analysis.SvClusteringMethods.CLUSTER_REASON_LOH_CHAIN;
import static com.hartwig.hmftools.linx.analysis.SvClusteringMethods.CLUSTER_REASON_LOOSE_OVERLAP;
import static com.hartwig.hmftools.linx.types.SvVarData.ASSEMBLY_TYPE_EQV;

import static org.junit.Assert.assertEquals;

import static junit.framework.TestCase.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.linx.cn.HomLossEvent;
import com.hartwig.hmftools.linx.types.ResolvedType;
import com.hartwig.hmftools.linx.types.SvCluster;
import com.hartwig.hmftools.linx.cn.LohEvent;
import com.hartwig.hmftools.linx.types.SvVarData;

import org.junit.Test;

public class MergeRuleTest
{
    @Test
    public void testProximityMerge()
    {
        SvTestHelper tester = new SvTestHelper();

        // basic proximity clustering with 2 duplicate breakend SGLs excluded
        SvVarData var1 = createDel(tester.nextVarId(), "1", 1000, 1100);
        tester.AllVariants.add(var1);

        SvVarData var2 = createDup(tester.nextVarId(), "1", 2000, 2100);
        tester.AllVariants.add(var2);

        SvVarData var3 = createIns(tester.nextVarId(), "1", 3000, 3100);
        tester.AllVariants.add(var3);

        SvVarData var4 = createInv(tester.nextVarId(), "1", 4000, 4100, 1);
        tester.AllVariants.add(var4);

        SvVarData var5 = createSgl(tester.nextVarId(), "1", 5000, -1, false);
        tester.AllVariants.add(var5);

        SvVarData var6 = createBnd(tester.nextVarId(), "1", 6000, -1, "5", 1000, 1);
        tester.AllVariants.add(var6);

        // equivalent breakends are kept separate
        SvVarData var7 = createSgl(tester.nextVarId(), "1", 7000, -1, false);
        tester.AllVariants.add(var7);

        SvVarData var8 = createSgl(tester.nextVarId(), "1", 7000, -1, false);
        tester.AllVariants.add(var8);

        SvVarData var9 = createSgl(tester.nextVarId(), "1", 7100, -1, false);
        var9.setAssemblyData(true, ASSEMBLY_TYPE_EQV);
        tester.AllVariants.add(var9);

        // and some variants on the BND's other chromosome, and other linking BNDs
        SvVarData var10 = createIns(tester.nextVarId(), "5", 2000, 20000);
        tester.AllVariants.add(var10);

        SvVarData var11 = createBnd(tester.nextVarId(), "2", 6000, -1, "5", 21000, 1);
        tester.AllVariants.add(var11);

        SvVarData var12 = createIns(tester.nextVarId(), "2", 7000, 8000);
        tester.AllVariants.add(var12);

        SvVarData var13 = createBnd(tester.nextVarId(), "3", 6000, -1, "5", 22000, 1);
        tester.AllVariants.add(var13);

        tester.preClusteringInit();
        tester.Analyser.clusterAndAnalyse();

        assertEquals(3, tester.getClusters().size());

        SvCluster cluster = tester.findClusterWithSVs(Lists.newArrayList(var7));
        assertTrue(cluster != null);
        assertEquals(ResolvedType.DUP_BE, cluster.getResolvedType());

        cluster = tester.findClusterWithSVs(Lists.newArrayList(var9));
        assertTrue(cluster != null);
        assertEquals(ResolvedType.DUP_BE, cluster.getResolvedType());

        assertTrue(tester.hasClusterWithSVs(Lists.newArrayList(var1, var2, var3, var4, var5, var6, var8, var10,
                var11, var12, var13)));

        // simple clustered SVs are split out in the final routine
        tester.clearClustersAndSVs();

        SvVarData del1 = createDel(tester.nextVarId(), "2", 1000, 3000);
        tester.AllVariants.add(del1);

        SvVarData del2 = createDel(tester.nextVarId(), "2", 2000, 4000);
        tester.AllVariants.add(del2);

        SvVarData del3 = createDel(tester.nextVarId(), "2", 3500, 8000);
        tester.AllVariants.add(del3);

        tester.preClusteringInit();
        tester.Analyser.clusterAndAnalyse();

        assertEquals(3, tester.getClusters().size());
    }

    @Test
    public void testFoldbackMerge()
    {
        SvTestHelper tester = new SvTestHelper();

        // 2 clusters with foldbacks on the same arm are merged
        SvVarData inv1 = createInv(tester.nextVarId(), "1", 100, 200, -1);
        tester.AllVariants.add(inv1);

        SvVarData inv2 = createInv(tester.nextVarId(), "1", 20000, 20100, 1);
        tester.AllVariants.add(inv2);

        tester.preClusteringInit();

        tester.Analyser.clusterAndAnalyse();

        assertEquals(1, tester.getClusters().size());
        assertTrue(inv1.getClusterReason().contains(CLUSTER_REASON_FOLDBACKS));
        assertTrue(inv2.getClusterReason().contains(CLUSTER_REASON_FOLDBACKS));

        // non-overlapping DELs can merge, whereas overlapping DELs are split out
        tester.clearClustersAndSVs();

        // test again with a foldback and an opposing SV as the next breakend
        tester.AllVariants.add(inv2);

        // the DEL is resolved and will be ignored
        SvVarData del = createDel(tester.nextVarId(), "1", 10000, 10100);
        tester.AllVariants.add(del);

        SvVarData sgl = createSgl(tester.nextVarId(), "1", 1000, -1, false);
        tester.AllVariants.add(sgl);

        tester.preClusteringInit();

        tester.Analyser.clusterAndAnalyse();

        assertEquals(tester.getClusters().size(), 2);
        assertTrue(tester.getClusters().get(0).getSVs().contains(inv2));
        assertTrue(tester.getClusters().get(0).getSVs().contains(sgl));
        assertTrue(tester.getClusters().get(1).getSVs().contains(del));

        assertTrue(inv1.getClusterReason().contains(CLUSTER_REASON_FOLDBACKS));
        assertTrue(inv2.getClusterReason().contains(CLUSTER_REASON_FOLDBACKS));

        tester.clearClustersAndSVs();

        // test with a single foldback facing the centromere
        SvVarData inv3 = createInv(tester.nextVarId(), "2", 10000, 10100, -1);
        tester.AllVariants.add(inv3);

        SvVarData sgl2 = createSgl(tester.nextVarId(), "2", 20000, 1, false);
        tester.AllVariants.add(sgl2);

        // next is too far away
        SvVarData sgl3 = createSgl(tester.nextVarId(), "2", 10000000, 1, false);
        tester.AllVariants.add(sgl3);

        tester.preClusteringInit();

        tester.Analyser.clusterAndAnalyse();

        assertEquals(tester.getClusters().size(), 2);
        assertTrue(tester.getClusters().get(0).getSVs().contains(inv3));
        assertTrue(tester.getClusters().get(0).getSVs().contains(sgl2));
        assertTrue(tester.getClusters().get(1).getSVs().contains(sgl3));

        assertTrue(inv3.getClusterReason().contains(CLUSTER_REASON_FOLDBACKS));
        assertTrue(sgl2.getClusterReason().contains(CLUSTER_REASON_FOLDBACKS));
    }

    @Test
    public void testLohHomLossEventMerge()
    {
        SvTestHelper tester = new SvTestHelper();

        // scenario 1: LOH containing all clustered HOM-loss events should also be clustered
        SvVarData var1 = createBnd("1", "1", 1000, 1, "2", 100, 1);
        SvVarData var2 = createBnd("2", "1", 100000, -1, "3", 100, 1);

        // 2x hom-loss events both clustered
        SvVarData var3 = createDel("3", "1", 6500, 6600);
        SvVarData var4 = createBnd("4", "1", 20000, 1, "5", 200, 1);
        SvVarData var5 = createBnd("5", "1", 22000, -1, "5", 100, -1);

        List<LohEvent> lohData = tester.CnDataLoader.getLohData();

        LohEvent lohEvent = new LohEvent("1", 1000, 100000, "BND", "BND",
                2, 1, 1, 1, 1, 99000, var1.dbId(), var2.dbId());

        lohData.add(lohEvent);

        List<HomLossEvent> homLossData = tester.CnDataLoader.getHomLossData();

        homLossData.add(new HomLossEvent(var3.chromosome(true), var3.position(true), var3.position(false),
                var3.typeStr(), var3.typeStr(), var3.dbId(), var3.dbId()));

        homLossData.add(new HomLossEvent(var4.chromosome(true), var4.position(true), var5.position(true),
                var4.typeStr(), var5.typeStr(), var4.dbId(), var5.dbId()));

        lohEvent.addHomLossEvents(homLossData);

        tester.AllVariants.add(var1);
        tester.AllVariants.add(var2);
        tester.AllVariants.add(var3);
        tester.AllVariants.add(var4);
        tester.AllVariants.add(var5);
        tester.preClusteringInit();

        tester.Analyser.clusterAndAnalyse();

        assertEquals(3, tester.Analyser.getClusters().size());

        SvCluster cluster = tester.findClusterWithSVs(Lists.newArrayList(var1, var2));
        assertTrue(cluster != null);
        assertTrue(cluster.getClusteringReasons().contains(CLUSTER_REASON_HOM_LOSS));

        // scenario 2: multiple hom-loss events clustered because LOH is clustered
        tester.clearClustersAndSVs();

        var1 = createDel("1", "1", 1000, 100000);
        var2 = createBnd("2", "1", 10000, 1, "2", 100, 1);
        var3 = createBnd("3", "1", 20000, -1, "3", 100, 1);
        var4 = createBnd("4", "1", 30000, 1, "4", 100, 1);
        var5 = createBnd("5", "1", 40000, -1, "5", 100, 1);

        lohData.clear();

        lohEvent = new LohEvent(var1.chromosome(true), var1.position(true), var1.position(false),
                "DEL", "DEL", 2, 1, 1, 1, 1, 99000, var1.dbId(), var1.dbId());

        lohData.add(lohEvent);

        homLossData.clear();

        homLossData.add(new HomLossEvent(var2.chromosome(true), var2.position(true), var3.position(true),
                var2.typeStr(), var3.typeStr(), var2.dbId(), var3.dbId()));

        homLossData.add(new HomLossEvent(var4.chromosome(true), var4.position(true), var5.position(true),
                var4.typeStr(), var5.typeStr(), var4.dbId(), var5.dbId()));

        lohEvent.addHomLossEvents(homLossData);

        tester.AllVariants.add(var1);
        tester.AllVariants.add(var2);
        tester.AllVariants.add(var3);
        tester.AllVariants.add(var4);
        tester.AllVariants.add(var5);
        tester.preClusteringInit();

        tester.Analyser.clusterAndAnalyse();

        assertEquals(3, tester.Analyser.getClusters().size());

        cluster = tester.findClusterWithSVs(Lists.newArrayList(var2, var3));
        assertTrue(cluster != null);
        assertTrue(cluster.getClusteringReasons().contains(CLUSTER_REASON_HOM_LOSS));

        cluster = tester.findClusterWithSVs(Lists.newArrayList(var4, var5));
        assertTrue(cluster != null);
        assertTrue(cluster.getClusteringReasons().contains(CLUSTER_REASON_HOM_LOSS));


        // scenario 3: hom-loss event overlaps a LOH
        tester.clearClustersAndSVs();

        var1 = createDel("1", "1", 10000, 30000);
        var2 = createBnd("2", "1", 20000, 1, "2", 100, 1);
        var3 = createBnd("3", "1", 40000, -1, "3", 100, 1);

        lohData.clear();

        lohEvent = new LohEvent(var1.chromosome(true), var1.position(true), var3.position(true),
                var1.typeStr(), var3.typeStr(), 2, 1, 1, 1, 1, 20000, var1.dbId(), var3.dbId());

        lohData.add(lohEvent);

        homLossData.clear();

        homLossData.add(new HomLossEvent(var2.chromosome(true), var2.position(true), var1.position(false),
                var2.typeStr(), var1.typeStr(), var2.dbId(), var1.dbId()));

        lohEvent.addHomLossEvents(homLossData);

        tester.AllVariants.add(var1);
        tester.AllVariants.add(var2);
        tester.AllVariants.add(var3);
        tester.preClusteringInit();

        tester.Analyser.clusterAndAnalyse();

        cluster = tester.findClusterWithSVs(Lists.newArrayList(var2, var3));
        assertTrue(cluster != null);
        assertTrue(cluster.getClusteringReasons().contains(CLUSTER_REASON_HOM_LOSS));

        // again but with more SVs involved
        tester.clearClustersAndSVs();

        var1 = createDel("1", "1", 10000, 30000);
        var2 = createDel("2", "1", 40000, 60000);
        var3 = createBnd("3", "1", 20000, 1, "2", 100, 1);
        var4 = createBnd("4", "1", 50000, -1, "3", 100, 1);

        lohData.clear();

        lohEvent = new LohEvent(var1.chromosome(true), var1.position(true), var2.position(false),
                var1.typeStr(), var2.typeStr(), 2, 1, 1, 1, 1, 20000, var1.dbId(), var2.dbId());

        lohData.add(lohEvent);

        homLossData.clear();

        homLossData.add(new HomLossEvent(var2.chromosome(true), var3.position(true), var1.position(false),
                var3.typeStr(), var1.typeStr(), var3.dbId(), var1.dbId()));

        homLossData.add(new HomLossEvent(var2.chromosome(true), var2.position(true), var4.position(true),
                var2.typeStr(), var4.typeStr(), var2.dbId(), var4.dbId()));

        lohEvent.addHomLossEvents(homLossData);

        tester.AllVariants.add(var1);
        tester.AllVariants.add(var2);
        tester.AllVariants.add(var3);
        tester.AllVariants.add(var4);
        tester.preClusteringInit();

        tester.Analyser.clusterAndAnalyse();

        cluster = tester.findClusterWithSVs(Lists.newArrayList(var3, var4));
        assertTrue(cluster != null);
        assertTrue(cluster.getClusteringReasons().contains(CLUSTER_REASON_HOM_LOSS));



    }

    @Test
    public void testLohResolvingClusterMerge()
    {
        // merge clusters based on their SVs being required to stop a SV chaining through an LOH event
        SvTestHelper tester = new SvTestHelper();

        SvVarData var1 = createDel("1", "1", 10000, 60000);

        SvVarData var2 = createDup("2", "1", 20000, 50000);

        SvVarData var3 = createBnd("3", "1", 30000, 1, "2", 10000, 1);

        SvVarData var4 = createBnd("4", "1", 40000, -1, "2", 20000, 1);

        SvVarData var5 = createDel("5", "1", 70000, 120000);

        SvVarData var6 = createDup("6", "1", 80000, 110000);

        SvVarData var7 = createBnd("7", "1", 90000, 1, "2", 80000, 1);

        SvVarData var8 = createBnd("8", "1", 100000, -1, "2", 90000, -1);

        SvVarData var9 = createDup("9", "2", 30000, 60000);

        // does run unto the DUP in the LOH but isn't clustered since is simple
        SvVarData var10 = createDel("10", "2", 40000, 50000);

        tester.AllVariants.add(var1);
        tester.AllVariants.add(var2);
        tester.AllVariants.add(var3);
        tester.AllVariants.add(var4);
        tester.AllVariants.add(var5);
        tester.AllVariants.add(var6);
        tester.AllVariants.add(var7);
        tester.AllVariants.add(var8);
        tester.AllVariants.add(var9);
        tester.AllVariants.add(var10);

        List<LohEvent> lohData = tester.CnDataLoader.getLohData();

        lohData.add(new LohEvent("1", 10000, 20000,
                "DEL", "DUP", 1, 1, 1, 0, 1, 10000,
                var1.dbId(), var2.dbId()));

        lohData.add(new LohEvent("1", 50000, 60000,
                "DUP", "DEL", 1, 1, 1, 0, 1, 19000,
                var2.dbId(), var1.dbId()));

        lohData.add(new LohEvent("1", 110000, 120000,
                "DUP", "DEL", 1, 1, 1, 0, 1, 10000,
                var6.dbId(), var5.dbId()));

        lohData.add(new LohEvent("2", 20000, 30000,
                "BND", "DUP", 1, 1, 1, 0, 1, 10000,
                var4.dbId(), var9.dbId()));

        tester.preClusteringInit();

        tester.Analyser.clusterAndAnalyse();

        assertEquals(2, tester.Analyser.getClusters().size());
        assertTrue(var10.getCluster().getSvCount() == 1);

        assertTrue(var2.getClusterReason().contains(CLUSTER_REASON_LOH_CHAIN));
        assertTrue(var2.getClusterReason().contains(var4.id()));
        assertTrue(var4.getClusterReason().contains(CLUSTER_REASON_LOH_CHAIN));
        assertTrue(var4.getClusterReason().contains(var2.id()));
    }

    @Test
    public void testConsistentBreakendOverlapMerge()
    {
        SvTestHelper tester = new SvTestHelper();

        List<SvVarData> allVariants = Lists.newArrayList();

        // a cluster has 3 consecutive breakends which span other unresolved SV breakends, which are then merged in
        SvVarData consec1 = createBnd(tester.nextVarId(), "1", 100000, 1, "2", 100, -1);
        allVariants.add(consec1);

        SvVarData consec2 = createBnd(tester.nextVarId(), "1", 1000, 1, "2", 200, 1);
        allVariants.add(consec2);

        SvVarData consec3 = createInv(tester.nextVarId(), "1", 30000, 101000, 1);
        allVariants.add(consec3);

        // create some SV in resolved clusters which will be ignored - a DEL with external TI , a simple DEL and a low-qual
        SvVarData var1 = createBnd(tester.nextVarId(), "1", 10000, 1, "3", 200, 1);
        allVariants.add(var1);

        SvVarData var2 = createBnd(tester.nextVarId(), "1", 10100, -1, "3", 100, -1);
        allVariants.add(var2);

        SvVarData var3 = createDel(tester.nextVarId(), "1", 60000, 60100);
        allVariants.add(var3);

        // eqv breakend will be ignored
        SvVarData var4 = createSgl(tester.nextVarId(), "1", 80000, -1, false);
        var4.setAssemblyData(true, ASSEMBLY_TYPE_EQV);
        allVariants.add(var4);

        // now some SV which will be overlapped by the consecutive breakends
        SvVarData overlap1 = createBnd(tester.nextVarId(), "1", 20000, -1, "4", 200, 1);
        allVariants.add(overlap1);

        SvVarData overlap2 = createSgl(tester.nextVarId(), "1", 40000, -1, false);
        allVariants.add(overlap2);

        tester.AllVariants.addAll(allVariants);
        tester.preClusteringInit();

        tester.Analyser.clusterAndAnalyse();

        assertEquals(4, tester.getClusters().size());

        SvCluster mainCluster = null;
        for(final SvCluster cluster : tester.getClusters())
        {
            if(cluster.getSvCount() == 5)
            {
                mainCluster = cluster;
                break;
            }
        }

        if(mainCluster == null)
            assertTrue(false);

        assertTrue(overlap1.getClusterReason().contains(CLUSTER_REASON_LOOSE_OVERLAP));
        assertTrue(overlap2.getClusterReason().contains(CLUSTER_REASON_LOOSE_OVERLAP));
    }

}
