package com.mhxx.gansimu.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mhxx.gansimu.databinding.ItemSimpleBinding
import com.mhxx.gansimu.model.Decoration

class DecorationAdapter(
    private val items: List<Decoration>
) : RecyclerView.Adapter<DecorationAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSimpleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    inner class VH(private val b: ItemSimpleBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(d: Decoration) {
            b.txtTitle.text = d.name
            b.txtSubtitle.text = "スロット${d.slotsRequired}  " +
                    d.skills.joinToString(" ") { "${it.series}${if (it.points > 0) "+" else ""}${it.points}" }
        }
    }
}
