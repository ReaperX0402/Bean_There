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
import java.util.LinkedHashSet

class MultiSelectCategoryAdapter(
    @LayoutRes private val itemLayout: Int = R.layout.item_category_button,
    private val maxSelection: Int = DEFAULT_MAX_SELECTION,
    private val onSelectionChanged: ((List<Tag>) -> Unit)? = null,
    private val onSelectionLimitReached: (() -> Unit)? = null
) : RecyclerView.Adapter<MultiSelectCategoryAdapter.CategoryViewHolder>() {

    private val categories: MutableList<Tag> = mutableListOf()
    private val selectedTagIds: LinkedHashSet<String> = LinkedHashSet()

    fun submitList(items: List<Tag>) {
        categories.clear()
        categories.addAll(items)
        val iterator = selectedTagIds.iterator()
        var changed = false
        while (iterator.hasNext()) {
            val id = iterator.next()
            if (categories.none { it.tag_id == id }) {
                iterator.remove()
                changed = true
            }
        }
        notifyDataSetChanged()
        if (changed) {
            onSelectionChanged?.invoke(getSelectedTags())
        }
    }

    fun setSelectedTagIds(ids: Collection<String>) {
        selectedTagIds.clear()
        for (id in ids) {
            if (selectedTagIds.size >= maxSelection) break
            if (categories.any { it.tag_id == id }) {
                selectedTagIds.add(id)
            }
        }
        notifyDataSetChanged()
        onSelectionChanged?.invoke(getSelectedTags())
    }

    fun clearSelection() {
        if (selectedTagIds.isEmpty()) return
        selectedTagIds.clear()
        notifyDataSetChanged()
        onSelectionChanged?.invoke(emptyList())
    }

    fun getSelectedTagIds(): List<String> = selectedTagIds.toList()

    private fun getSelectedTags(): List<Tag> {
        if (selectedTagIds.isEmpty()) return emptyList()
        val selected = mutableListOf<Tag>()
        for (id in selectedTagIds) {
            val tag = categories.firstOrNull { it.tag_id == id }
            if (tag != null) {
                selected.add(tag)
            }
        }
        return selected
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(itemLayout, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val tag = categories[position]
        val isSelected = selectedTagIds.contains(tag.tag_id)
        holder.bind(tag, isSelected) {
            toggleSelection(tag, position)
        }
    }

    override fun getItemCount(): Int = categories.size

    private fun toggleSelection(tag: Tag, position: Int) {
        val tagId = tag.tag_id
        val wasSelected = selectedTagIds.contains(tagId)
        if (wasSelected) {
            selectedTagIds.remove(tagId)
            notifyItemChanged(position)
            onSelectionChanged?.invoke(getSelectedTags())
            return
        }

        if (selectedTagIds.size >= maxSelection) {
            onSelectionLimitReached?.invoke()
            return
        }

        selectedTagIds.add(tagId)
        notifyItemChanged(position)
        onSelectionChanged?.invoke(getSelectedTags())
    }

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val button: MaterialButton = itemView as MaterialButton

        fun bind(tag: Tag, isSelected: Boolean, onClick: () -> Unit) {
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
            button.isClickable = true
            button.isFocusable = true
            button.setOnClickListener { onClick() }
        }
    }

    companion object {
        private const val DEFAULT_MAX_SELECTION = 2
    }
}
