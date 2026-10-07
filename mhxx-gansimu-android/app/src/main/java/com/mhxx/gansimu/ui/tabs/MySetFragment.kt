package com.mhxx.gansimu.ui.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mhxx.gansimu.databinding.FragmentSimpleListBinding

/** マイセットタブ (保存機能は今後実装) */
class MySetFragment : Fragment() {

    private var _binding: FragmentSimpleListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSimpleListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.txtTitle.text = "マイセット"
        binding.txtEmpty.visibility = View.VISIBLE
        binding.txtEmpty.text = "保存した装備セットがここに表示されます。\n（機能は今後実装予定）"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
