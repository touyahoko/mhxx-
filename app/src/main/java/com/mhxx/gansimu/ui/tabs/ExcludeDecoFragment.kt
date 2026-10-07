package com.mhxx.gansimu.ui.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mhxx.gansimu.GanSimuApp
import com.mhxx.gansimu.databinding.FragmentSimpleListBinding
import com.mhxx.gansimu.state.AppState
import com.mhxx.gansimu.ui.adapter.DecorationAdapter

class ExcludeDecoFragment : Fragment() {
    private var _binding: FragmentSimpleListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSimpleListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val repo = GanSimuApp.instance.repository
        binding.txtTitle.text = "装飾品除外（タップで切替） 除外: ${AppState.excludedDecoNames.size}"
        binding.recycler.layoutManager = LinearLayoutManager(context)
        binding.recycler.adapter = DecorationAdapter(repo.decorations) { deco ->
            if (deco.name in AppState.excludedDecoNames) {
                AppState.excludedDecoNames.remove(deco.name)
                Toast.makeText(context, "${deco.name} の除外を解除", Toast.LENGTH_SHORT).show()
            } else {
                AppState.excludedDecoNames.add(deco.name)
                Toast.makeText(context, "${deco.name} を除外", Toast.LENGTH_SHORT).show()
            }
            binding.txtTitle.text = "装飾品除外（タップで切替） 除外: ${AppState.excludedDecoNames.size}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
