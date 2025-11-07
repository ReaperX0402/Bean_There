package com.example.myapplication.data

import com.example.myapplication.model.Cafe
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CafeRepository {

    private val client get() = SupabaseProvider.client

    // --- READ ALL CAFES ---
    suspend fun getAllCafes(): List<Cafe> = withContext(Dispatchers.IO) {
        client.from("cafe")
            .select(Columns.ALL) {
                order(column = "name", order = Order.ASCENDING)
            }
            .decodeList<Cafe>()
    }

    // --- SEARCH CAFES BY NAME ---
    suspend fun searchCafes(query: String): List<Cafe> = withContext(Dispatchers.IO) {
        client.from("cafe")
            .select {
                filter{
                    ilike("name", "%$query%")
                }
                order(column = "name", order = Order.ASCENDING)
            }
            .decodeList<Cafe>()
    }

    // --- UPDATE CAFE RATING ---
    suspend fun updateCafeRating(cafeId: String, newRating: Double): Cafe =
        withContext(Dispatchers.IO) {
            client.from("cafe")
                .update({
                    set("rating_avg", newRating)
                }) {
                    filter { eq("cafe_id", cafeId) }
                    select()
                }
                .decodeSingle<Cafe>()
        }

}