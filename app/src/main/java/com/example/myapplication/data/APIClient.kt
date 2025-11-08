package com.example.myapplication.data

import com.example.myapplication.model.Cafe
import com.example.myapplication.model.Tag
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object CafeRepository {

    private const val DEFAULT_IMAGE_BUCKET = "cafe-images"

    private val client get() = SupabaseProvider.client

    @Serializable
    private data class CafeTagResponse(
        val tag: Tag? = null
    )

    @Serializable
    private data class CafeResponse(
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
        val cafeTags: List<CafeTagResponse> = emptyList()
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

    private suspend fun fetchCafes(columns: Columns): List<Cafe> = withContext(Dispatchers.IO) {
        client.from("cafe")
            .select(columns) {
                order(column = "name", order = Order.ASCENDING)
            }
            .decodeList<CafeResponse>()
            .map { it.toCafe() }
    }

    // --- READ ALL CAFES ---
    suspend fun getAllCafes(): List<Cafe> = fetchCafes(
        Columns.raw("*, cafe_tag(tag(*))")
    )

    // --- SEARCH CAFES BY NAME ---
    suspend fun searchCafes(query: String): List<Cafe> = withContext(Dispatchers.IO) {
        client.from("cafe")
            .select(columns = Columns.raw("*, cafe_tag(tag(*))")) {
                filter {
                    ilike("name", "%$query%")
                }
                order(column = "name", order = Order.ASCENDING)
            }
            .decodeList<CafeResponse>()
            .map { it.toCafe() }
    }

    // --- READ ALL TAGS ---
    suspend fun getAllTags(): List<Tag> = withContext(Dispatchers.IO) {
        client.from("tag")
            .select(Columns.ALL) {
                order(column = "tag_name", order = Order.ASCENDING)
            }
            .decodeList<Tag>()
    }

    // --- READ CAFE BY ID ---
    suspend fun getCafeById(cafeId: String): Cafe? = withContext(Dispatchers.IO) {
        client.from("cafe")
            .select(columns = Columns.raw("*, cafe_tag(tag(*))")) {
                filter {
                    eq("cafe_id", cafeId)
                }
                limit(1)
            }
            .decodeList<CafeResponse>()
            .firstOrNull()
            ?.toCafe()
    }

    // --- UPDATE CAFE RATING ---
    suspend fun updateCafeRating(cafeId: String, newRating: Double): Cafe =
        withContext(Dispatchers.IO) {
            client.from("cafe")
                .update({
                    set("rating_avg", newRating)
                }) {
                    filter { eq("cafe_id", cafeId) }
                    select(columns = Columns.raw("*, cafe_tag(tag(*))"))
                }
                .decodeSingle<CafeResponse>()
                .toCafe()
        }
}