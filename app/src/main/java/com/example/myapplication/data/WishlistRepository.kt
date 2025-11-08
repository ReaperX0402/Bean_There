package com.example.myapplication.data

import com.example.myapplication.model.Cafe
import com.example.myapplication.model.Tag
import com.example.myapplication.model.WishlistItem
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class WishlistAddResult(
    val item: WishlistItem,
    val isNew: Boolean
)

object WishlistRepository {

    private val client get() = SupabaseProvider.client

    @Serializable
    private data class WishlistTagResponse(
        val tag: Tag? = null
    )

    @Serializable
    private data class WishlistCafeResponse(
        val cafe_id: String,
        val name: String,
        val address: String? = null,
        val phone_no: String? = null,
        val rating_avg: Double? = null,
        @SerialName("img_url")
        val imagePath: String? = null,
        val lat: Double? = null,
        val long: Double? = null,
        @SerialName("operating_hours")
        val operatingHours: String? = null,
        @SerialName("cafe_tag")
        val cafeTags: List<WishlistTagResponse> = emptyList()
    ) {
        fun toCafe(): Cafe = Cafe(
            cafe_id = cafe_id,
            name = name,
            address = address,
            phone_no = phone_no,
            rating_avg = rating_avg,
            img_url = resolveImageUrl(imagePath),
            lat = lat,
            long = long,
            operatingHours = operatingHours,
            tags = cafeTags.mapNotNull { it.tag }
        )
    }

    @Serializable
    private data class WishlistResponse(
        val wishlist_id: String,
        val cafe_id: String,
        val created_at: String? = null,
        val cafe: WishlistCafeResponse? = null
    ) {
        fun toWishlistItem(fallbackCafe: Cafe? = null): WishlistItem? {
            val cafe = cafe?.toCafe() ?: fallbackCafe ?: return null
            return WishlistItem(
                id = wishlist_id,
                cafe = cafe,
                createdAt = created_at
            )
        }
    }

    suspend fun getWishlist(userId: String): List<WishlistItem> = withContext(Dispatchers.IO) {
        val responses = client.from("wishlist")
            .select(columns = SELECT_COLUMNS) {
                filter { eq("user_id", userId) }
                order(column = "created_at", order = Order.DESCENDING)
            }
            .decodeList<WishlistResponse>()

        if (responses.isEmpty()) {
            return@withContext emptyList()
        }

        val fallbackById = buildFallbackCafes(responses)
        responses.mapNotNull { response ->
            val fallbackCafe = fallbackById[response.cafe_id]
            response.toWishlistItem(fallbackCafe = fallbackCafe)
        }
    }

    suspend fun addToWishlist(
        userId: String,
        cafe: Cafe
    ): WishlistAddResult = withContext(Dispatchers.IO) {
        val cafeId = cafe.cafe_id
        val existing = client.from("wishlist")
            .select(columns = SELECT_COLUMNS) {
                filter {
                    eq("user_id", userId)
                    eq("cafe_id", cafeId)
                }
                limit(1)
            }
            .decodeList<WishlistResponse>()
            .firstNotNullOfOrNull { it.toWishlistItem(fallbackCafe = cafe) }

        if (existing != null) {
            return@withContext WishlistAddResult(existing, false)
        }

        val created = client.from("wishlist")
            .insert(
                mapOf(
                    "user_id" to userId,
                    "cafe_id" to cafeId
                )
            ) {
                select(columns = SELECT_COLUMNS)
            }
            .decodeSingle<WishlistResponse>()
            .toWishlistItem(fallbackCafe = cafe)
            ?: throw IllegalStateException("Wishlist item missing cafe data")
        WishlistAddResult(created, true)
    }

    suspend fun removeFromWishlist(userId: String, wishlistId: String) = withContext(Dispatchers.IO) {
        client.from("wishlist")
            .delete {
                filter {
                    eq("wishlist_id", wishlistId)
                    eq("user_id", userId)
                }
            }
    }

    private fun resolveImageUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http", ignoreCase = true)) return path

        val sanitized = path.trim().removePrefix("/")
        val firstSlash = sanitized.indexOf('/')
        val bucket = if (firstSlash in 1 until sanitized.length) {
            sanitized.substring(0, firstSlash)
        } else {
            DEFAULT_IMAGE_BUCKET
        }
        val objectPath = if (firstSlash in 1 until sanitized.length) {
            sanitized.substring(firstSlash + 1)
        } else {
            sanitized
        }
        if (objectPath.isBlank()) return null

        return client.storage.from(bucket).publicUrl(objectPath)
    }

    private const val DEFAULT_IMAGE_BUCKET = "cafe-images"
    private val SELECT_COLUMNS = Columns.raw(
        "wishlist_id, cafe_id, created_at, cafe:cafe_id(*, cafe_tag(tag(*)))"
    )

    private suspend fun buildFallbackCafes(
        responses: List<WishlistResponse>
    ): Map<String, Cafe> {
        val missingIds = responses
            .filter { it.cafe == null }
            .map { it.cafe_id }
            .distinct()

        if (missingIds.isEmpty()) {
            return emptyMap()
        }

        val fallback = mutableMapOf<String, Cafe>()
        for (id in missingIds) {
            val cafe = CafeRepository.getCafeById(id)
            if (cafe != null) {
                fallback[id] = cafe
            }
        }
        return fallback
    }
}
