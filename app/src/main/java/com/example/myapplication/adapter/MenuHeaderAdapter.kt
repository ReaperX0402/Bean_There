package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.model.MenuSection

class MenuHeaderAdapter(
    private val onMenuSelected: (MenuSection) -> Unit
) : RecyclerView.Adapter<MenuHeaderAdapter.MenuHeaderViewHolder>() {

    private val menus: MutableList<MenuSection> = mutableListOf()

    fun submitList(newMenus: List<MenuSection>) {
        menus.clear()
        menus.addAll(newMenus)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuHeaderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu_header, parent, false)
        return MenuHeaderViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuHeaderViewHolder, position: Int) {
        holder.bind(menus[position])
    }

    override fun getItemCount(): Int = menus.size

    inner class MenuHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val menuName: TextView = itemView.findViewById(R.id.menu_header_name)
        private val menuDescription: TextView = itemView.findViewById(R.id.menu_header_description)
        private val itemCount: TextView = itemView.findViewById(R.id.menu_header_item_count)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener
                val menu = menus.getOrNull(position) ?: return@setOnClickListener
                onMenuSelected(menu)
            }
        }

        fun bind(section: MenuSection) {
            val context = itemView.context
            menuName.text = section.menuName
            val descriptionText = section.menuDescription
            menuDescription.isVisible = !descriptionText.isNullOrBlank()
            menuDescription.text = descriptionText
            val count = section.items.size
            itemCount.text = context.resources.getQuantityString(
                R.plurals.order_item_count,
                count,
                count
            )
        }
    }
}
