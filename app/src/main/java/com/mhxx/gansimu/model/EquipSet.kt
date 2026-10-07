package com.mhxx.gansimu.model

/**
 * 装備5部位 + お守りのセット
 */
data class EquipSet(
    val name: String = "",
    val head: Equipment? = null,
    val body: Equipment? = null,
    val arm: Equipment? = null,
    val wst: Equipment? = null,
    val leg: Equipment? = null,
    val charmName: String = "",
    val charmSkills: List<SkillPoint> = emptyList(),
    val charmSlots: Int = 0
) {
    fun parts(): List<Pair<EquipPart, Equipment?>> = listOf(
        EquipPart.HEAD to head,
        EquipPart.BODY to body,
        EquipPart.ARM to arm,
        EquipPart.WST to wst,
        EquipPart.LEG to leg
    )

    fun allEquipment(): List<Equipment> =
        listOfNotNull(head, body, arm, wst, leg)

    fun totalSlots(): Int =
        allEquipment().sumOf { it.slots } + charmSlots

    /** スキル系統ごとのポイント合計（装飾品なし） */
    fun skillTotals(): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        allEquipment().forEach { eq ->
            eq.skills.forEach { sp ->
                map[sp.series] = (map[sp.series] ?: 0) + sp.points
            }
        }
        charmSkills.forEach { sp ->
            map[sp.series] = (map[sp.series] ?: 0) + sp.points
        }
        return map
    }

    fun summary(): String = buildString {
        parts().forEach { (part, eq) ->
            append("${part.displayName}: ${eq?.name ?: "（なし）"}\n")
        }
        if (charmName.isNotEmpty()) append("守: $charmName\n")
        val totals = skillTotals().filter { it.value != 0 }.toList()
            .sortedByDescending { it.second }
        if (totals.isNotEmpty()) {
            append("スキル: ")
            append(totals.joinToString(" ") { "${it.first}${if (it.second > 0) "+" else ""}${it.second}" })
        }
    }
}

/** 検索で目指すスキル条件 */
data class SkillTarget(
    val series: String,
    val requiredPoints: Int,
    val skillName: String = series
)
