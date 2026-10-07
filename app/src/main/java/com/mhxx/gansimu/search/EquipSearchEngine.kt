package com.mhxx.gansimu.search

import com.mhxx.gansimu.data.DataRepository
import com.mhxx.gansimu.model.*
import com.mhxx.gansimu.state.AppState

/**
 * 装備組み合わせ探索
 * 固定装備・除外装備を反映し、目標スキルポイントを満たす5部位セットを探す
 */
class EquipSearchEngine(private val repo: DataRepository) {

    data class Result(
        val set: EquipSet,
        val skillTotals: Map<String, Int>,
        val matchedSkills: List<String>
    )

    /**
     * @param targets 目標スキル（系統 + 必要ポイント）
     * @param maxResults 最大件数
     * @param maxCandidatesPerSlot 部位ごとの候補上限（探索時間制御）
     */
    fun search(
        targets: List<SkillTarget>,
        maxResults: Int = 30,
        maxCandidatesPerSlot: Int = 40
    ): List<Result> {
        if (targets.isEmpty()) return emptyList()
        val targetSeries = targets.map { it.series }.toSet()

        // 各部位の候補を絞る
        val candidates = EquipPart.entries.associateWith { part ->
            buildCandidates(part, targetSeries, maxCandidatesPerSlot)
        }

        val results = mutableListOf<Result>()
        val fixed = AppState.fixedByPart

        // 固定がある部位は1通り、なければ候補リスト
        fun listFor(part: EquipPart): List<Equipment?> {
            fixed[part]?.let { return listOf(it) }
            val list = candidates[part] ?: emptyList()
            // 空スロットも許容（固定なし時）
            return list
        }

        val heads = listFor(EquipPart.HEAD)
        val bodies = listFor(EquipPart.BODY)
        val arms = listFor(EquipPart.ARM)
        val wsts = listFor(EquipPart.WST)
        val legs = listFor(EquipPart.LEG)

        // 組み合わせ数が大きすぎる場合はさらに絞る
        val totalCombos = heads.size.toLong() * bodies.size * arms.size * wsts.size * legs.size
        var h = heads
        var b = bodies
        var a = arms
        var w = wsts
        var l = legs
        if (totalCombos > 2_000_000) {
            fun trim(list: List<Equipment?>, n: Int) = list.take(n)
            val n = 12
            h = trim(h, n); b = trim(b, n); a = trim(a, n); w = trim(w, n); l = trim(l, n)
        }

        outer@ for (head in h) {
            for (body in b) {
                for (arm in a) {
                    for (wst in w) {
                        for (leg in l) {
                            val set = EquipSet(
                                head = head, body = body, arm = arm, wst = wst, leg = leg
                            )
                            val totals = set.skillTotals()
                            if (meetsTargets(totals, targets)) {
                                val matched = targets
                                    .filter { (totals[it.series] ?: 0) >= it.requiredPoints }
                                    .map { it.skillName }
                                results.add(Result(set, totals, matched))
                                if (results.size >= maxResults) break@outer
                            }
                        }
                    }
                }
            }
        }

        // スキル合計の余裕が大きい順
        return results.sortedByDescending { r ->
            targets.sumOf { t -> (r.skillTotals[t.series] ?: 0) }
        }
    }

    private fun meetsTargets(totals: Map<String, Int>, targets: List<SkillTarget>): Boolean {
        return targets.all { t -> (totals[t.series] ?: 0) >= t.requiredPoints }
    }

    /**
     * 目標スキル系統にポイントを持つ装備、またはスロットが多い装備を優先
     */
    private fun buildCandidates(
        part: EquipPart,
        targetSeries: Set<String>,
        limit: Int
    ): List<Equipment> {
        val all = repo.equipmentByPart[part] ?: emptyList()
        return all.asSequence()
            .filter { AppState.matchesType(it) }
            .filter { !AppState.isExcluded(it) }
            .map { eq ->
                val contribute = eq.skills
                    .filter { it.series in targetSeries }
                    .sumOf { it.points }
                eq to (contribute * 10 + eq.slots)
            }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(limit)
            .toList()
    }

    companion object {
        /** 選択スキルから目標ポイントを生成（発動に必要なポイント） */
        fun targetsFromSkills(skills: List<Skill>): List<SkillTarget> {
            // 同じ系統は最大ポイントのものを採用
            return skills
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
