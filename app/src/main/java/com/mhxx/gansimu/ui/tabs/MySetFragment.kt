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
import com.mhxx.gansimu.model.EquipSet
import com.mhxx.gansimu.state.AppState
import com.mhxx.gansimu.state.MySetStorage
import com.mhxx.gansimu.ui.adapter.MySetAdapter

/** マイセット - 保存/表示/削除 */
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

        // 読込
        if (AppState.mySets.isEmpty()) {
            AppState.mySets.clear()
            AppState.mySets.addAll(storage.loadAll(repo))
        }

        adapter = MySetAdapter(
            items = AppState.mySets.toList(),
            onClick = { set ->
                AlertDialog.Builder(requireContext())
                    .setTitle(set.name)
                    .setMessage(set.summary())
                    .setPositiveButton("閉じる", null)
                    .setNegativeButton("削除") { _, _ ->
                        AppState.mySets.removeAll { it.name == set.name && it.head?.name == set.head?.name }
                        storage.saveAll(AppState.mySets)
                        refresh()
                        Toast.makeText(context, "削除しました", Toast.LENGTH_SHORT).show()
                    }
                    .show()
            }
        )
        binding.recycler.layoutManager = LinearLayoutManager(context)
        binding.recycler.adapter = adapter

        binding.txtTitle.text = "マイセット（長押しで検索結果を保存）"
        binding.txtTitle.setOnLongClickListener {
            saveLastSearch()
            true
        }
        binding.txtTitle.setOnClickListener {
            // 通常タップでも保存ダイアログ
            saveLastSearch()
        }

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
            setTextColor(0xFFE8EAF0.toInt())
            setHintTextColor(0xFF8B90A5.toInt())
        }
        AlertDialog.Builder(requireContext())
            .setTitle("マイセットに保存")
            .setView(input)
            .setPositiveButton("保存") { _, _ ->
                val name = input.text.toString().ifBlank { "マイセット" }
                val set = candidate.copy(name = name)
                AppState.mySets.add(0, set)
                storage.saveAll(AppState.mySets)
                refresh()
                Toast.makeText(context, "「$name」を保存しました", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun refresh() {
        if (AppState.mySets.isEmpty()) {
            binding.txtEmpty.visibility = View.VISIBLE
            binding.txtEmpty.text = "保存したセットがありません。\nシミュレータで検索後、このタイトルをタップで保存できます。"
            binding.txtEmpty.setTextColor(0xFF8B90A5.toInt())
        } else {
            binding.txtEmpty.visibility = View.GONE
        }
        adapter.updateList(AppState.mySets.toList())
        binding.txtTitle.text = "マイセット (${AppState.mySets.size}件) — タップで保存 / 項目タップで詳細"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
