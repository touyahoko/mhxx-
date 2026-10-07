package com.mhxx.gansimu.search

import com.mhxx.gansimu.data.DataRepository
import com.mhxx.gansimu.model.*
import com.mhxx.gansimu.state.AppState

/**
 * 装備組み合わせ探索エンジン
 * - 固定装備 / 除外装備 / タイプ・性別フィルタ
 * - お守りポイント加算
 * - 装飾品自動配置（貪欲）
 * - 目標スキルポイント充足判定
 */
class EquipSearchEngine(private val repo: DataRepository) {

    data class Result(
        val set: EquipSet,
        val skillTotals: Map<String, Int>,
        val matchedSkills: List<String>,
        val usedDecos: List<Pair<String, Int>>,
        val remainingSlots: Int
    )

    fun search(
        targets: List<SkillTarget>,
        maxResults: Int = 30,
        maxCandidatesPerSlot: Int = 40
    ): List<Result> {
        if (targets.isEmpty()) return emptyList()
        val targetSeries = targets.map { it.series }.toSet()

        val candidates = EquipPart.entries.associateWith { part ->
            buildCandidates(part, targetSeries, maxCandidatesPerSlot)
        }

        val results = mutableListOf<Result>()
        val fixed = AppState.fixedByPart

        fun listFor(part: EquipPart): List<Equipment> {
            fixed[part]?.let { return listOf(it) }
            return candidates[part] ?: emptyList()
        }

        var heads = listFor(EquipPart.HEAD)
        var bodies = listFor(EquipPart.BODY)
        var arms = listFor(EquipPart.ARM)
        var wsts = listFor(EquipPart.WST)
        var legs = listFor(EquipPart.LEG)

        // 候補が空の部位はスキップできないので最低限ダミーは出さない。固定がなければ空リスト＝その部位なしは不可
        // 組み合わせ爆発抑制
        val combo = heads.size.toLong() * bodies.size.coerceAtLeast(1) *
            arms.size.coerceAtLeast(1) * wsts.size.coerceAtLeast(1) * legs.size.coerceAtLeast(1)
        if (combo > 1_500_000L) {
            val n = 10
            heads = heads.take(n)
            bodies = bodies.take(n)
            arms = arms.take(n)
            wsts = wsts.take(n)
            legs = legs.take(n)
        } else if (combo > 400_000L) {
            val n = 18
            heads = heads.take(n)
            bodies = bodies.take(n)
            arms = arms.take(n)
            wsts = wsts.take(n)
            legs = legs.take(n)
        }

        if (heads.isEmpty() || bodies.isEmpty() || arms.isEmpty() || wsts.isEmpty() || legs.isEmpty()) {
            return emptyList()
        }

        val charm = AppState.effectiveCharm()
        val charmSkills = charm?.skills ?: emptyList()
        val charmSlots = charm?.slots ?: 0
        val charmName = charm?.name ?: ""

        outer@ for (head in heads) {
            for (body in bodies) {
                for (arm in arms) {
                    for (wst in wsts) {
                        for (leg in legs) {
                            val baseSet = EquipSet(
                                head = head, body = body, arm = arm, wst = wst, leg = leg,
                                charmName = charmName,
                                charmSkills = charmSkills,
                                charmSlots = charmSlots
                            )
                            val baseTotals = baseSet.skillTotals()
                            val slots = baseSet.totalSlots()

                            // 装飾品で補完
                            val filled = DecoFiller.fill(
                                baseTotals, slots, targets, repo.decorations
                            )

                            if (meetsTargets(filled.totals, targets)) {
                                val matched = targets
                                    .filter { (filled.totals[it.series] ?: 0) >= it.requiredPoints }
                                    .map { it.skillName }
                                results.add(
                                    Result(
                                        set = baseSet,
                                        skillTotals = filled.totals,
                                        matchedSkills = matched,
                                        usedDecos = filled.usedDecos,
                                        remainingSlots = filled.remainingSlots
                                    )
                                )
                                if (results.size >= maxResults) break@outer
                            }
                        }
                    }
                }
            }
        }

        return results.sortedByDescending { r ->
            targets.sumOf { t -> (r.skillTotals[t.series] ?: 0) } * 10 + r.remainingSlots
        }
    }

    private fun meetsTargets(totals: Map<String, Int>, targets: List<SkillTarget>): Boolean {
        return targets.all { t -> (totals[t.series] ?: 0) >= t.requiredPoints }
    }

    private fun buildCandidates(
        part: EquipPart,
        targetSeries: Set<String>,
        limit: Int
    ): List<Equipment> {
        val all = repo.equipmentByPart[part] ?: emptyList()
        return all.asSequence()
            .filter { AppState.matchesType(it) }
            .filter { AppState.matchesGender(it) }
            .filter { !AppState.isExcluded(it) }
            .map { eq ->
                val contribute = eq.skills
                    .filter { it.series in targetSeries }
                    .sumOf { it.points }
                // 貢献ポイント重視、同点ならスロット多め
                eq to (contribute * 100 + eq.slots * 3 + eq.rarity)
            }
            .sortedByDescending { it.second }
            .map { it.first }
            .distinctBy { it.name }
            .take(limit)
            .toList()
    }

    companion object {
        fun targetsFromSkills(skills: List<Skill>): List<SkillTarget> {
            return skills
                .filter { it.points > 0 }
                .groupBy { it.series }
                .map { (series, list) ->
                    val best = list.maxByOrNull { it.points }!!
                    SkillTarget(
                        series = series,
                        requiredPoints = best.points.coerceAtLeast(1),
                        skillName = best.name
                    )
                }
        }
    }
}
