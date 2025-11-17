package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.model.MenuItem
import com.example.myapplication.model.MenuSection

class MenuSectionAdapter(
    private val onAddItem: (MenuSection, MenuItem) -> Unit
) : RecyclerView.Adapter<MenuSectionAdapter.MenuSectionViewHolder>() {

    private val sections: MutableList<MenuSection> = mutableListOf()

    fun submitList(newSections: List<MenuSection>) {
        sections.clear()
        sections.addAll(newSections)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuSectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu_section, parent, false)
        return MenuSectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuSectionViewHolder, position: Int) {
        holder.bind(sections[position])
    }

    override fun getItemCount(): Int = sections.size

    inner class MenuSectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cafeName: TextView = itemView.findViewById(R.id.menu_cafe_name)
        private val menuTitle: TextView = itemView.findViewById(R.id.menu_title)
        private val menuDescription: TextView = itemView.findViewById(R.id.menu_description)
        private val itemsRecycler: RecyclerView = itemView.findViewById(R.id.menu_items_recycler)
        private val itemAdapter = MenuItemAdapter { menuItem ->
            val position = bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) return@MenuItemAdapter
            val section = sections.getOrNull(position)
            if (section != null) {
                onAddItem(section, menuItem)
            }
        }

        init {
            itemsRecycler.layoutManager = LinearLayoutManager(itemView.context)
            itemsRecycler.adapter = itemAdapter
            itemsRecycler.isNestedScrollingEnabled = false
        }

        fun bind(section: MenuSection) {
            cafeName.text = section.cafeName
            menuTitle.text = section.menuName
            menuDescription.isVisible = !section.menuDescription.isNullOrBlank()
            menuDescription.text = section.menuDescription
            itemAdapter.submitList(section.items)
        }
    }
}
