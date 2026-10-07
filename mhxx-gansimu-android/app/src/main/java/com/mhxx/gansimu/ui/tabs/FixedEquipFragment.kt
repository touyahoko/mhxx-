package com.mhxx.gansimu.ui.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mhxx.gansimu.GanSimuApp
import com.mhxx.gansimu.databinding.FragmentEquipListBinding
import com.mhxx.gansimu.model.EquipPart
import com.mhxx.gansimu.ui.adapter.EquipmentAdapter

/** 固定装備設定タブ */
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
        binding.txtTitle.text = "固定する装備を選択"

        val parts = listOf("すべて") + EquipPart.entries.map { it.displayName }
        binding.spinnerPart.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, parts)

        val adapter = EquipmentAdapter(repo.getAllEquipment()) { }
        binding.recycler.layoutManager = LinearLayoutManager(context)
        binding.recycler.adapter = adapter

        binding.spinnerPart.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val list = if (pos == 0) repo.getAllEquipment()
                else repo.equipmentByPart[EquipPart.entries[pos - 1]] ?: emptyList()
                adapter.updateList(list)
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
