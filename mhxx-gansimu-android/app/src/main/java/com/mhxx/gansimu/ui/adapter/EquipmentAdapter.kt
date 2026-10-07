package com.mhxx.gansimu.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mhxx.gansimu.databinding.ItemEquipmentBinding
import com.mhxx.gansimu.model.Equipment

class EquipmentAdapter(
    private var items: List<Equipment>,
    private val onClick: (Equipment) -> Unit
) : RecyclerView.Adapter<EquipmentAdapter.VH>() {

    fun updateList(newList: List<Equipment>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemEquipmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    inner class VH(private val b: ItemEquipmentBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(eq: Equipment) {
            b.txtName.text = eq.name
            b.txtPart.text = eq.part.displayName
            b.txtRarity.text = "レア${eq.rarity}"
            b.txtSlots.text = "スロ${eq.slots}"
            b.txtSkills.text = eq.skills.joinToString(" ") { "${it.series}${if (it.points > 0) "+" else ""}${it.points}" }
            b.root.setOnClickListener { onClick(eq) }
        }
    }
}
