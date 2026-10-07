package com.mhxx.gansimu.search

import com.mhxx.gansimu.data.DataRepository
import com.mhxx.gansimu.model.*
import com.mhxx.gansimu.state.AppState

/**
 * お守り穴埋め自動計算
 * 装備5部位 + 装飾品配置後に足りないスキルポイントを「必要お守り」として逆算する
 */
object CharmCalculator {

    data class RequiredCharm(
        val skills: List<SkillPoint>,
        val suggestedSlots: Int,
        val detail: String
    )

    /**
     * 現在の固定・除外・タイプ設定の下で、目標スキルを満たす装備を探索し、
     * お守りなしで最も穴が小さいセットについて必要お守りを算出する。
     */
    fun calculate(
        repo: DataRepository,
        targets: List<SkillTarget>
    ): RequiredCharm? {
        if (targets.isEmpty()) return null

        // お守りなしで探索（AppStateの選択お守りを一時無効化）
        val savedCharm = AppState.selectedCharm
        val savedCustom = AppState.customCharm
        val savedUse = AppState.useCharm
        AppState.selectedCharm = null
        AppState.customCharm = null
        AppState.useCharm = false

        val engine = EquipSearchEngine(repo)
        // お守りなし前提の装備候補を広く取る
        val results = try {
            engine.search(targets, maxResults = 15, maxCandidatesPerSlot = 35)
        } finally {
            AppState.selectedCharm = savedCharm
            AppState.customCharm = savedCustom
            AppState.useCharm = savedUse
        }

        // お守りなしで既に達成できている場合
        if (results.isNotEmpty()) {
            val best = results.first()
            // 装飾品込みで達成済み → お守り不要
            return RequiredCharm(
                skills = emptyList(),
                suggestedSlots = best.remainingSlots.coerceAtMost(3),
                detail = buildString {
                    appendLine("お守りなしでも達成可能です。")
                    appendLine("例: " + best.set.parts().joinToString(" / ") {
                        "${it.first.displayName}:${it.second?.name ?: "-"}"
                    })
                    if (best.usedDecos.isNotEmpty()) {
                        appendLine("珠: " + best.usedDecos.joinToString { "${it.first}×${it.second}" })
                    }
                    append("残スロ: ${best.remainingSlots}")
                }
            )
        }

        // お守りなしでは未達 → 各セット候補で不足分を計算し、最小不足を探す
        // 探索は「目標を無視して高貢献装備の組み合わせ」から不足を見る
        val targetSeries = targets.map { it.series }.toSet()
        val sampleSets = sampleHighContributeSets(repo, targetSeries, 80)

        var bestGap: List<SkillPoint> = emptyList()
        var bestGapSum = Int.MAX_VALUE
        var bestSet: EquipSet? = null
        var bestAfterDeco: Map<String, Int> = emptyMap()

        for (set in sampleSets) {
            val base = set.skillTotals()
            val slots = set.totalSlots()
            val filled = DecoFiller.fill(base, slots, targets, repo.decorations)
            val gap = targets.mapNotNull { t ->
                val have = filled.totals[t.series] ?: 0
                val need = t.requiredPoints - have
                if (need > 0) SkillPoint(t.series, need) else null
            }
            val gapSum = gap.sumOf { it.points }
            if (gapSum < bestGapSum) {
                bestGapSum = gapSum
                bestGap = gap
                bestSet = set
                bestAfterDeco = filled.totals
            }
            if (gapSum == 0) break
        }

        if (bestSet == null) {
            // フォールバック: 目標ポイントをそのまま必要お守りとする
            val gap = targets.map { SkillTarget ->
                SkillPoint(SkillTarget.series, SkillTarget.requiredPoints)
            }
            return RequiredCharm(
                skills = gap,
                suggestedSlots = 3,
                detail = "装備候補を十分に評価できませんでした。目標スキル分をお守りで賄う前提です。"
            )
        }

        if (bestGap.isEmpty()) {
            return RequiredCharm(
                skills = emptyList(),
                suggestedSlots = 0,
                detail = "サンプル装備＋装飾品で達成可能です。"
            )
        }

        // スロット提案: 不足ポイントが多ければスロ3推奨
        val suggestedSlots = when {
            bestGapSum >= 10 -> 3
            bestGapSum >= 5 -> 2
            else -> 1
        }

        return RequiredCharm(
            skills = bestGap,
            suggestedSlots = suggestedSlots,
            detail = buildString {
                appendLine("不足スキル（お守りで補う）:")
                bestGap.forEach { sp ->
                    appendLine("  ・${sp.series} +${sp.points}")
                }
                appendLine("推奨スロット: $suggestedSlots")
                appendLine()
                appendLine("基準装備例:")
                bestSet.parts().forEach { (part, eq) ->
                    appendLine("  ${part.displayName}: ${eq?.name ?: "-"}")
                }
                appendLine("装飾品後ポイント:")
                bestAfterDeco.filter { it.value != 0 }
                    .toList().sortedByDescending { it.second }.take(8)
                    .forEach { append("  ${it.first}${if (it.second > 0) "+" else ""}${it.second}") }
            }
        )
    }

    private fun sampleHighContributeSets(
        repo: DataRepository,
        targetSeries: Set<String>,
        limit: Int
    ): List<EquipSet> {
        fun top(part: EquipPart, n: Int): List<Equipment> {
            return (repo.equipmentByPart[part] ?: emptyList())
                .asSequence()
                .filter { AppState.matchesType(it) && AppState.matchesGender(it) && !AppState.isExcluded(it) }
                .map { eq ->
                    val c = eq.skills.filter { it.series in targetSeries }.sumOf { it.points }
                    eq to (c * 100 + eq.slots)
                }
                .sortedByDescending { it.second }
                .map { it.first }
                .take(n)
                .toList()
        }

        val n = 6
        val h = top(EquipPart.HEAD, n)
        val b = top(EquipPart.BODY, n)
        val a = top(EquipPart.ARM, n)
        val w = top(EquipPart.WST, n)
        val l = top(EquipPart.LEG, n)
        if (h.isEmpty() || b.isEmpty() || a.isEmpty() || w.isEmpty() || l.isEmpty()) return emptyList()

        val out = mutableListOf<EquipSet>()
        outer@ for (head in h) {
            for (body in b) {
                for (arm in a) {
                    for (wst in w) {
                        for (leg in l) {
                            out.add(EquipSet(head = head, body = body, arm = arm, wst = wst, leg = leg))
                            if (out.size >= limit) break@outer
                        }
                    }
                }
            }
        }
        return out
    }
}
