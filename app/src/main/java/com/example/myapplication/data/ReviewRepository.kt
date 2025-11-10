package com.example.myapplication.data

import android.util.Log
import com.example.myapplication.model.ReviewWithCafe
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.net.URLConnection
import java.time.Instant
import java.util.Locale
import java.util.UUID
import io.ktor.http.ContentType
import kotlinx.serialization.SerialName

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
        val img_url: String? = null,
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

    @Serializable
    private data class ReviewWithCafeResponse(
        val review_id: String,
        val cafe_id: String,
        val comment: String? = null,
        @SerialName("img_url")
        val imageUrl: String? = null,
        val rating: Double,
        val review_date: String,
        val cafe: CafeSummaryResponse? = null
    ) {
        fun toReviewWithCafe(): ReviewWithCafe {
            val parsedDate = runCatching { Instant.parse(review_date) }.getOrNull()
            return ReviewWithCafe(
                reviewId = review_id,
                cafeId = cafe_id,
                cafeName = cafe?.name.orEmpty(),
                comment = comment,
                rating = rating,
                reviewImageUrl = imageUrl,
                reviewDate = parsedDate
            )
        }
    }

    @Serializable
    private data class CafeSummaryResponse(
        val cafe_id: String,
        val name: String
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
            img_url = imageUrl,
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
        require(file.exists()) { "File does not exist: ${file.absolutePath}" }

        val detectedMime = URLConnection.guessContentTypeFromName(file.name)
            ?: file.inputStream().use { stream ->
                URLConnection.guessContentTypeFromStream(stream)
            }

        val rawExt = file.extension.takeIf { it.isNotBlank() }
            ?: detectedMime?.substringAfter('/')
            ?: "jpg"
        val normalizedExt = rawExt.lowercase(Locale.ROOT).let { ext ->
            if (ext == "jpeg") "jpg" else ext
        }

        val resolvedContentType = when {
            detectedMime.isNullOrBlank() && normalizedExt == "jpg" -> "image/jpeg"
            detectedMime.equals("image/jpg", ignoreCase = true) -> "image/jpeg"
            !detectedMime.isNullOrBlank() -> detectedMime
            else -> "image/$normalizedExt"
        }

        val path = "$STORAGE_PATH_PREFIX${UUID.randomUUID()}.$normalizedExt"
        val bucket = client.storage.from(STORAGE_BUCKET)

        bucket.upload(path, file) {
            upsert = false
            contentType = ContentType.parse(resolvedContentType)
        }

        "$PUBLIC_REVIEW_BASE_URL${path.removePrefix(STORAGE_PATH_PREFIX)}"
    }

    suspend fun getReviewsByUser(userId: String): List<ReviewWithCafe> = withContext(Dispatchers.IO) {
        client.from(REVIEW_TABLE)
            .select(columns = Columns.raw(
                "review_id,user_id,cafe_id,comment,img_url,rating,review_date," +
                        "cafe(cafe_id,name)"
                )) {
                    filter { eq("user_id", userId) }
                    order(column = "review_date", order = Order.DESCENDING)
                }
                .decodeList<ReviewWithCafeResponse>()
                .map { it.toReviewWithCafe() }
    }

}
