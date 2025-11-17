package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.model.CafeMenuGroup

class CafeSelectionAdapter(
    private val onCafeSelected: (CafeMenuGroup) -> Unit
) : RecyclerView.Adapter<CafeSelectionAdapter.CafeViewHolder>() {

    private val cafes: MutableList<CafeMenuGroup> = mutableListOf()

    fun submitList(newCafes: List<CafeMenuGroup>) {
        cafes.clear()
        cafes.addAll(newCafes)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CafeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cafe_entry, parent, false)
        return CafeViewHolder(view)
    }

    override fun onBindViewHolder(holder: CafeViewHolder, position: Int) {
        holder.bind(cafes[position])
    }

    override fun getItemCount(): Int = cafes.size

    inner class CafeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cafeName: TextView = itemView.findViewById(R.id.cafe_entry_name)
        private val menuCount: TextView = itemView.findViewById(R.id.cafe_entry_menu_count)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener
                val cafe = cafes.getOrNull(position) ?: return@setOnClickListener
                onCafeSelected(cafe)
            }
        }

        fun bind(group: CafeMenuGroup) {
            val context = itemView.context
            cafeName.text = group.cafeName
            val count = group.menuCount
            menuCount.text = context.resources.getQuantityString(
                R.plurals.order_menu_count,
                count,
                count
            )
        }
    }
}
