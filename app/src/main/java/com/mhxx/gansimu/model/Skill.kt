package com.mhxx.gansimu.model

/**
 * スキル定義 (MHXX_SKILL.csv)
 * スキル名, スキル系統, ポイント, タイプ(0=両方,1=剣士,2=ガンナー)
 */
data class Skill(
    val id: Int,
    val name: String,           // スキル名 (例: 毒耐性)
    val series: String,         // スキル系統 (例: 毒)
    val points: Int,            // ポイント (正=発動, 負=マイナススキル)
    val type: Int               // 0=両方, 1=剣士, 2=ガンナー
) {
    val isPositive: Boolean get() = points > 0
    val isSwordsman: Boolean get() = type == 0 || type == 1
    val isGunner: Boolean get() = type == 0 || type == 2
}
