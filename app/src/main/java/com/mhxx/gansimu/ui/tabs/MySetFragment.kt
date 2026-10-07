package com.mhxx.gansimu.ui.tabs

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mhxx.gansimu.GanSimuApp
import com.mhxx.gansimu.databinding.FragmentSimpleListBinding
import com.mhxx.gansimu.state.AppState
import com.mhxx.gansimu.state.MySetStorage
import com.mhxx.gansimu.ui.adapter.MySetAdapter

class MySetFragment : Fragment() {

    private var _binding: FragmentSimpleListBinding? = null
    private val binding get() = _binding!!
    private lateinit var storage: MySetStorage
    private lateinit var adapter: MySetAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSimpleListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        storage = MySetStorage(requireContext())
        val repo = GanSimuApp.instance.repository

        if (AppState.mySets.isEmpty()) {
            AppState.mySets.addAll(storage.loadAll(repo))
        }

        adapter = MySetAdapter(AppState.mySets.toList()) { set ->
            AlertDialog.Builder(requireContext())
                .setTitle(set.name)
                .setMessage(set.summary())
                .setPositiveButton("閉じる", null)
                .setNegativeButton("削除") { _, _ ->
                    AppState.mySets.remove(set)
                    storage.saveAll(AppState.mySets)
                    refresh()
                }
                .setNeutralButton("固定に反映") { _, _ ->
                    set.parts().forEach { (part, eq) ->
                        if (eq != null) AppState.setFixed(eq)
                    }
                    Toast.makeText(context, "各部位を固定装備に設定しました", Toast.LENGTH_SHORT).show()
                }
                .show()
        }
        binding.recycler.layoutManager = LinearLayoutManager(context)
        binding.recycler.adapter = adapter
        binding.txtTitle.setOnClickListener { saveLastSearch() }
        refresh()
    }

    private fun saveLastSearch() {
        val candidate = SimulatorFragment.lastSearchResult
        if (candidate == null) {
            Toast.makeText(context, "先にシミュレータで検索してください", Toast.LENGTH_SHORT).show()
            return
        }
        val input = EditText(requireContext()).apply {
            setText(candidate.name.ifEmpty { "マイセット${AppState.mySets.size + 1}" })
            setTextColor(0xFF202020.toInt())
        }
        AlertDialog.Builder(requireContext())
            .setTitle("マイセットに保存")
            .setView(input)
            .setPositiveButton("保存") { _, _ ->
                val name = input.text.toString().ifBlank { "マイセット" }
                AppState.mySets.add(0, candidate.copy(name = name))
                storage.saveAll(AppState.mySets)
                refresh()
                Toast.makeText(context, "「$name」を保存", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun refresh() {
        if (AppState.mySets.isEmpty()) {
            binding.txtEmpty.visibility = View.VISIBLE
            binding.txtEmpty.text = "保存セットなし\nシミュレータで検索後、このタイトルをタップで保存"
            binding.txtEmpty.setTextColor(0xFF606060.toInt())
        } else {
            binding.txtEmpty.visibility = View.GONE
        }
        adapter.updateList(AppState.mySets.toList())
        binding.txtTitle.text = "マイセット (${AppState.mySets.size}) — タップで検索結果を保存"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
