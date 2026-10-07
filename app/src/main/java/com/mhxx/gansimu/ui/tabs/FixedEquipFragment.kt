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
import com.mhxx.gansimu.databinding.FragmentEquipListBinding
import com.mhxx.gansimu.model.EquipPart
import com.mhxx.gansimu.state.AppState
import com.mhxx.gansimu.ui.adapter.EquipmentAdapter

/** 固定装備設定 - タップでその部位を固定（検索に反映） */
class FixedEquipFragment : Fragment() {

    private var _binding: FragmentEquipListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEquipListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val repo = GanSimuApp.instance.repository
        updateTitle()

        val parts = listOf("すべて") + EquipPart.entries.map { it.displayName }
        binding.spinnerPart.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, parts)

        val adapter = EquipmentAdapter(repo.getAllEquipment()) { eq ->
            val current = AppState.getFixed(eq.part)
            if (current?.name == eq.name) {
                AppState.clearFixed(eq.part)
                Toast.makeText(context, "${eq.part.displayName}の固定を解除", Toast.LENGTH_SHORT).show()
            } else {
                AppState.setFixed(eq)
                Toast.makeText(context, "${eq.part.displayName}を ${eq.name} に固定", Toast.LENGTH_SHORT).show()
            }
            updateTitle()
        }
        binding.recycler.layoutManager = LinearLayoutManager(context)
        binding.recycler.adapter = adapter

        binding.spinnerPart.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val list = if (pos == 0) repo.getAllEquipment()
                else repo.equipmentByPart[EquipPart.entries[pos - 1]] ?: emptyList()
                adapter.updateList(list.filter { AppState.matchesType(it) })
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        })
    }

    private fun updateTitle() {
        val fixed = AppState.fixedByPart.entries.joinToString(" ") {
            "${it.key.displayName}:${it.value.name}"
        }
        binding.txtTitle.text = if (fixed.isEmpty()) {
            "固定装備（タップで固定/解除） なし"
        } else {
            "固定中: $fixed"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
