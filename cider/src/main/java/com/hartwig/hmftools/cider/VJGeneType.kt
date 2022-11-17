package com.hartwig.hmftools.cider

// this file defines the gene segments that encode the variable region
// of Ig/TCR locus. They are V, D, J and constant

// the various Ig/TCR locus
enum class IgTcrLocus
{
    IGH,
    IGK,
    IGL,
    TRA,
    TRB,
    TRD,
    TRG
}

enum class VJ
{
    V, J
}

// make this a inner type of VJGene
// create a new type called IgLocus to house IGH, TRA, TRB etc
// note: IGH, TRA etc are locus, IGHV, TRAJ etc are gene segments
// this also include KDE, for simplicity we treat it just like a J anchor
//
enum class VJGeneType
{
    IGHV,
    IGHJ,
    IGKV,
    IGKJ,
    IGLV,
    IGLJ,
    TRAV,
    TRAJ,
    TRBV,
    TRBJ,
    TRDV,
    TRDJ,
    TRGV,
    TRGJ,
    IGKKDE;

    //val locus: IgTcrLocus = IgTcrLocus.valueOf(name.take(3))
    val vj: VJ = if (name == "IGKKDE") VJ.J else VJ.valueOf(name[3].toString())

    // gene types that can be paired with
    fun pairedVjGeneTypes() : List<VJGeneType>
    {
        return when (this)
        {
            IGHV -> listOf(IGHJ)
            IGHJ -> listOf(IGHV)
            IGKV -> listOf(IGKJ, IGKKDE)
            IGKJ -> listOf(IGKV)
            IGLV -> listOf(IGLJ)
            IGLJ -> listOf(IGLV)
            TRAV -> listOf(TRAJ, TRDJ)
            TRAJ -> listOf(TRAV, TRDV)
            TRBV -> listOf(TRBJ)
            TRBJ -> listOf(TRBV)
            TRDV -> listOf(TRDJ, TRAJ)
            TRDJ -> listOf(TRDV, TRAV)
            TRGV -> listOf(TRGJ)
            TRGJ -> listOf(TRGV)
            IGKKDE -> listOf(IGKV)
        }
    }
}
