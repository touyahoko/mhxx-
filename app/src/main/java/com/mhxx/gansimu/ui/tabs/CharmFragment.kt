package com.mhxx.gansimu.ui.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mhxx.gansimu.GanSimuApp
import com.mhxx.gansimu.databinding.FragmentSimpleListBinding
import com.mhxx.gansimu.ui.adapter.CharmAdapter

/** お守り設定タブ */
class CharmFragment : Fragment() {

    private var _binding: FragmentSimpleListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSimpleListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val repo = GanSimuApp.instance.repository
        binding.txtTitle.text = "お守り一覧 (${repo.charms.size}件)"
        binding.recycler.layoutManager = LinearLayoutManager(context)
        binding.recycler.adapter = CharmAdapter(repo.charms)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
