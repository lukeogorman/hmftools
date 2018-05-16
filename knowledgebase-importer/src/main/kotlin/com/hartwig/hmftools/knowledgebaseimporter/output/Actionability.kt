package com.hartwig.hmftools.knowledgebaseimporter.output

data class Actionability(val source: String, val cancerType: String, val drug: String, val level: String, val significance: String,
                         val evidenceType: String, val hmfLevel: HmfLevel) {
    companion object {
        val header = listOf("source", "drug", "cancerType", "level", "hmfLevel", "evidenceType", "significance")

        operator fun invoke(source: String, cancerTypes: List<String>, drugs: List<String>, level: String, significance: String,
                            evidenceType: String, hmfLevel: HmfLevel): List<Actionability> {
            return cancerTypes.flatMap { cancerType ->
                drugs.map { drug ->
                    Actionability(source, cancerType, drug, level, significance, evidenceType, hmfLevel)
                }
            }
        }
    }

    val record: List<String> = listOf(source, drug, cancerType, level, hmfLevel.name, evidenceType, significance)
}
