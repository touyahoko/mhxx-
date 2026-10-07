package com.mhxx.gansimu.model

/**
 * お守り (MHXX_CHARM.csv)
 */
data class Charm(
    val id: Int,
    val name: String,
    val slots: Int,
    val skills: List<SkillPoint>
)
