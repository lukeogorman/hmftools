package com.hartwig.hmftools.linx.chaining;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;

import static com.hartwig.hmftools.linx.analysis.SvUtilities.copyNumbersEqual;
import static com.hartwig.hmftools.linx.analysis.SvUtilities.formatPloidy;
import static com.hartwig.hmftools.linx.chaining.ChainFinder.MIN_CHAINING_PLOIDY_LEVEL;
import static com.hartwig.hmftools.linx.chaining.ChainPloidyLimits.calcPloidyUncertainty;
import static com.hartwig.hmftools.linx.chaining.ChainPloidyLimits.ploidyMatch;
import static com.hartwig.hmftools.linx.chaining.ChainingRule.ASSEMBLY;
import static com.hartwig.hmftools.linx.chaining.ChainingRule.FOLDBACK_SPLIT;
import static com.hartwig.hmftools.linx.chaining.SvChain.reconcileChains;
import static com.hartwig.hmftools.linx.types.SvVarData.SE_END;
import static com.hartwig.hmftools.linx.types.SvVarData.SE_START;
import static com.hartwig.hmftools.linx.types.SvVarData.isStart;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.linx.cn.PloidyCalcData;
import com.hartwig.hmftools.linx.types.SvBreakend;
import com.hartwig.hmftools.linx.types.SvLinkedPair;
import com.hartwig.hmftools.linx.types.SvVarData;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChainLinkAllocator
{
    private static final Logger LOGGER = LogManager.getLogger(ChainLinkAllocator.class);

    private int mClusterId;

    private final List<SvLinkedPair> mSkippedPairs;
    private int mLinkIndex; // incrementing value for each link added to any chain
    private boolean mIsValid;
    private boolean mPairSkipped; // keep track of any excluded pair or SV without exiting the chaining routine
    private final List<SvLinkedPair> mUniquePairs; // cache of unique pairs added through c
    private int mNextChainId;
    private final Map<SvVarData, SvChainState> mSvConnectionsMap;
    private final List<SvChainState> mSvCompletedConnections;

    // references
    private final Map<SvVarData,List<SvLinkedPair>> mComplexDupCandidates;
    private final List<SvChain> mChains;
    private final Map<SvBreakend, List<SvLinkedPair>> mSvBreakendPossibleLinks;

    public ChainLinkAllocator(
            final Map<SvBreakend, List<SvLinkedPair>> svBreakendPossibleLinks,
            final List<SvChain> chains,
            final Map<SvVarData,List<SvLinkedPair>> complexDupCandidates)
    {
        mSvBreakendPossibleLinks = svBreakendPossibleLinks;
        mComplexDupCandidates = complexDupCandidates;
        mChains = chains;

        mSvConnectionsMap = Maps.newHashMap();
        mSvCompletedConnections = Lists.newArrayList();
        mUniquePairs = Lists.newArrayList();
        mSkippedPairs = Lists.newArrayList();
        mIsValid = true;
        mNextChainId = 0;
    }

    public final Map<SvVarData, SvChainState> getSvConnectionsMap() { return mSvConnectionsMap; }
    public final List<SvChainState> getSvCompletedConnections() { return mSvCompletedConnections; }

    public boolean hasSkippedPairs(final SvLinkedPair pair) { return mSkippedPairs.contains(pair); }
    public final List<SvLinkedPair> getUniquePairs() { return mUniquePairs; }

    public int getNextChainId() { return mNextChainId; }
    public int getLinkIndex() { return mLinkIndex; }

    public boolean isValid() { return mIsValid; }

    public boolean pairSkipped() { return mPairSkipped; }
    public void clearPairSkipped() { mPairSkipped = false; }

    public void initialise(int clusterId)
    {
        mClusterId = clusterId;

        mIsValid = true;
        mLinkIndex = 0;
        mPairSkipped = false;
        mNextChainId = 0;

        mUniquePairs.clear();
        mSkippedPairs.clear();
        mSvConnectionsMap.clear();
        mSvCompletedConnections.clear();
    }

    public void populateSvPloidyMap(final List<SvVarData> svList, boolean clusterHasReplication)
    {
        // make a cache of all unchained breakends in those of replicated SVs
        for(final SvVarData var : svList)
        {
            if(var.ploidy() <= MIN_CHAINING_PLOIDY_LEVEL)
            {
                // for now skip these
                LOGGER.debug("cluster({}) skipping SV({}) with low ploidy({} min={} max={})",
                        mClusterId, var.id(), formatPloidy(var.ploidy()), formatPloidy(var.ploidyMin()), formatPloidy(var.ploidyMax()));
                continue;
            }

            mSvConnectionsMap.put(var, new SvChainState(var, !clusterHasReplication));
        }
    }

    public void processProposedLinks(List<ProposedLinks> proposedLinksList)
    {
        boolean linkAdded = false;

        while (!proposedLinksList.isEmpty())
        {
            ProposedLinks proposedLinks = proposedLinksList.get(0);

            // in case an earlier link has invalidated the chain
            if (proposedLinks.targetChain() != null && !mChains.contains(proposedLinks.targetChain()))
                break;

            proposedLinksList.remove(0);

            if (!proposedLinks.isValid())
            {
                LOGGER.error("cluster({}) skipping invalid proposed links: {}", mClusterId, proposedLinks.toString());
                continue;
            }

            linkAdded |= addLinks(proposedLinks);

            if (!mIsValid)
                return;

            if (proposedLinks.multiConnection()) // stop after the first complex link is made
                break;
        }

        if (linkAdded)
        {
            mSkippedPairs.clear(); // any skipped links can now be re-evaluated
        }
    }

    protected static int SPEC_LINK_INDEX = -1;
    // protected static int SPEC_LINK_INDEX = 26;

    public boolean addLinks(final ProposedLinks proposedLinks)
    {
        // if a chain is specified, add the links to it
        // otherwise look for a chain which can link in these new pairs
        // and if none can be found, create a new chain with them

        // if no chain has a ploidy matching that of the new link and the new link is lower, then split the chain
        // if the chain has a lower ploidy, then only assign the ploidy of the chain
        // if the chain has a matching ploidy then recalculate it with the new SV's ploidy and uncertainty

        SvLinkedPair newPair = proposedLinks.Links.get(0);

        if (mLinkIndex == SPEC_LINK_INDEX)
        {
            LOGGER.debug("specific index({})", mLinkIndex);
        }

        final String topRule = proposedLinks.topRule().toString();
        proposedLinks.Links.forEach(x -> x.setLinkReason(topRule, mLinkIndex));

        boolean addLinksToNewChain = true;
        boolean reconcileChains = false;

        if (proposedLinks.targetChain() != null)
        {
            addLinksToNewChain = false;
            addComplexLinksToExistingChain(proposedLinks);
            reconcileChains = true;
        }
        else if (proposedLinks.multiConnection())
        {
            // if no chain has been specified then don't search for one - this is managed by the specific rule-finder
        }
        else
        {
            SvChain targetChain = null;

            boolean pairLinkedOnFirst = false;
            boolean addToStart = false;
            boolean matchesChainPloidy = false;
            double newSvPloidy = 0;

            // test every chain for whether the link would close it and look for a chain which can connect with this link

            // if one of the breakends in this new link has its other breakend in another chain and is exhausted, then force it
            // to connect to that existing chain

            List<SvChain> chains = getChainsWithOpenBreakend(newPair.firstBreakend());
            chains.addAll(getChainsWithOpenBreakend(newPair.secondBreakend()));

            for (SvChain chain : chains)
            {
                boolean[] canAddToStart = { false, false };
                boolean linksToFirst = false;
                SvChain requiredChain = null;

                boolean ploidyMatched = ploidyMatch(
                        proposedLinks.ploidy(), chain.ploidyUncertainty(), chain.ploidy(), chain.ploidyUncertainty());

                for(int se = SE_START; se <= SE_END; ++se)
                {
                    final SvBreakend chainBreakend = chain.getOpenBreakend(isStart(se));

                    if(chainBreakend == null)
                        continue;

                    if(chainBreakend == newPair.firstBreakend())
                        linksToFirst = true;
                    else if(chainBreakend == newPair.secondBreakend())
                        linksToFirst = false;
                    else
                        continue;

                    canAddToStart[se] = true;

                    // now look for a chain which must be taken due to being the other breakend's sole, exhaustive connection
                    final SvChainState svConn = mSvConnectionsMap.get(chainBreakend.getSV());

                    if(svConn == null)
                    {
                        LOGGER.error("SV({}) missing chain state info", chainBreakend.getSV().id());
                        mIsValid = false;
                        return false;
                    }

                    if(!svConn.breakendExhausted(!chainBreakend.usesStart()) || svConn.getConnections(!chainBreakend.usesStart()).size() > 1)
                        continue;

                    if(requiredChain == null && proposedLinks.linkPloidyMatch() && ploidyMatched
                    && (chainBreakend == newPair.firstBreakend() || chainBreakend == newPair.secondBreakend()))
                    {
                        LOGGER.trace("pair({}) links breakend({}) to chain({}) as only exhausted connection", newPair, chainBreakend, chain.id());

                        // this chain end is exhausted and matches the link, so force it to be used
                        requiredChain = chain;
                    }
                }

                if (!canAddToStart[SE_START] && !canAddToStart[SE_END])
                    continue;

                boolean couldCloseChain = (canAddToStart[SE_START] && canAddToStart[SE_END]) ? chain.linkWouldCloseChain(newPair) : false;

                if (couldCloseChain)
                {
                    LOGGER.trace("skipping linked pair({}) would close existing chain({})", newPair.toString(), chain.id());
                    addSkippedPair(newPair);
                    return false;
                }

                if(requiredChain != null)
                {
                    targetChain = requiredChain;
                    addToStart = canAddToStart[SE_START];
                    newSvPloidy = targetChain.ploidy();
                    matchesChainPloidy = true;
                    pairLinkedOnFirst = linksToFirst;
                    continue;
                }

                final SvBreakend newBreakend = linksToFirst ? newPair.secondBreakend() : newPair.firstBreakend();

                // check whether a match was expected
                if (!ploidyMatched)
                {
                    if (proposedLinks.linkPloidyMatch())
                        continue;

                    if (targetChain != null && targetChain.ploidy() > chain.ploidy())
                        continue; // stick with the larger ploidy chain
                }
                else if(targetChain != null && matchesChainPloidy)
                {
                    // stick with existing matched chain even if there are other equivalent options
                    continue;
                }

                targetChain = chain;
                addToStart = canAddToStart[SE_START];
                pairLinkedOnFirst = linksToFirst;

                // record the ploidy of the SV which would be added to this chain to determine whether the chain will need splitting
                newSvPloidy = proposedLinks.breakendPloidy(newBreakend);

                if (ploidyMatched)
                {
                    matchesChainPloidy = true;
                }
            }

            // for now don't allow chains to be split, so previous a mismatch if the chain has a higher ploidy
            if(targetChain != null && !matchesChainPloidy && targetChain.ploidy() > proposedLinks.ploidy())
            {
                LOGGER.debug("skipping targetChain({} ploidy={}) for proposedLink({}) on ploidy mismatch",
                        targetChain.id(), formatPloidy(targetChain.ploidy()), proposedLinks);

                targetChain = null;
            }

            if (targetChain != null)
            {
                addLinksToNewChain = false;
                reconcileChains = true;
                addLinksToExistingChain(proposedLinks, targetChain, addToStart, pairLinkedOnFirst, matchesChainPloidy, newSvPloidy);
            }
        }

        if(addLinksToNewChain)
        {
            reconcileChains = addLinksToNewChain(proposedLinks);
        }

        registerNewLink(proposedLinks);
        ++mLinkIndex;

        if (reconcileChains)
        {
            // now see if any partial chains can be linked
            reconcileChains(mChains);
        }

        return true;
    }

    private void addComplexLinksToExistingChain(final ProposedLinks proposedLinks)
    {
        // scenarios:
        // - ploidy matches - add the new link and recalculate the chain ploidy
        // - foldback or complex dup with 2-1 ploidy match - replicate the chain accordingly and halve the chain ploidy
        // - foldback or complex dup with chain greater than 2x the foldback or complex dup
        //      - split off the excess and then replicate and halve the remainder
        // - foldback where the foldback itself is a chain, connecting to a single other breakend which may also be chained
        //      - split the non-foldback chain and add both connections
        // - complex dup
        //      - around a single SV - just add the 2 new links
        //      - around a chain - split chain if > 2x ploidy, then add the 2 new links

        SvChain targetChain = proposedLinks.targetChain();
        boolean matchesChainPloidy = proposedLinks.linkPloidyMatch();
        double newSvPloidy = proposedLinks.ploidy();

        if(targetChain != null)
        {
            if (!matchesChainPloidy && targetChain.ploidy() > newSvPloidy * 2)
            {
                SvChain newChain = new SvChain(mNextChainId++);
                mChains.add(newChain);

                // copy the existing links into a new chain and set to the ploidy difference
                newChain.copyFrom(targetChain);

                if (targetChain.ploidy() > newSvPloidy * 2)
                {
                    // chain will have its ploidy halved a  nyway so just split off the excess
                    newChain.setPloidyData(targetChain.ploidy() - newSvPloidy * 2, targetChain.ploidyUncertainty());
                    targetChain.setPloidyData(newSvPloidy * 2, targetChain.ploidyUncertainty());
                }

                LOGGER.debug("new chain({}) ploidy({}) from chain({}) ploidy({}) from new SV ploidy({})",
                        newChain.id(), formatPloidy(newChain.ploidy()),
                        targetChain.id(), formatPloidy(targetChain.ploidy()), formatPloidy(newSvPloidy));
            }
        }

        LOGGER.debug("duplicating chain({} links={} sv={}) for multi-connect {}",
                targetChain.id(), targetChain.getLinkCount(), targetChain.getSvCount(), proposedLinks.getSplittingRule());

        if (proposedLinks.getSplittingRule() == FOLDBACK_SPLIT)
        {
            final SvChain foldbackChain = proposedLinks.foldbackChain();

            if(foldbackChain != null)
            {
                targetChain.foldbackChainOnChain(foldbackChain, proposedLinks.Links.get(0), proposedLinks.Links.get(1));
                mChains.remove(foldbackChain);
            }
            else
            {
                targetChain.foldbackChainOnLink(proposedLinks.Links.get(0), proposedLinks.Links.get(1));
            }
        }
        else
        {
            targetChain.duplicateChainOnLink(proposedLinks.Links.get(0), proposedLinks.Links.get(1));
        }

        double newPloidy = targetChain.ploidy() * 0.5;
        double newUncertainty = targetChain.ploidyUncertainty() / sqrt(2);
        targetChain.setPloidyData(newPloidy, newUncertainty);

        for (SvLinkedPair pair : proposedLinks.Links)
        {
            LOGGER.debug("index({}) method({}) adding linked pair({} ploidy={}) to existing chain({}) ploidy({})",
                    mLinkIndex, proposedLinks.topRule(), pair.toString(), formatPloidy(proposedLinks.ploidy()),
                    targetChain.id(), String.format("%.1f unc=%.1f", targetChain.ploidy(), targetChain.ploidyUncertainty()));
        }
    }

    private void addLinksToExistingChain(final ProposedLinks proposedLinks, SvChain targetChain,
            boolean addToStart, boolean pairLinkedOnFirst, boolean matchesChainPloidy, double newSvPloidy)
    {
        // scenarios:
        // - ploidy matches - add the new link and recalculate the chain ploidy
        // - normal link with chain ploidy higher - split off the chain
        // - normal link with chain ploidy lower - only allocate the chain ploidy for the new link
        boolean requiresChainSplit = false;

        final SvLinkedPair newPair = proposedLinks.Links.get(0);

        if (!matchesChainPloidy && targetChain.ploidy() > newSvPloidy)
        {
            requiresChainSplit = true;
            matchesChainPloidy = true;
        }

        if (requiresChainSplit)
        {
            SvChain newChain = new SvChain(mNextChainId++);
            mChains.add(newChain);

            // copy the existing links into a new chain and set to the ploidy difference
            newChain.copyFrom(targetChain);

            newChain.setPloidyData(targetChain.ploidy() - newSvPloidy, targetChain.ploidyUncertainty());
            targetChain.setPloidyData(newSvPloidy, targetChain.ploidyUncertainty());

            LOGGER.debug("new chain({}) ploidy({}) from chain({}) ploidy({}) from new SV ploidy({})",
                    newChain.id(), formatPloidy(newChain.ploidy()),
                    targetChain.id(), formatPloidy(targetChain.ploidy()), formatPloidy(newSvPloidy));
        }

        final SvBreakend newSvBreakend = pairLinkedOnFirst ? newPair.secondBreakend() : newPair.firstBreakend();

        PloidyCalcData ploidyData;

        if (matchesChainPloidy || targetChain.ploidy() > newSvPloidy)
        {
            if (!proposedLinks.linkPloidyMatch())
            {
                ploidyData = calcPloidyUncertainty(
                        new PloidyCalcData(proposedLinks.ploidy(), newSvBreakend.ploidyUncertainty()),
                        new PloidyCalcData(targetChain.ploidy(), targetChain.ploidyUncertainty()));
            }
            else
            {
                ploidyData = calcPloidyUncertainty(
                        new PloidyCalcData(proposedLinks.breakendPloidy(newSvBreakend), newSvBreakend.ploidyUncertainty()),
                        new PloidyCalcData(targetChain.ploidy(), targetChain.ploidyUncertainty()));
            }

            targetChain.setPloidyData(ploidyData.PloidyEstimate, ploidyData.PloidyUncertainty);
        }
        else
        {
            // ploidy of the link is higher so keep the chain ploidy unch and reduce what can be allocated from this link
            proposedLinks.setLowerPloidy(targetChain.ploidy());
        }

        targetChain.addLink(proposedLinks.Links.get(0), addToStart);

        LOGGER.debug("index({}) method({}) adding linked pair({} ploidy={}) to existing chain({}) ploidy({})",
                mLinkIndex, proposedLinks.topRule(), newPair.toString(), formatPloidy(proposedLinks.ploidy()),
                targetChain.id(), String.format("%.1f unc=%.1f", targetChain.ploidy(), targetChain.ploidyUncertainty()));
    }

    private boolean addLinksToNewChain(final ProposedLinks proposedLinks)
    {
        boolean reconcileChains = false;
        final SvLinkedPair newPair = proposedLinks.Links.get(0);

        // where more than one links is being added, they may not be able to be added to the same chain
        // eg a chained foldback replicating another breakend - the chain reconciliation step will join them back up
        SvChain newChain = null;
        for (final SvLinkedPair pair : proposedLinks.Links)
        {
            if (newChain != null)
            {
                if (newChain.canAddLinkedPairToStart(pair))
                {
                    newChain.addLink(pair, true);
                }
                else if (newChain.canAddLinkedPairToEnd(pair))
                {
                    newChain.addLink(pair, false);
                }
                else
                {
                    newChain = null;
                    reconcileChains = true;
                }
            }

            if (newChain == null)
            {
                newChain = new SvChain(mNextChainId++);
                mChains.add(newChain);

                newChain.addLink(pair, true);

                PloidyCalcData ploidyData;

                if (!proposedLinks.linkPloidyMatch() || proposedLinks.multiConnection())
                {
                    ploidyData = calcPloidyUncertainty(
                            new PloidyCalcData(proposedLinks.ploidy(), newPair.first().ploidyUncertainty()),
                            new PloidyCalcData(proposedLinks.ploidy(), newPair.second().ploidyUncertainty()));
                }
                else
                {
                    // blend the ploidies of the 2 SVs
                    ploidyData = calcPloidyUncertainty(
                            new PloidyCalcData(proposedLinks.breakendPloidy(newPair.firstBreakend()),
                                    newPair.first().ploidyUncertainty()),
                            new PloidyCalcData(proposedLinks.breakendPloidy(newPair.secondBreakend()),
                                    newPair.second().ploidyUncertainty()));
                }

                newChain.setPloidyData(ploidyData.PloidyEstimate, ploidyData.PloidyUncertainty);
            }

            LOGGER.debug("index({}) method({}) adding linked pair({} ploidy={}) to new chain({}) ploidy({})",
                    mLinkIndex, proposedLinks.topRule(), pair.toString(), formatPloidy(proposedLinks.ploidy()),
                    newChain.id(), String.format("%.1f unc=%.1f", newChain.ploidy(), newChain.ploidyUncertainty()));
        }

        return reconcileChains;
    }

    private void registerNewLink(final ProposedLinks proposedLink)
    {
        List<SvBreakend> exhaustedBreakends = Lists.newArrayList();
        boolean canUseMaxPloidy = proposedLink.topRule() == ASSEMBLY;

        for (final SvLinkedPair newPair : proposedLink.Links)
        {
            for (int se = SE_START; se <= SE_END; ++se)
            {
                final SvBreakend breakend = newPair.getBreakend(isStart(se));

                if (exhaustedBreakends.contains(breakend))
                    continue;

                final SvBreakend otherPairBreakend = newPair.getOtherBreakend(breakend);
                final SvVarData var = breakend.getSV();

                SvChainState svConn = mSvConnectionsMap.get(var);

                if (otherPairBreakend == null || breakend == null)
                {
                    LOGGER.error("cluster({}) invalid breakend in proposed link: {}", mClusterId, proposedLink.toString());
                    mIsValid = false;
                    return;
                }

                if (svConn == null || svConn.breakendExhaustedVsMax(breakend.usesStart()))
                {
                    LOGGER.error("breakend({}) breakend already exhausted: {} with proposedLink({})",
                            breakend.toString(), svConn != null ? svConn.toString() : "null", proposedLink.toString());
                    mIsValid = false;
                    return;
                }

                svConn.addConnection(otherPairBreakend, breakend.usesStart());

                boolean breakendExhausted = proposedLink.breakendPloidyMatched(breakend);

                if (breakendExhausted)
                {
                    // this proposed link fully allocates the breakend
                    svConn.add(breakend.usesStart(), max(svConn.unlinked(breakend.usesStart()), proposedLink.ploidy()));
                }
                else
                {
                    svConn.add(breakend.usesStart(), proposedLink.ploidy());

                    breakendExhausted = canUseMaxPloidy ? svConn.breakendExhaustedVsMax(breakend.usesStart())
                            : svConn.breakendExhausted(breakend.usesStart());
                }

                if (breakendExhausted)
                    exhaustedBreakends.add(breakend);

                final SvBreakend otherSvBreakend = var.getBreakend(!breakend.usesStart());

                if (otherSvBreakend != null)
                    removeOppositeLinks(otherSvBreakend, otherPairBreakend);
            }

            // track unique pairs to avoid conflicts (eg end-to-end and start-to-start)
            if (!matchesExistingPair(newPair))
            {
                mUniquePairs.add(newPair);
            }
        }

        // clean up breakends and SVs which have been fully allocated
        for (final SvBreakend breakend : exhaustedBreakends)
        {
            final SvVarData var = breakend.getSV();

            SvChainState svConn = mSvConnectionsMap.get(var);

            if (svConn != null)
            {
                boolean otherBreakendExhausted = canUseMaxPloidy ? svConn.breakendExhaustedVsMax(!breakend.usesStart())
                        : svConn.breakendExhausted(!breakend.usesStart());

                if (otherBreakendExhausted)
                {
                    checkSvComplete(svConn);

                    // could be moved to rule selector
                    if (mComplexDupCandidates.get(var) != null)
                    {
                        mComplexDupCandidates.remove(var);
                    }
                }
            }

            // since this breakend has been exhausted, remove any links which depend on it
            removePossibleLinks(breakend);
        }
    }

    private void removePossibleLinks(SvBreakend breakend)
    {
        List<SvLinkedPair> possibleLinks = mSvBreakendPossibleLinks.get(breakend);

        if (possibleLinks == null || possibleLinks.isEmpty())
            return;

        int index = 0;
        while (index < possibleLinks.size())
        {
            SvLinkedPair possibleLink = possibleLinks.get(index);

            if (possibleLink.hasBreakend(breakend))
            {
                // remove this from consideration
                possibleLinks.remove(index);

                SvBreakend otherBreakend = possibleLink.getBreakend(true) == breakend ?
                        possibleLink.getBreakend(false) : possibleLink.getBreakend(true);

                // and remove the pair which was cached in the other breakend's possibles list
                List<SvLinkedPair> otherPossibles = mSvBreakendPossibleLinks.get(otherBreakend);

                if (otherPossibles != null)
                {
                    for (SvLinkedPair otherPair : otherPossibles)
                    {
                        if (otherPair == possibleLink)
                        {
                            otherPossibles.remove(otherPair);

                            if (otherPossibles.isEmpty())
                            {
                                // LOGGER.debug("breakend({}) has no more possible links", otherBreakend);
                                mSvBreakendPossibleLinks.remove(otherBreakend);
                            }

                            break;
                        }
                    }
                }
            }
            else
            {
                ++index;
            }
        }

        if (possibleLinks.isEmpty())
        {
            //LOGGER.debug("breakend({}) has no more possible links", origBreakend);
            mSvBreakendPossibleLinks.remove(breakend);
        }
    }

    private void removeOppositeLinks(final SvBreakend otherSvBreakend, final SvBreakend pairOtherBreakend)
    {
        // otherSvBreakend - the opposite breakend of the breakend just linked
        // pairOtherBreakend - the other breakend in the link

        // check for an opposite pairing between these 2 SVs - need to look into other breakends' lists

        // such a link can happen for a complex dup around a single SV, so skip if any of these exist
        if (mComplexDupCandidates.containsKey(otherSvBreakend.getSV()) || mComplexDupCandidates.containsKey(pairOtherBreakend.getSV()))
            return;

        List<SvLinkedPair> otherBeLinks = mSvBreakendPossibleLinks.get(otherSvBreakend);

        if (otherBeLinks == null)
            return;

        if (otherBeLinks.isEmpty())
        {
            // should have been cleaned up already
            mSvBreakendPossibleLinks.remove(otherSvBreakend);
            return;
        }

        final SvBreakend pairOtherOppBreakend = pairOtherBreakend.getOtherBreakend();

        if (pairOtherOppBreakend == null)
            return;

        for (SvLinkedPair pair : otherBeLinks)
        {
            if (pair.hasBreakend(otherSvBreakend) && pair.hasBreakend(pairOtherOppBreakend))
            {
                otherBeLinks.remove(pair);

                if (otherBeLinks.isEmpty())
                    mSvBreakendPossibleLinks.remove(otherSvBreakend);

                return;
            }
        }
    }

    private void checkSvComplete(final SvChainState svConn)
    {
        if (svConn.breakendExhausted(true) && (svConn.SV.isSglBreakend() || svConn.breakendExhausted(false)))
        {
            LOGGER.trace("SV({}) both breakends exhausted", svConn.toString());
            mSvConnectionsMap.remove(svConn.SV);
            mSvCompletedConnections.add(svConn);
        }
    }

    protected double getUnlinkedBreakendCount(final SvBreakend breakend)
    {
        SvChainState svConn = mSvConnectionsMap.get(breakend.getSV());
        if (svConn == null)
            return 0;

        return !svConn.breakendExhausted(breakend.usesStart()) ? svConn.unlinked(breakend.usesStart()) : 0;
    }

    protected double getUnlinkedBreakendCount(final SvBreakend breakend, boolean limitByChains)
    {
        if(limitByChains)
            return getChainLimitedBreakendPloidy(breakend);
        else
            return getUnlinkedBreakendCount(breakend);
    }

    protected double getChainLimitedBreakendPloidy(final SvBreakend breakend)
    {
        SvChainState svConn = mSvConnectionsMap.get(breakend.getSV());
        if (svConn == null)
            return 0;

        double unlinkedPloidy = !svConn.breakendExhausted(breakend.usesStart()) ? svConn.unlinked(breakend.usesStart()) : 0;

        if(unlinkedPloidy == 0)
            return 0;

        if(!svConn.hasConnections())
            return unlinkedPloidy;

        // if the breakend is the open end of a chain, then the chain's ploidy is a limit on the max which can assigned
        double maxChainPloidy = 0;
        double totalChainPloidy = 0;

        List<SvChain> chains = getChainsWithOpenBreakend(breakend);

        if(chains.isEmpty())
            return unlinkedPloidy;

        // if an Sv is fully connected to a chain on one side, then only offer up the chain ploidy
        if(chains.size() == 1 && svConn.breakendExhausted(!breakend.usesStart()))
            return chains.get(0).ploidy();

        for(final SvChain chain : chains)
        {
            if(chain.getOpenBreakend(true) == breakend)
            {
                totalChainPloidy += chain.ploidy();
                maxChainPloidy = max(maxChainPloidy, chain.ploidy());
            }

            if(chain.getOpenBreakend(false) == breakend)
            {
                totalChainPloidy += chain.ploidy();
                maxChainPloidy = max(maxChainPloidy, chain.ploidy());
            }
        }

        if(totalChainPloidy > 0)
        {
            double unchainedPloidy = max(unlinkedPloidy - totalChainPloidy, 0);
            return max(maxChainPloidy, unchainedPloidy);
        }
        else
        {
            return unlinkedPloidy;
        }
    }

    protected List<SvChain> getChainsWithOpenBreakend(final SvBreakend breakend)
    {
        return mChains.stream()
                .filter(x -> x.getOpenBreakend(true) == breakend || x.getOpenBreakend(false) == breakend)
                .collect(Collectors.toList());
    }

    protected double getMaxUnlinkedBreakendCount(final SvBreakend breakend)
    {
        SvChainState svConn = mSvConnectionsMap.get(breakend.getSV());
        if (svConn == null)
            return 0;

        if (!svConn.breakendExhausted(breakend.usesStart()))
            return svConn.unlinked(breakend.usesStart());
        else if (!svConn.breakendExhaustedVsMax(breakend.usesStart()))
            return svConn.maxUnlinked(breakend.usesStart());
        else
            return 0;
    }

    protected double getUnlinkedCount(final SvVarData var)
    {
        SvChainState svConn = mSvConnectionsMap.get(var);
        if (svConn == null)
            return 0;

        if (svConn.breakendExhausted(true) || svConn.breakendExhausted(false))
            return 0;

        return min(svConn.unlinked(SE_START), svConn.unlinked(SE_END));
    }

    public boolean matchesExistingPair(final SvLinkedPair pair)
    {
        for(SvLinkedPair existingPair : mUniquePairs)
        {
            if(pair.matches(existingPair))
                return true;
        }

        return false;
    }

    private void addSkippedPair(final SvLinkedPair pair)
    {
        if (!mSkippedPairs.contains(pair))
        {
            mPairSkipped = true;
            mSkippedPairs.add(pair);
        }
    }

    private void removeSkippedPairs(List<SvLinkedPair> possiblePairs)
    {
        // some pairs are temporarily unavailable for use (eg those which would close a chain)
        // to to avoid continually trying to add them, keep them out of consideration until a new links is added
        if(mSkippedPairs.isEmpty())
            return;

        int index = 0;
        while(index < possiblePairs.size())
        {
            SvLinkedPair pair = possiblePairs.get(index);

            if(mSkippedPairs.contains(pair))
                possiblePairs.remove(index);
            else
                ++index;
        }
    }

}
