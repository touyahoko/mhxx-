package com.mhxx.gansimu.state

import com.mhxx.gansimu.model.EquipPart
import com.mhxx.gansimu.model.EquipSet
import com.mhxx.gansimu.model.Equipment
import com.mhxx.gansimu.model.Skill

/**
 * アプリ全体の状態（除外・固定・選択スキル・マイセット）
 */
object AppState {
    /** 除外する装備キー: "HEAD:名前" */
    val excludedKeys = mutableSetOf<String>()

    /** 部位ごとの固定装備 */
    val fixedByPart = mutableMapOf<EquipPart, Equipment>()

    /** 除外する装飾品名 */
    val excludedDecoNames = mutableSetOf<String>()

    /** 選択中スキル（発動スキル） */
    val selectedSkills = mutableListOf<Skill>()

    /** マイセット一覧 */
    val mySets = mutableListOf<EquipSet>()

    /** タイプフィルタ 0=両方 1=剣士 2=ガンナー */
    var typeFilter: Int = 0

    fun equipKey(eq: Equipment): String = "${eq.part.name}:${eq.name}"

    fun isExcluded(eq: Equipment): Boolean = equipKey(eq) in excludedKeys

    fun toggleExclude(eq: Equipment): Boolean {
        val key = equipKey(eq)
        return if (key in excludedKeys) {
            excludedKeys.remove(key)
            false
        } else {
            excludedKeys.add(key)
            true
        }
    }

    fun setFixed(eq: Equipment) {
        fixedByPart[eq.part] = eq
        // 固定したら除外からは外す
        excludedKeys.remove(equipKey(eq))
    }

    fun clearFixed(part: EquipPart) {
        fixedByPart.remove(part)
    }

    fun getFixed(part: EquipPart): Equipment? = fixedByPart[part]

    fun matchesType(eq: Equipment): Boolean = when (typeFilter) {
        1 -> eq.isSwordsman
        2 -> eq.isGunner
        else -> true
    }
}
