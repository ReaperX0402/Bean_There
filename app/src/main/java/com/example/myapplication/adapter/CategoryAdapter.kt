package com.example.myapplication.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.model.Tag
import com.google.android.material.button.MaterialButton

class CategoryAdapter(
    @LayoutRes private val itemLayout: Int = R.layout.item_category_button,
    private val onTagClicked: ((Tag) -> Unit)? = null
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private val categories: MutableList<Tag> = mutableListOf()
    private var selectedTagId: String? = null

    fun submitList(items: List<Tag>) {
        categories.clear()
        categories.addAll(items)
        if (selectedTagId != null && categories.none { it.tag_id == selectedTagId }) {
            selectedTagId = null
        }
        notifyDataSetChanged()
    }

    fun setSelectedTag(tagId: String?) {
        if (selectedTagId == tagId) return
        selectedTagId = tagId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(itemLayout, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val tag = categories[position]
        holder.bind(tag, tag.tag_id == selectedTagId, onTagClicked)
    }

    override fun getItemCount(): Int = categories.size

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val button: MaterialButton = itemView as MaterialButton

        fun bind(tag: Tag, isSelected: Boolean, onTagClicked: ((Tag) -> Unit)?) {
            button.text = tag.tag_name

            val context = button.context
            val backgroundColorRes = if (isSelected) R.color.matcha else R.color.light_yellow
            val textColorRes = if (isSelected) R.color.white else R.color.black

            button.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, backgroundColorRes)
            )
            button.setTextColor(ContextCompat.getColor(context, textColorRes))
            button.strokeColor = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.coffee_brown)
            )
            button.isSelected = isSelected

            if (onTagClicked != null) {
                button.isClickable = true
                button.isFocusable = true
                button.setOnClickListener { onTagClicked(tag) }
            } else {
                button.isClickable = false
                button.isFocusable = false
                button.setOnClickListener(null)
            }
        }
    }
}
