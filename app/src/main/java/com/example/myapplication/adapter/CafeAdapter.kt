package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.myapplication.R
import com.example.myapplication.model.Cafe
import com.google.android.material.button.MaterialButton

class CafeAdapter(
    private val onViewDetails: (Cafe) -> Unit
) : RecyclerView.Adapter<CafeAdapter.CafeViewHolder>() {

    private val cafes: MutableList<Cafe> = mutableListOf()

    fun submitList(items: List<Cafe>) {
        cafes.clear()
        cafes.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CafeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recommended_cafe_card, parent, false)
        return CafeViewHolder(view, onViewDetails)
    }

    override fun onBindViewHolder(holder: CafeViewHolder, position: Int) {
        holder.bind(cafes[position])
    }

    override fun getItemCount(): Int = cafes.size

    class CafeViewHolder(
        itemView: View,
        private val onViewDetails: (Cafe) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val cafeImage: ImageView = itemView.findViewById(R.id.cafe_image)
        private val cafeName: TextView = itemView.findViewById(R.id.cafe_name)
        private val cafeRating: TextView = itemView.findViewById(R.id.cafe_rating)
        private val cafeAddress: TextView = itemView.findViewById(R.id.cafe_address)
        private val categoriesRecycler: RecyclerView = itemView.findViewById(R.id.categories_recycle)
        private val viewDetailsButton: MaterialButton = itemView.findViewById(R.id.view_details_btn)
        private val categoryAdapter = CategoryAdapter(R.layout.item_category_button_small)

        init {
            categoriesRecycler.layoutManager = LinearLayoutManager(
                itemView.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            categoriesRecycler.adapter = categoryAdapter
        }

        fun bind(cafe: Cafe) {
            val context = itemView.context
            cafeName.text = cafe.name
            cafeImage.contentDescription = cafe.name
            cafeImage.load(cafe.img_url) {
                crossfade(true)
                placeholder(R.drawable.contact2)
                error(R.drawable.contact2)
            }
            val ratingText = cafe.rating_avg?.let { rating ->
                context.getString(R.string.rating_format, rating)
            } ?: context.getString(R.string.rating_unavailable)
            cafeRating.text = ratingText
            cafeAddress.text = cafe.address?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.address_unavailable)
            categoryAdapter.submitList(cafe.tags)
            viewDetailsButton.setOnClickListener { onViewDetails(cafe) }
        }
    }
}
