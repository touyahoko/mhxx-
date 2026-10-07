package com.mhxx.gansimu.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mhxx.gansimu.databinding.ItemSkillBinding
import com.mhxx.gansimu.model.Skill

class SkillAdapter(
    private var items: List<Skill>,
    private val onClick: (Skill) -> Unit
) : RecyclerView.Adapter<SkillAdapter.VH>() {

    fun updateList(newList: List<Skill>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSkillBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    inner class VH(private val b: ItemSkillBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(skill: Skill) {
            b.txtName.text = skill.name
            b.txtSeries.text = skill.series
            b.txtPoints.text = if (skill.points > 0) "+${skill.points}" else "${skill.points}"
            b.txtType.text = when (skill.type) {
                1 -> "剣士"
                2 -> "ガンナー"
                else -> "両方"
            }
            b.root.setOnClickListener { onClick(skill) }
        }
    }
}
