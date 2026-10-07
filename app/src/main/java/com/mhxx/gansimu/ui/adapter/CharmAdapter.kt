package com.mhxx.gansimu.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mhxx.gansimu.databinding.ItemSimpleBinding
import com.mhxx.gansimu.model.Charm
import com.mhxx.gansimu.state.AppState

class CharmAdapter(
    private val items: List<Charm>,
    private val onClick: ((Charm) -> Unit)? = null
) : RecyclerView.Adapter<CharmAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSimpleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    inner class VH(private val b: ItemSimpleBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(c: Charm) {
            val selected = AppState.selectedCharm?.id == c.id
            b.txtTitle.text = (if (selected) "✓ " else "") + c.name
            b.txtSubtitle.text = "スロット${c.slots}  " +
                c.skills.joinToString(" ") { "${it.series}${if (it.points > 0) "+" else ""}${it.points}" }
            b.root.setOnClickListener { onClick?.invoke(c) }
        }
    }
}
