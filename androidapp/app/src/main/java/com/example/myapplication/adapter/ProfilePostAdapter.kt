package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.myapplication.R
import com.example.myapplication.model.ReviewWithCafe
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ProfilePostAdapter : RecyclerView.Adapter<ProfilePostAdapter.ProfilePostViewHolder>() {

    private val items: MutableList<ReviewWithCafe> = mutableListOf()

    fun submitList(reviews: List<ReviewWithCafe>) {
        items.clear()
        items.addAll(reviews)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfilePostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_post, parent, false)
        return ProfilePostViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProfilePostViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ProfilePostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val cafeNameText: TextView = itemView.findViewById(R.id.post_cafe_name)
        private val reviewDateText: TextView = itemView.findViewById(R.id.post_date)
        private val reviewRatingText: TextView = itemView.findViewById(R.id.post_rating)
        private val reviewCommentText: TextView = itemView.findViewById(R.id.post_comment)
        private val reviewImageView: ImageView = itemView.findViewById(R.id.post_image)

        fun bind(review: ReviewWithCafe) {
            val context = itemView.context
            cafeNameText.text = review.cafeName.ifBlank {
                context.getString(R.string.review_unknown_cafe)
            }
            reviewRatingText.text = context.getString(
                R.string.profile_review_rating_format,
                review.rating
            )

            val formattedDate = review.reviewDate?.let { DATE_FORMATTER.format(it) }
            reviewDateText.text = formattedDate?.let {
                context.getString(R.string.profile_review_date_format, it)
            } ?: context.getString(R.string.profile_review_date_unknown)

            if (!review.comment.isNullOrBlank()) {
                reviewCommentText.isVisible = true
                reviewCommentText.text = review.comment
            } else {
                reviewCommentText.isGone = true
                reviewCommentText.text = null
            }

            val imageUrl = review.reviewImageUrl
            if (!imageUrl.isNullOrBlank()) {
                reviewImageView.isVisible = true
                reviewImageView.load(imageUrl) {
                    crossfade(true)
                    placeholder(R.drawable.contact2)
                    error(R.drawable.contact2)
                }
                val cafeName = review.cafeName.ifBlank {
                    context.getString(R.string.review_unknown_cafe)
                }
                reviewImageView.contentDescription = context.getString(
                    R.string.profile_review_image_content_description,
                    cafeName
                )
            } else {
                reviewImageView.isGone = true
                reviewImageView.setImageDrawable(null)
                reviewImageView.contentDescription = null
            }
        }

        companion object {
            private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(
                "MMM d, yyyy",
                Locale.getDefault()
            ).withZone(ZoneId.systemDefault())
        }
    }
}
