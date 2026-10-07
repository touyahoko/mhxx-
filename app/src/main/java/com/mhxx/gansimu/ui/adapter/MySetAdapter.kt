package com.mhxx.gansimu.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mhxx.gansimu.databinding.ItemSimpleBinding
import com.mhxx.gansimu.model.EquipSet

class MySetAdapter(
    private var items: List<EquipSet>,
    private val onClick: (EquipSet) -> Unit
) : RecyclerView.Adapter<MySetAdapter.VH>() {

    fun updateList(newList: List<EquipSet>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSimpleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    inner class VH(private val b: ItemSimpleBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(set: EquipSet) {
            b.txtTitle.text = set.name
            val parts = set.parts().mapNotNull { it.second?.name }.joinToString(" / ")
            val skills = set.skillTotals().filter { it.value != 0 }
                .toList().sortedByDescending { it.second }.take(5)
                .joinToString(" ") { "${it.first}${if (it.second > 0) "+" else ""}${it.second}" }
            b.txtSubtitle.text = "$parts\n$skills"
            b.root.setOnClickListener { onClick(set) }
        }
    }
}
