package com.mhxx.gansimu.search

import com.mhxx.gansimu.model.Decoration
import com.mhxx.gansimu.model.SkillPoint
import com.mhxx.gansimu.model.SkillTarget
import com.mhxx.gansimu.state.AppState

/**
 * 空きスロットに装飾品を詰めて目標スキルを補完する
 * 元アプリの装飾品配置ロジックの簡易版（貪欲法）
 */
object DecoFiller {

    data class FillResult(
        val totals: Map<String, Int>,
        val usedDecos: List<Pair<String, Int>>, // 名前 to 個数
        val remainingSlots: Int
    )

    /**
     * @param baseTotals 装備+お守りのスキル合計
     * @param totalSlots 空きスロット総数
     * @param targets 目標
     * @param decorations 使用可能な装飾品
     */
    fun fill(
        baseTotals: Map<String, Int>,
        totalSlots: Int,
        targets: List<SkillTarget>,
        decorations: List<Decoration>
    ): FillResult {
        val totals = baseTotals.toMutableMap()
        val used = mutableMapOf<String, Int>()
        var slots = totalSlots

        if (slots <= 0) {
            return FillResult(totals, emptyList(), 0)
        }

        // 不足している系統
        fun deficits(): List<Pair<String, Int>> {
            return targets.mapNotNull { t ->
                val have = totals[t.series] ?: 0
                val need = t.requiredPoints - have
                if (need > 0) t.series to need else null
            }.sortedByDescending { it.second }
        }

        // 使える装飾品（除外されていない、目標系統に貢献するもの）
        val targetSeries = targets.map { it.series }.toSet()
        val usable = decorations
            .filter { !AppState.isDecoExcluded(it.name) }
            .filter { deco -> deco.skills.any { it.series in targetSeries && it.points > 0 } }
            .sortedWith(
                compareByDescending<Decoration> { d ->
                    d.skills.filter { it.series in targetSeries }.sumOf { it.points }.toDouble() / d.slotsRequired.coerceAtLeast(1)
                }.thenBy { it.slotsRequired }
            )

        // 貪欲に詰める
        var guard = 0
        while (slots > 0 && guard++ < 50) {
            val defs = deficits()
            if (defs.isEmpty()) break

            var placed = false
            for ((series, need) in defs) {
                val deco = usable.firstOrNull { d ->
                    d.slotsRequired <= slots &&
                        d.skills.any { it.series == series && it.points > 0 }
                } ?: continue

                // 配置
                slots -= deco.slotsRequired
                deco.skills.forEach { sp ->
                    totals[sp.series] = (totals[sp.series] ?: 0) + sp.points
                }
                used[deco.name] = (used[deco.name] ?: 0) + 1
                placed = true
                break
            }
            if (!placed) break
        }

        return FillResult(
            totals = totals,
            usedDecos = used.map { it.key to it.value },
            remainingSlots = slots
        )
    }
}
