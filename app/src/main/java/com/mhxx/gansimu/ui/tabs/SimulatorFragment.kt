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
import com.mhxx.gansimu.model.EquipSet
import com.mhxx.gansimu.search.EquipSearchEngine
import com.mhxx.gansimu.state.AppState
import com.mhxx.gansimu.ui.adapter.SkillAdapter
import kotlin.concurrent.thread

/**
 * シミュレータ本体
 * スキル選択 → 装備+お守り+装飾品の組み合わせ探索
 */
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
            } else {
                AppState.selectedSkills.add(skill)
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

        refreshStats()
        updateSelectedText()
    }

    override fun onResume() {
        super.onResume()
        refreshStats()
    }

    private fun refreshStats() {
        val repo = GanSimuApp.instance.repository
        val charm = AppState.charmDescription()
        binding.txtStats.text = buildString {
            append("装備${repo.getAllEquipment().size} スキル${repo.skills.size} 装飾${repo.decorations.size}  ")
            append("除外${AppState.excludedKeys.size} 固定${AppState.fixedByPart.size}  ")
            append("お守り:$charm")
        }
    }

    private fun updateSelectedText() {
        binding.txtSelected.text = if (AppState.selectedSkills.isEmpty()) {
            "選択中のスキル: なし（一覧をタップして追加）"
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
        binding.txtResult.text = "検索中…（除外・固定・お守り・装飾品を反映）"

        val targets = EquipSearchEngine.targetsFromSkills(AppState.selectedSkills)
        val repo = GanSimuApp.instance.repository

        thread {
            val t0 = System.currentTimeMillis()
            val results = try {
                EquipSearchEngine(repo).search(targets, maxResults = 25, maxCandidatesPerSlot = 40)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
            val ms = System.currentTimeMillis() - t0

            activity?.runOnUiThread {
                searching = false
                binding.btnSearch.isEnabled = true
                if (results.isEmpty()) {
                    binding.txtResult.text = buildString {
                        appendLine("条件を満たす組み合わせが見つかりませんでした。")
                        appendLine("目標: " + targets.joinToString { "${it.skillName}(${it.series}≥${it.requiredPoints})" })
                        appendLine("除外${AppState.excludedKeys.size} / 固定${AppState.fixedByPart.size} / 装飾除外${AppState.excludedDecoNames.size}")
                        append("お守り: ${AppState.charmDescription()}")
                    }
                } else {
                    val sb = StringBuilder()
                    sb.appendLine("ヒット ${results.size}件  (${ms}ms)\n")
                    results.forEachIndexed { i, r ->
                        sb.appendLine("━━ 【${i + 1}】 ${r.matchedSkills.joinToString()}")
                        r.set.parts().forEach { (part, eq) ->
                            sb.appendLine("  ${part.displayName}: ${eq?.name ?: "-"}")
                        }
                        if (r.set.charmName.isNotEmpty()) {
                            sb.appendLine("  守: ${r.set.charmName}")
                        }
                        if (r.usedDecos.isNotEmpty()) {
                            sb.appendLine("  珠: " + r.usedDecos.joinToString { "${it.first}×${it.second}" })
                        }
                        val tops = r.skillTotals.filter { it.value != 0 }
                            .toList().sortedByDescending { it.second }.take(10)
                        sb.appendLine("  → " + tops.joinToString(" ") {
                            "${it.first}${if (it.second > 0) "+" else ""}${it.second}"
                        })
                        sb.appendLine("  残スロ: ${r.remainingSlots}")
                        sb.appendLine()
                    }
                    binding.txtResult.text = sb.toString()
                    lastSearchResult = results.first().set.copy(name = "検索結果1")
                    lastSearchResults = results.map { it.set }
                    Toast.makeText(context, "${results.size}件ヒット", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        var lastSearchResult: EquipSet? = null
        var lastSearchResults: List<EquipSet> = emptyList()
    }
}
