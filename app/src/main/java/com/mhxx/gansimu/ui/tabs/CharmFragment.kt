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
import com.mhxx.gansimu.databinding.FragmentCharmBinding
import com.mhxx.gansimu.model.Charm
import com.mhxx.gansimu.model.SkillPoint
import com.mhxx.gansimu.search.CharmCalculator
import com.mhxx.gansimu.search.EquipSearchEngine
import com.mhxx.gansimu.state.AppState
import com.mhxx.gansimu.ui.adapter.CharmAdapter
import kotlin.concurrent.thread

/**
 * お守り設定
 * - 任意スキル・ポイント・スロットの手入力（元デスクトップ版相当）
 * - 不足分からの必要お守り自動計算
 * - CSV一覧からの選択
 */
class CharmFragment : Fragment() {

    private var _binding: FragmentCharmBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCharmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val repo = GanSimuApp.instance.repository

        // 系統スピナー（先頭に「なし」）
        val seriesOptions = listOf("（なし）") + repo.skillSeries
        val seriesAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            seriesOptions
        )
        binding.spinnerSkill1.adapter = seriesAdapter
        binding.spinnerSkill2.adapter = seriesAdapter

        binding.spinnerSlots.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("0", "1", "2", "3")
        )
        binding.spinnerSlots.setSelection(3)

        // 既存カスタムがあれば反映
        AppState.customCharm?.let { c ->
            c.skills.getOrNull(0)?.let { sp ->
                val idx = seriesOptions.indexOf(sp.series).coerceAtLeast(0)
                binding.spinnerSkill1.setSelection(idx)
                binding.editPts1.setText(sp.points.toString())
            }
            c.skills.getOrNull(1)?.let { sp ->
                val idx = seriesOptions.indexOf(sp.series).coerceAtLeast(0)
                binding.spinnerSkill2.setSelection(idx)
                binding.editPts2.setText(sp.points.toString())
            }
            binding.spinnerSlots.setSelection(c.slots.coerceIn(0, 3))
        }

        binding.btnApplyCharm.setOnClickListener { applyCustomCharm(seriesOptions) }
        binding.btnClearCharm.setOnClickListener {
            AppState.customCharm = null
            AppState.selectedCharm = null
            binding.editPts1.setText("0")
            binding.editPts2.setText("0")
            binding.spinnerSkill1.setSelection(0)
            binding.spinnerSkill2.setSelection(0)
            updateCurrentLabel()
            Toast.makeText(context, "お守りをクリアしました", Toast.LENGTH_SHORT).show()
        }

        binding.btnCalcCharm.setOnClickListener { runAutoCalc() }

        // CSV一覧
        binding.recyclerCharms.layoutManager = LinearLayoutManager(context)
        binding.recyclerCharms.adapter = CharmAdapter(repo.charms) { charm ->
            AppState.selectedCharm = charm
            AppState.customCharm = null
            AppState.useCharm = true
            // フォームにも反映
            charm.skills.getOrNull(0)?.let { sp ->
                val idx = seriesOptions.indexOf(sp.series).coerceAtLeast(0)
                binding.spinnerSkill1.setSelection(idx)
                binding.editPts1.setText(sp.points.toString())
            } ?: run {
                binding.spinnerSkill1.setSelection(0)
                binding.editPts1.setText("0")
            }
            charm.skills.getOrNull(1)?.let { sp ->
                val idx = seriesOptions.indexOf(sp.series).coerceAtLeast(0)
                binding.spinnerSkill2.setSelection(idx)
                binding.editPts2.setText(sp.points.toString())
            } ?: run {
                binding.spinnerSkill2.setSelection(0)
                binding.editPts2.setText("0")
            }
            binding.spinnerSlots.setSelection(charm.slots.coerceIn(0, 3))
            updateCurrentLabel()
            Toast.makeText(context, "${charm.name} を選択", Toast.LENGTH_SHORT).show()
        }

        updateCurrentLabel()
    }

    private fun applyCustomCharm(seriesOptions: List<String>) {
        val s1 = binding.spinnerSkill1.selectedItemPosition
        val s2 = binding.spinnerSkill2.selectedItemPosition
        val pts1 = binding.editPts1.text.toString().toIntOrNull() ?: 0
        val pts2 = binding.editPts2.text.toString().toIntOrNull() ?: 0
        val slots = binding.spinnerSlots.selectedItemPosition.coerceIn(0, 3)

        val skills = mutableListOf<SkillPoint>()
        if (s1 > 0 && pts1 != 0) {
            skills.add(SkillPoint(seriesOptions[s1], pts1))
        }
        if (s2 > 0 && pts2 != 0) {
            skills.add(SkillPoint(seriesOptions[s2], pts2))
        }

        if (skills.isEmpty() && slots == 0) {
            AppState.customCharm = null
            Toast.makeText(context, "スキルもスロットも空です", Toast.LENGTH_SHORT).show()
            updateCurrentLabel()
            return
        }

        val name = buildString {
            append("カスタム")
            skills.forEach { append(" ${it.series}${if (it.points > 0) "+" else ""}${it.points}") }
            append(" スロ$slots")
        }

        AppState.customCharm = Charm(
            id = -1,
            name = name,
            slots = slots,
            skills = skills
        )
        AppState.selectedCharm = null
        AppState.useCharm = true
        updateCurrentLabel()
        Toast.makeText(context, "カスタムお守りを設定しました", Toast.LENGTH_SHORT).show()
    }

    private fun runAutoCalc() {
        if (AppState.selectedSkills.isEmpty()) {
            Toast.makeText(context, "先にシミュレータでスキルを選択してください", Toast.LENGTH_SHORT).show()
            return
        }
        binding.txtCharmCalc.text = "計算中…"
        binding.btnCalcCharm.isEnabled = false
        val targets = EquipSearchEngine.targetsFromSkills(AppState.selectedSkills)
        val repo = GanSimuApp.instance.repository

        thread {
            val result = try {
                CharmCalculator.calculate(repo, targets)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            activity?.runOnUiThread {
                binding.btnCalcCharm.isEnabled = true
                if (result == null) {
                    binding.txtCharmCalc.text = "計算に失敗しました"
                    return@runOnUiThread
                }
                binding.txtCharmCalc.text = result.detail

                // 不足がある場合はフォームに自動入力
                if (result.skills.isNotEmpty()) {
                    val seriesOptions = listOf("（なし）") + repo.skillSeries
                    result.skills.getOrNull(0)?.let { sp ->
                        val idx = seriesOptions.indexOf(sp.series).coerceAtLeast(0)
                        binding.spinnerSkill1.setSelection(idx)
                        binding.editPts1.setText(sp.points.toString())
                    }
                    result.skills.getOrNull(1)?.let { sp ->
                        val idx = seriesOptions.indexOf(sp.series).coerceAtLeast(0)
                        binding.spinnerSkill2.setSelection(idx)
                        binding.editPts2.setText(sp.points.toString())
                    } ?: run {
                        binding.spinnerSkill2.setSelection(0)
                        binding.editPts2.setText("0")
                    }
                    binding.spinnerSlots.setSelection(result.suggestedSlots.coerceIn(0, 3))
                    Toast.makeText(context, "不足分をフォームに入力しました。「このお守りを使用」で確定", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "お守りなしで達成可能です", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateCurrentLabel() {
        binding.txtCurrentCharm.text = "現在: ${AppState.charmDescription()}"
    }

    override fun onResume() {
        super.onResume()
        updateCurrentLabel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
