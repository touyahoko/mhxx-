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
import com.mhxx.gansimu.ui.adapter.SkillAdapter

/**
 * シミュレータタブ
 * スキル選択 → 装備検索 の基本UI
 * (元アプリの核心機能の入り口)
 */
class SimulatorFragment : Fragment() {

    private var _binding: FragmentSimulatorBinding? = null
    private val binding get() = _binding!!

    private lateinit var skillAdapter: SkillAdapter
    private val selectedSkills = mutableListOf<Skill>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSimulatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val repo = GanSimuApp.instance.repository

        // スキル系統スピナー
        val seriesList = listOf("（すべて）") + repo.skillSeries
        binding.spinnerSeries.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            seriesList
        )

        // スキル一覧
        skillAdapter = SkillAdapter(repo.skills) { skill ->
            if (selectedSkills.any { it.id == skill.id }) {
                selectedSkills.removeAll { it.id == skill.id }
                Toast.makeText(context, "${skill.name} を解除", Toast.LENGTH_SHORT).show()
            } else {
                selectedSkills.add(skill)
                Toast.makeText(context, "${skill.name} を追加", Toast.LENGTH_SHORT).show()
            }
            updateSelectedText()
        }
        binding.recyclerSkills.layoutManager = LinearLayoutManager(context)
        binding.recyclerSkills.adapter = skillAdapter

        binding.spinnerSeries.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val filtered = if (pos == 0) {
                    repo.skills
                } else {
                    val series = seriesList[pos]
                    repo.skills.filter { it.series == series }
                }
                skillAdapter.updateList(filtered)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        binding.btnSearch.setOnClickListener {
            if (selectedSkills.isEmpty()) {
                Toast.makeText(context, "スキルを選択してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // TODO: 本格的な装備組み合わせ探索アルゴリズムをここに実装
            // 元アプリの探索ロジック (c.g 等) を移植する必要あり
            val msg = buildString {
                append("検索条件:\n")
                selectedSkills.forEach { append("・${it.name} (${it.series} ${it.points})\n") }
                append("\n※装備探索ロジックは今後移植予定です")
            }
            binding.txtResult.text = msg
        }

        binding.btnClear.setOnClickListener {
            selectedSkills.clear()
            updateSelectedText()
            binding.txtResult.text = ""
        }

        // 統計表示
        binding.txtStats.text = buildString {
            append("装備: ${repo.getAllEquipment().size}件  ")
            append("スキル: ${repo.skills.size}件  ")
            append("装飾品: ${repo.decorations.size}件  ")
            append("お守り: ${repo.charms.size}件")
        }
    }

    private fun updateSelectedText() {
        binding.txtSelected.text = if (selectedSkills.isEmpty()) {
            "選択中のスキル: なし"
        } else {
            "選択中: " + selectedSkills.joinToString(", ") { it.name }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
