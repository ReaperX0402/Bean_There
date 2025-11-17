package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.myapplication.R
import com.example.myapplication.model.CartItem
import com.google.android.material.button.MaterialButton

class CartItemAdapter(
    private val onQuantityChanged: (CartItem, Int) -> Unit,
    private val onRemoveItem: (CartItem) -> Unit
) : RecyclerView.Adapter<CartItemAdapter.CartItemViewHolder>() {

    private val items: MutableList<CartItem> = mutableListOf()

    fun submitList(newItems: List<CartItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart_entry, parent, false)
        return CartItemViewHolder(view, onQuantityChanged, onRemoveItem)
    }

    override fun onBindViewHolder(holder: CartItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class CartItemViewHolder(
        itemView: View,
        private val onQuantityChanged: (CartItem, Int) -> Unit,
        private val onRemoveItem: (CartItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val itemImage: ImageView = itemView.findViewById(R.id.cart_item_image)
        private val itemName: TextView = itemView.findViewById(R.id.cart_item_name)
        private val itemPrice: TextView = itemView.findViewById(R.id.cart_item_price)
        private val itemTotal: TextView = itemView.findViewById(R.id.cart_item_total)
        private val quantityText: TextView = itemView.findViewById(R.id.cart_item_quantity)
        private val increaseButton: MaterialButton = itemView.findViewById(R.id.cart_item_increase)
        private val decreaseButton: MaterialButton = itemView.findViewById(R.id.cart_item_decrease)
        private val removeButton: MaterialButton = itemView.findViewById(R.id.cart_item_remove)

        fun bind(item: CartItem) {
            val context = itemView.context
            itemName.text = item.itemName
            itemPrice.text = context.getString(R.string.order_price_format, item.price)
            itemTotal.text = context.getString(R.string.order_price_format, item.lineTotal)
            quantityText.text = item.quantity.toString()
            itemImage.load(item.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.contact2)
                error(R.drawable.contact2)
            }

            increaseButton.setOnClickListener {
                onQuantityChanged(item, item.quantity + 1)
            }
            decreaseButton.setOnClickListener {
                onQuantityChanged(item, item.quantity - 1)
            }
            removeButton.setOnClickListener { onRemoveItem(item) }
        }
    }
}
