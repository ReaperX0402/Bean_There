package com.example.myapplication.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.net.URLConnection
import java.time.Instant
import java.util.Locale
import java.util.UUID

object ReviewRepository {

    private const val REVIEW_TABLE = "review"
    private const val REVIEW_TAG_TABLE = "review_tag"
    private const val STORAGE_BUCKET = "photos"
    private const val STORAGE_PATH_PREFIX = "reviews/"
    private const val PUBLIC_REVIEW_BASE_URL = "https://gaotkltiafwezuzegkkz.supabase.co/storage/v1/object/public/photos/reviews/"

    private val client get() = SupabaseProvider.client

    @Serializable
    private data class ReviewInsert(
        val user_id: String,
        val cafe_id: String,
        val comment: String? = null,
        val image_url: String? = null,
        val rating: Double,
        val review_date: String
    )

    @Serializable
    private data class ReviewResponse(
        val review_id: String
    )

    @Serializable
    private data class ReviewTagInsert(
        val review_id: String,
        val tag_id: String
    )

    suspend fun createReview(
        userId: String,
        cafeId: String,
        comment: String?,
        rating: Double,
        imageUrl: String?
    ): String = withContext(Dispatchers.IO) {
        val payload = ReviewInsert(
            user_id = userId,
            cafe_id = cafeId,
            comment = comment,
            image_url = imageUrl,
            rating = rating,
            review_date = Instant.now().toString()
        )

        client.from(REVIEW_TABLE)
            .insert(payload) {
                select()
            }
            .decodeSingle<ReviewResponse>()
            .review_id
    }

    suspend fun attachTags(reviewId: String, tagIds: List<String>) {
        if (tagIds.isEmpty()) return
        val rows = tagIds.map { ReviewTagInsert(review_id = reviewId, tag_id = it) }
        withContext(Dispatchers.IO) {
            client.from(REVIEW_TAG_TABLE).insert(rows)
        }
    }

    suspend fun uploadReviewImage(file: File): String = withContext(Dispatchers.IO) {
        val ext = file.extension.ifBlank { "jpg" }.lowercase(Locale.ROOT)
        val path = "reviews/${UUID.randomUUID()}.$ext"
        val type = URLConnection.guessContentTypeFromName(file.name) ?: "image/$ext"

        val bucket = client.storage.from("photos")

        bucket.upload(path, file) {
            upsert = false
            contentType = type
        }

        bucket.publicUrl(path)
    }

}
