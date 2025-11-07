package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.model.Cafe

class CafeAdapter : RecyclerView.Adapter<CafeAdapter.CafeViewHolder>() {

    private val cafes: MutableList<Cafe> = mutableListOf()

    fun submitList(items: List<Cafe>) {
        cafes.clear()
        cafes.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CafeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recommended_cafe_card, parent, false)
        return CafeViewHolder(view)
    }

    override fun onBindViewHolder(holder: CafeViewHolder, position: Int) {
        holder.bind(cafes[position])
    }

    override fun getItemCount(): Int = cafes.size

    class CafeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cafeImage: ImageView = itemView.findViewById(R.id.cafe_image)
        private val cafeName: TextView = itemView.findViewById(R.id.cafe_name)
        private val categoriesRecycler: RecyclerView = itemView.findViewById(R.id.categories_recycle)
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
            cafeName.text = cafe.name
            val context = itemView.context
            val drawableName = cafe.img_url
            if (!drawableName.isNullOrBlank()) {
                val drawableRes = context.resources.getIdentifier(
                    drawableName,
                    "drawable",
                    context.packageName
                )
                if (drawableRes != 0) {
                    cafeImage.setImageResource(drawableRes)
                } else {
                    cafeImage.setImageResource(R.drawable.contact2)
                }
            } else {
                cafeImage.setImageResource(R.drawable.contact2)
            }
            categoryAdapter.submitList(cafe.tags)
        }
    }
}
