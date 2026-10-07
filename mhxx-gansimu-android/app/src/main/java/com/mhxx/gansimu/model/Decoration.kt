package com.mhxx.gansimu.model

/**
 * 装飾品 (MHXX_DECO.csv)
 */
data class Decoration(
    val id: Int,
    val name: String,
    val slotsRequired: Int,     // 必要スロット数
    val skills: List<SkillPoint>
)
