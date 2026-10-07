package com.mhxx.gansimu.ui.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mhxx.gansimu.GanSimuApp
import com.mhxx.gansimu.databinding.FragmentSimulatorBinding
import com.mhxx.gansimu.model.Skill
import com.mhxx.gansimu.search.EquipSearchEngine
import com.mhxx.gansimu.state.AppState
import com.mhxx.gansimu.state.MySetStorage
import com.mhxx.gansimu.ui.adapter.SkillAdapter
import kotlin.concurrent.thread

class SimulatorFragment : Fragment() {

    private var _binding: FragmentSimulatorBinding? = null
    private val binding get() = _binding!!
    private lateinit var skillAdapter: SkillAdapter
    private var searching = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSimulatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val repo = GanSimuApp.instance.repository

        val seriesList = listOf("（すべて）") + repo.skillSeries
        binding.spinnerSeries.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            seriesList
        )

        skillAdapter = SkillAdapter(repo.skills.filter { it.isPositive }) { skill ->
            if (AppState.selectedSkills.any { it.id == skill.id }) {
                AppState.selectedSkills.removeAll { it.id == skill.id }
                Toast.makeText(context, "${skill.name} を解除", Toast.LENGTH_SHORT).show()
            } else {
                AppState.selectedSkills.add(skill)
                Toast.makeText(context, "${skill.name} を追加", Toast.LENGTH_SHORT).show()
            }
            updateSelectedText()
        }
        binding.recyclerSkills.layoutManager = LinearLayoutManager(context)
        binding.recyclerSkills.adapter = skillAdapter

        binding.spinnerSeries.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val base = repo.skills.filter { it.isPositive }
                val filtered = if (pos == 0) base else base.filter { it.series == seriesList[pos] }
                skillAdapter.updateList(filtered)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        binding.btnSearch.setOnClickListener { runSearch() }
        binding.btnClear.setOnClickListener {
            AppState.selectedSkills.clear()
            updateSelectedText()
            binding.txtResult.text = ""
        }

        binding.txtStats.text = buildString {
            append("装備: ${repo.getAllEquipment().size}  ")
            append("スキル: ${repo.skills.size}  ")
            append("装飾品: ${repo.decorations.size}  ")
            append("除外: ${AppState.excludedKeys.size}  ")
            append("固定: ${AppState.fixedByPart.size}")
        }
        updateSelectedText()
    }

    private fun updateSelectedText() {
        binding.txtSelected.text = if (AppState.selectedSkills.isEmpty()) {
            "選択中のスキル: なし"
        } else {
            "選択中: " + AppState.selectedSkills.joinToString(", ") {
                "${it.name}(${it.series}${it.points})"
            }
        }
    }

    private fun runSearch() {
        if (searching) {
            Toast.makeText(context, "検索中です…", Toast.LENGTH_SHORT).show()
            return
        }
        if (AppState.selectedSkills.isEmpty()) {
            Toast.makeText(context, "スキルを選択してください", Toast.LENGTH_SHORT).show()
            return
        }
        searching = true
        binding.btnSearch.isEnabled = false
        binding.txtResult.text = "検索中…"

        val targets = EquipSearchEngine.targetsFromSkills(AppState.selectedSkills)
        val repo = GanSimuApp.instance.repository

        thread {
            val engine = EquipSearchEngine(repo)
            val results = try {
                engine.search(targets, maxResults = 20, maxCandidatesPerSlot = 35)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
            activity?.runOnUiThread {
                searching = false
                binding.btnSearch.isEnabled = true
                if (results.isEmpty()) {
                    binding.txtResult.text = buildString {
                        append("条件を満たす組み合わせが見つかりませんでした。\n")
                        append("目標: ")
                        append(targets.joinToString { "${it.skillName}(${it.series}≥${it.requiredPoints})" })
                        append("\n除外${AppState.excludedKeys.size}件 / 固定${AppState.fixedByPart.size}件 反映済み")
                    }
                } else {
                    val sb = StringBuilder()
                    sb.appendLine("見つかったセット: ${results.size}件\n")
                    results.forEachIndexed { i, r ->
                        sb.appendLine("【${i + 1}】 ${r.matchedSkills.joinToString()}")
                        r.set.parts().forEach { (part, eq) ->
                            sb.appendLine("  ${part.displayName}: ${eq?.name ?: "-"}")
                        }
                        val tops = r.skillTotals.filter { it.value != 0 }
                            .toList().sortedByDescending { it.second }.take(8)
                        sb.appendLine("  → " + tops.joinToString(" ") {
                            "${it.first}${if (it.second > 0) "+" else ""}${it.second}"
                        })
                        sb.appendLine()
                    }
                    binding.txtResult.text = sb.toString()

                    // 先頭をマイセット候補として保持できるように
                    if (results.isNotEmpty()) {
                        Toast.makeText(
                            context,
                            "${results.size}件ヒット。長押しでマイセット保存はマイセットタブから",
                            Toast.LENGTH_SHORT
                        ).show()
                        // 最新検索結果の先頭を一時保存
                        lastSearchResult = results.first().set.copy(name = "検索結果1")
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /** 直近の検索1位（マイセット保存用） */
        var lastSearchResult: com.mhxx.gansimu.model.EquipSet? = null
    }
}
