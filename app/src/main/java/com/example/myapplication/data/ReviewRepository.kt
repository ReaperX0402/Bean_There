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
        val extension = file.extension.ifBlank { "jpg" }.lowercase(Locale.ROOT)
        val fileName = "review_${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension"
        val remotePath = STORAGE_PATH_PREFIX + fileName
        val contentType = URLConnection.guessContentTypeFromName(file.name)
            ?: "image/$extension"
        val bytes = file.readBytes()

        client.storage
            .from(STORAGE_BUCKET)
            .upload(
                path = remotePath,
                data = bytes,
                upsert = false,
                contentType = contentType,
            )

        PUBLIC_REVIEW_BASE_URL + fileName
    }
}
