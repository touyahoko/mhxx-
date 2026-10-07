package com.mhxx.gansimu.state

import com.mhxx.gansimu.model.*

object AppState {
    val excludedKeys = mutableSetOf<String>()
    val fixedByPart = mutableMapOf<EquipPart, Equipment>()
    val excludedDecoNames = mutableSetOf<String>()

    /** CSVから選んだお守り */
    var selectedCharm: Charm? = null

    /**
     * 任意入力のお守り（元デスクトップ版相当）
     * 設定時は selectedCharm より優先
     */
    var customCharm: Charm? = null

    var useCharm: Boolean = true

    val selectedSkills = mutableListOf<Skill>()
    val mySets = mutableListOf<EquipSet>()
    var typeFilter: Int = 0
    var genderFilter: Int = 0

    fun equipKey(eq: Equipment): String = "${eq.part.name}:${eq.name}"
    fun isExcluded(eq: Equipment): Boolean = equipKey(eq) in excludedKeys

    fun toggleExclude(eq: Equipment): Boolean {
        val key = equipKey(eq)
        return if (key in excludedKeys) {
            excludedKeys.remove(key); false
        } else {
            excludedKeys.add(key); true
        }
    }

    fun setFixed(eq: Equipment) {
        fixedByPart[eq.part] = eq
        excludedKeys.remove(equipKey(eq))
    }

    fun clearFixed(part: EquipPart) = fixedByPart.remove(part)
    fun getFixed(part: EquipPart): Equipment? = fixedByPart[part]

    fun matchesType(eq: Equipment): Boolean = when (typeFilter) {
        1 -> eq.isSwordsman
        2 -> eq.isGunner
        else -> true
    }

    fun matchesGender(eq: Equipment): Boolean = when (genderFilter) {
        1 -> eq.gender == 0 || eq.gender == 1
        2 -> eq.gender == 0 || eq.gender == 2
        else -> true
    }

    fun isDecoExcluded(name: String): Boolean = name in excludedDecoNames

    /** 検索に使う実効お守り（カスタム優先） */
    fun effectiveCharm(): Charm? {
        if (!useCharm) return null
        return customCharm ?: selectedCharm
    }

    fun charmDescription(): String {
        val c = effectiveCharm() ?: return "なし"
        val skills = c.skills.joinToString(" ") {
            "${it.series}${if (it.points > 0) "+" else ""}${it.points}"
        }
        return "${c.name} スロ${c.slots} $skills"
    }
}
