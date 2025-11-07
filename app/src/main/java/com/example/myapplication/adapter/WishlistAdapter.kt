package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.myapplication.R
import com.example.myapplication.model.WishlistItem

class WishlistAdapter(
    private val onOpenLocation: (WishlistItem) -> Unit,
    private val onRemove: (WishlistItem) -> Unit
) : RecyclerView.Adapter<WishlistAdapter.WishlistViewHolder>() {

    private val items: MutableList<WishlistItem> = mutableListOf()

    fun submitList(newItems: List<WishlistItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WishlistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.wishlist_card, parent, false)
        return WishlistViewHolder(view, onOpenLocation, onRemove)
    }

    override fun onBindViewHolder(holder: WishlistViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class WishlistViewHolder(
        itemView: View,
        private val onOpenLocation: (WishlistItem) -> Unit,
        private val onRemove: (WishlistItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val cafeImage: ImageView = itemView.findViewById(R.id.cafe_image)
        private val cafeName: TextView = itemView.findViewById(R.id.cafe_name)
        private val cafeRating: TextView = itemView.findViewById(R.id.cafe_rating)
        private val cafeAddress: TextView = itemView.findViewById(R.id.cafe_address)
        private val categoriesRecycler: RecyclerView = itemView.findViewById(R.id.categories_recycle)
        private val locationButton: ImageButton = itemView.findViewById(R.id.location_btn)
        private val cancelButton: ImageButton = itemView.findViewById(R.id.cancel_btn)
        private val categoryAdapter = CategoryAdapter(R.layout.item_category_button_small)

        init {
            categoriesRecycler.layoutManager = LinearLayoutManager(
                itemView.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            categoriesRecycler.adapter = categoryAdapter
        }

        fun bind(item: WishlistItem) {
            val context = itemView.context
            val cafe = item.cafe
            cafeName.text = cafe.name
            cafeImage.contentDescription = cafe.name
            cafeImage.load(cafe.img_url) {
                crossfade(true)
                placeholder(R.drawable.contact2)
                error(R.drawable.contact2)
            }
            cafeRating.text = cafe.rating_avg?.let { rating ->
                context.getString(R.string.rating_format, rating)
            } ?: context.getString(R.string.rating_unavailable)
            cafeAddress.text = cafe.address?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.address_unavailable)
            categoryAdapter.submitList(cafe.tags)

            locationButton.setOnClickListener { onOpenLocation(item) }
            cancelButton.setOnClickListener { onRemove(item) }
        }
    }
}
