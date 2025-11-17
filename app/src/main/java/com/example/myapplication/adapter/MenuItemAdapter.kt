package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.model.MenuItem
import com.google.android.material.button.MaterialButton

class MenuItemAdapter(
    private val onAddItem: (MenuItem) -> Unit
) : RecyclerView.Adapter<MenuItemAdapter.MenuItemViewHolder>() {

    private val items: MutableList<MenuItem> = mutableListOf()

    fun submitList(newItems: List<MenuItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu_entry, parent, false)
        return MenuItemViewHolder(view, onAddItem)
    }

    override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class MenuItemViewHolder(
        itemView: View,
        private val onAddItem: (MenuItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val itemName: TextView = itemView.findViewById(R.id.menu_item_name)
        private val itemDescription: TextView = itemView.findViewById(R.id.menu_item_description)
        private val itemPrice: TextView = itemView.findViewById(R.id.menu_item_price)
        private val addButton: MaterialButton = itemView.findViewById(R.id.menu_item_add_button)

        fun bind(item: MenuItem) {
            val context = itemView.context
            itemName.text = item.itemName
            val descriptionText = item.description?.takeIf { it.isNotBlank() }
                ?: item.text?.takeIf { it.isNotBlank() }
            itemDescription.isVisible = !descriptionText.isNullOrBlank()
            itemDescription.text = descriptionText.orEmpty()
            itemPrice.text = context.getString(R.string.order_price_format, item.price)

            val available = item.isAvailable
            addButton.isEnabled = available
            addButton.text = if (available) {
                context.getString(R.string.order_add_to_cart)
            } else {
                context.getString(R.string.order_unavailable)
            }
            addButton.setOnClickListener {
                if (available) {
                    onAddItem(item)
                }
            }
        }
    }
}
