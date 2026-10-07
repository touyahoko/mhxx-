package com.mhxx.gansimu.data

import android.content.Context
import com.mhxx.gansimu.model.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset

/**
 * MHXXデータ読み込みリポジトリ
 * 元アプリの e.u に相当するデータ初期化処理
 */
class DataRepository(private val context: Context) {

    var skills: List<Skill> = emptyList()
        private set
    var skillSeries: List<String> = emptyList()
        private set
    var equipmentByPart: Map<EquipPart, List<Equipment>> = emptyMap()
        private set
    var decorations: List<Decoration> = emptyList()
        private set
    var charms: List<Charm> = emptyList()
        private set
    var categories: List<String> = emptyList()
        private set
    var sibori: List<Pair<String, String>> = emptyList()
        private set

    private val shiftJis = Charset.forName("Shift_JIS")

    @Volatile
    var isLoaded = false
        private set

    fun load() {
        if (isLoaded) return
        synchronized(this) {
            if (isLoaded) return
            loadSkills()
            loadEquipment()
            loadDecorations()
            loadCharms()
            loadConf()
            isLoaded = true
        }
    }

    private fun readAssetLines(path: String): List<String> {
        return context.assets.open(path).use { input ->
            BufferedReader(InputStreamReader(input, shiftJis)).use { reader ->
                reader.readLines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
            }
        }
    }

    private fun loadSkills() {
        val lines = readAssetLines("data/MHXX_SKILL.csv")
        val list = mutableListOf<Skill>()
        val seriesSet = linkedSetOf<String>()

        lines.forEachIndexed { index, line ->
            val cols = line.split(",")
            if (cols.size < 4) return@forEachIndexed
            val name = cols[0].trim()
            val series = cols[1].trim()
            var pointsStr = cols[2].trim()
            if (pointsStr.startsWith("+")) pointsStr = pointsStr.substring(1)
            val points = pointsStr.toIntOrNull() ?: return@forEachIndexed
            val type = cols[3].trim().toIntOrNull() ?: 0

            list.add(Skill(index, name, series, points, type))
            seriesSet.add(series)
        }

        // 元アプリ同様、プラススキルを先に並べる
        skills = list.sortedByDescending { it.points }
        skillSeries = seriesSet.toList()
    }

    private fun loadEquipment() {
        val map = mutableMapOf<EquipPart, List<Equipment>>()
        EquipPart.entries.forEach { part ->
            map[part] = loadEquipFile(part)
        }
        equipmentByPart = map
    }

    private fun loadEquipFile(part: EquipPart): List<Equipment> {
        val lines = readAssetLines("data/${part.fileName}")
        val list = mutableListOf<Equipment>()

        lines.forEachIndexed { index, line ->
            val cols = line.split(",")
            if (cols.size < 15) return@forEachIndexed

            val name = cols[0].trim()
            val gender = cols[1].trim().toIntOrNull() ?: 0
            val type = cols[2].trim().toIntOrNull() ?: 0
            val rarity = cols[3].trim().toIntOrNull() ?: 1
            val slots = cols[4].trim().toIntOrNull() ?: 0

            // 防御・耐性 (簡易: 位置は元CSVに依存)
            val defense = cols.getOrNull(9)?.trim()?.toIntOrNull() ?: 0
            val fireRes = cols.getOrNull(10)?.trim()?.toIntOrNull() ?: 0
            val waterRes = cols.getOrNull(11)?.trim()?.toIntOrNull() ?: 0
            val thunderRes = cols.getOrNull(12)?.trim()?.toIntOrNull() ?: 0
            val iceRes = cols.getOrNull(13)?.trim()?.toIntOrNull() ?: 0
            val dragonRes = cols.getOrNull(14)?.trim()?.toIntOrNull() ?: 0

            // スキルポイント (スキル系統1,値1, 系統2,値2 ... 最大5)
            val skillPoints = mutableListOf<SkillPoint>()
            var i = 15
            while (i + 1 < cols.size && skillPoints.size < 5) {
                val series = cols[i].trim()
                val pts = cols[i + 1].trim().toIntOrNull()
                if (series.isNotEmpty() && pts != null && pts != 0) {
                    skillPoints.add(SkillPoint(series, pts))
                }
                i += 2
            }

            list.add(
                Equipment(
                    id = index,
                    name = name,
                    part = part,
                    gender = gender,
                    type = type,
                    rarity = rarity,
                    slots = slots,
                    skills = skillPoints,
                    defense = defense,
                    fireRes = fireRes,
                    waterRes = waterRes,
                    thunderRes = thunderRes,
                    iceRes = iceRes,
                    dragonRes = dragonRes
                )
            )
        }
        return list
    }

    private fun loadDecorations() {
        val lines = readAssetLines("data/MHXX_DECO.csv")
        val list = mutableListOf<Decoration>()

        lines.forEachIndexed { index, line ->
            val cols = line.split(",")
            if (cols.size < 3) return@forEachIndexed
            val name = cols[0].trim()
            val slots = cols[1].trim().toIntOrNull() ?: 1
            val skillPoints = mutableListOf<SkillPoint>()
            var i = 2
            while (i + 1 < cols.size) {
                val series = cols[i].trim()
                val pts = cols[i + 1].trim().toIntOrNull()
                if (series.isNotEmpty() && pts != null) {
                    skillPoints.add(SkillPoint(series, pts))
                }
                i += 2
            }
            list.add(Decoration(index, name, slots, skillPoints))
        }
        decorations = list
    }

    private fun loadCharms() {
        val lines = readAssetLines("data/MHXX_CHARM.csv")
        val list = mutableListOf<Charm>()

        lines.forEachIndexed { index, line ->
            val cols = line.split(",")
            if (cols.size < 2) return@forEachIndexed
            val name = cols[0].trim()
            val slots = cols.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
            val skillPoints = mutableListOf<SkillPoint>()
            var i = 2
            while (i + 1 < cols.size) {
                val series = cols[i].trim()
                val pts = cols[i + 1].trim().toIntOrNull()
                if (series.isNotEmpty() && pts != null) {
                    skillPoints.add(SkillPoint(series, pts))
                }
                i += 2
            }
            list.add(Charm(index, name, slots, skillPoints))
        }
        charms = list
    }

    private fun loadConf() {
        try {
            categories = readAssetLines("conf/CATEGORY.txt")
        } catch (_: Exception) {
            categories = emptyList()
        }
        try {
            sibori = readAssetLines("conf/SIBORI.txt").mapNotNull { line ->
                val parts = line.split(",", limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else null
            }
        } catch (_: Exception) {
            sibori = emptyList()
        }
    }

    fun getAllEquipment(): List<Equipment> =
        equipmentByPart.values.flatten()

    fun findSkillByName(name: String): Skill? =
        skills.find { it.name == name }

    fun findSkillsBySeries(series: String): List<Skill> =
        skills.filter { it.series == series }
}
