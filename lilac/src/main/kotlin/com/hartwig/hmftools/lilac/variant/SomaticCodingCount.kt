package com.hartwig.hmftools.lilac.variant

import com.hartwig.hmftools.common.variant.CodingEffect
import com.hartwig.hmftools.lilac.hla.HlaAllele

data class SomaticCodingCount(val allele: HlaAllele, val missense: Double, val nonsense: Double, val splice: Double, val synonymous: Double) {
    val total = missense + nonsense + splice + synonymous

    companion object {
        fun create(winners: List<HlaAllele>): List<SomaticCodingCount> {
            return winners.sorted().map { SomaticCodingCount(it, 0.0, 0.0, 0.0, 0.0) }
        }

        fun List<SomaticCodingCount>.addVariant(variantEffect: CodingEffect, variantAlleles: Set<HlaAllele>): List<SomaticCodingCount> {
            val contribution = 1.0 / variantAlleles.size
            val result = mutableListOf<SomaticCodingCount>()
            result.addAll(this.filter { it.allele !in variantAlleles })

            for (variantAllele in variantAlleles) {
                val counts = this.filter { it.allele == variantAllele }.sortedBy { it.total }

                // Split between them
                // result.addAll(counts.map { it.addVariant(variantEffect, contribution / counts.size) })

                // Alternative, give only to first to first
                if (counts.isNotEmpty()) {
                    result.add(counts[0].addVariant(variantEffect, contribution))
                    result.addAll(counts.takeLast(counts.size - 1))
                }
            }

            return result.sortedBy { it.allele }
        }
    }

    private fun addVariant(effect: CodingEffect, contribution: Double): SomaticCodingCount {
        return when (effect) {
            CodingEffect.MISSENSE -> copy(missense = missense + contribution)
            CodingEffect.NONSENSE_OR_FRAMESHIFT -> copy(nonsense = nonsense + contribution)
            CodingEffect.SPLICE -> copy(splice = splice + contribution)
            CodingEffect.SYNONYMOUS -> copy(synonymous = synonymous + contribution)
            else -> this
        }
    }

}