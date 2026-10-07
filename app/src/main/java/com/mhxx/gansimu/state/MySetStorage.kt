package com.mhxx.gansimu.state

import android.content.Context
import com.mhxx.gansimu.data.DataRepository
import com.mhxx.gansimu.model.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * マイセットの保存・読込 (SharedPreferences + JSON)
 */
class MySetStorage(context: Context) {
    private val prefs = context.getSharedPreferences("my_sets", Context.MODE_PRIVATE)

    fun saveAll(sets: List<EquipSet>) {
        val arr = JSONArray()
        sets.forEach { set ->
            arr.put(setToJson(set))
        }
        prefs.edit().putString("sets", arr.toString()).apply()
    }

    fun loadAll(repo: DataRepository): List<EquipSet> {
        val raw = prefs.getString("sets", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                jsonToSet(arr.getJSONObject(i), repo)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun setToJson(set: EquipSet): JSONObject {
        return JSONObject().apply {
            put("name", set.name)
            put("head", set.head?.name ?: "")
            put("body", set.body?.name ?: "")
            put("arm", set.arm?.name ?: "")
            put("wst", set.wst?.name ?: "")
            put("leg", set.leg?.name ?: "")
            put("charmName", set.charmName)
            put("charmSlots", set.charmSlots)
            val skills = JSONArray()
            set.charmSkills.forEach { sp ->
                skills.put(JSONObject().put("series", sp.series).put("points", sp.points))
            }
            put("charmSkills", skills)
        }
    }

    private fun jsonToSet(obj: JSONObject, repo: DataRepository): EquipSet? {
        fun find(part: EquipPart, name: String): Equipment? {
            if (name.isEmpty()) return null
            return repo.equipmentByPart[part]?.find { it.name == name }
        }
        val charmSkills = mutableListOf<SkillPoint>()
        val arr = obj.optJSONArray("charmSkills")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                charmSkills.add(SkillPoint(o.getString("series"), o.getInt("points")))
            }
        }
        return EquipSet(
            name = obj.optString("name", "マイセット"),
            head = find(EquipPart.HEAD, obj.optString("head")),
            body = find(EquipPart.BODY, obj.optString("body")),
            arm = find(EquipPart.ARM, obj.optString("arm")),
            wst = find(EquipPart.WST, obj.optString("wst")),
            leg = find(EquipPart.LEG, obj.optString("leg")),
            charmName = obj.optString("charmName", ""),
            charmSlots = obj.optInt("charmSlots", 0),
            charmSkills = charmSkills
        )
    }
}
