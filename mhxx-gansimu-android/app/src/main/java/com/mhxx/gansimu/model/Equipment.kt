package com.mhxx.gansimu.model

/**
 * 装備定義 (MHXX_EQUIP_*.csv)
 * 名前,性別,タイプ,レア度,スロット数,生産時G級条件,... スキル系統1,スキル値1,...
 */
data class Equipment(
    val id: Int,
    val name: String,
    val part: EquipPart,        // HEAD / BODY / ARM / WST / LEG
    val gender: Int,            // 0=両, 1=男, 2=女
    val type: Int,              // 0=両方, 1=剣士, 2=ガンナー
    val rarity: Int,
    val slots: Int,             // スロット数 0-3
    val skills: List<SkillPoint>, // スキルポイント
    val defense: Int = 0,
    val fireRes: Int = 0,
    val waterRes: Int = 0,
    val thunderRes: Int = 0,
    val iceRes: Int = 0,
    val dragonRes: Int = 0
) {
    val isSwordsman: Boolean get() = type == 0 || type == 1
    val isGunner: Boolean get() = type == 0 || type == 2
}

data class SkillPoint(
    val series: String,
    val points: Int
)

enum class EquipPart(val displayName: String, val fileName: String) {
    HEAD("頭", "MHXX_EQUIP_HEAD.csv"),
    BODY("胴", "MHXX_EQUIP_BODY.csv"),
    ARM("腕", "MHXX_EQUIP_ARM.csv"),
    WST("腰", "MHXX_EQUIP_WST.csv"),
    LEG("脚", "MHXX_EQUIP_LEG.csv");

    companion object {
        fun fromFileName(name: String): EquipPart? =
            entries.find { it.fileName == name }
    }
}
