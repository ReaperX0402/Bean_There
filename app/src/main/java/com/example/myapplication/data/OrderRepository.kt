package com.example.myapplication.data

import com.example.myapplication.model.Menu
import com.example.myapplication.model.MenuItem
import com.example.myapplication.model.MenuSection
import com.example.myapplication.model.OrderRequest
import com.example.myapplication.model.Cafe
import com.example.myapplication.model.CafeMenuGroup
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.collections.buildMap

object OrderRepository {

    private val client get() = SupabaseProvider.client

    @Serializable
    private data class MenuResponse(
        @SerialName("menu_id") val menuId: String,
        @SerialName("cafe_id") val cafeId: String,
        val name: String,
        val description: String? = null,
        @SerialName("is_active") val isActive: Boolean = true,
        val cafe: Cafe? = null
    ) {
        fun toSection(items: List<MenuItem>): MenuSection {
            val menu = Menu(
                menuId = menuId,
                cafeId = cafeId,
                name = name,
                description = description,
                isActive = isActive
            )
            val cafeName = cafe?.name ?: cafeId
            return MenuSection(
                menu = menu,
                cafeName = cafeName,
                items = items
            )
        }
    }

    @Serializable
    private data class OrderResponse(
        @SerialName("order_id") val orderId: String
    )

    suspend fun getMenuHierarchy(): List<CafeMenuGroup> = withContext(Dispatchers.IO) {
        val menuResponses = client.from("menu")
            .select(columns = Columns.raw("*, cafe(*)")) {
                order(column = "name", order = Order.ASCENDING)
            }
            .decodeList<MenuResponse>()
            .filter { it.isActive }

        if (menuResponses.isEmpty()) {
            return@withContext emptyList<CafeMenuGroup>()
        }

        val menuIds = menuResponses.map { it.menuId }.toSet()
        val itemsByMenu = fetchMenuItems(menuIds)

        val sections = menuResponses
            .map { response ->
                val sectionItems = itemsByMenu[response.menuId].orEmpty()
                response.toSection(sectionItems)
            }

        sections
            .groupBy { section -> section.cafeId to section.cafeName }
            .mapNotNull { (key, menuList) ->
                val filteredMenus = menuList.filter { it.items.isNotEmpty() }
                if (filteredMenus.isEmpty()) {
                    null
                } else {
                    CafeMenuGroup(
                        cafeId = key.first,
                        cafeName = key.second,
                        menus = filteredMenus
                    )
                }
            }
            .sortedBy { it.cafeName }
    }

    private suspend fun fetchMenuItems(menuIds: Set<String>): Map<String, List<MenuItem>> =
        withContext(Dispatchers.IO) {
            if (menuIds.isEmpty()) return@withContext emptyMap()

            client.from("item")
                .select(columns = Columns.ALL) {
                    order(column = "item_name", order = Order.ASCENDING)
                }
                .decodeList<MenuItem>()
                .filter { it.menuId in menuIds }
                .groupBy { it.menuId }
        }

    suspend fun placeOrder(request: OrderRequest): String = withContext(Dispatchers.IO) {
        val orderPayload = buildMap {
            put("cafe_id", request.cafeId)
            put("user_id", request.userId)
            put("status", request.status)
            put("total", request.total)
            request.notes?.takeIf { it.isNotBlank() }?.let { put("notes", it) }
            request.pax?.let { put("no_pax", it) }
        }

        val orderId = client.from("order")
            .insert(orderPayload) {
                select(columns = Columns.list("order_id"))
                single()
            }
            .decodeSingle<OrderResponse>()
            .orderId

        if (request.items.isNotEmpty()) {
            val orderItems = request.items.map { item ->
                mapOf(
                    "order_id" to orderId,
                    "item_id" to item.itemId,
                    "qty" to item.quantity,
                    "unit_price" to item.price,
                    "line_total" to item.lineTotal
                )
            }
            client.from("order_item").insert(orderItems)
        }

        orderId
    }
}
