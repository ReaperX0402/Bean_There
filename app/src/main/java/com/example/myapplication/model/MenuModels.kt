package com.example.myapplication.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Menu(
    @SerialName("menu_id")
    val menuId: String,
    @SerialName("cafe_id")
    val cafeId: String,
    val name: String,
    val description: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true
)

@Serializable
data class MenuItem(
    @SerialName("item_id")
    val itemId: String,
    @SerialName("menu_id")
    val menuId: String,
    @SerialName("item_name")
    val itemName: String,
    @SerialName("img_url")
    val imageUrl: String? = null,
    val text: String? = null,
    val description: String? = null,
    val price: Double = 0.0,
    val availability: Boolean? = null
) {
    val isAvailable: Boolean
        get() = availability != false
}

data class MenuSection(
    val menu: Menu,
    val cafeName: String,
    val items: List<MenuItem>
) {
    val menuId: String get() = menu.menuId
    val cafeId: String get() = menu.cafeId
    val menuName: String get() = menu.name
    val menuDescription: String? get() = menu.description
}

@Serializable
data class CartItem(
    val itemId: String,
    val itemName: String,
    val price: Double,
    val quantity: Int,
    val imageUrl: String? = null
) {
    val lineTotal: Double
        get() = price * quantity
}

@Serializable
data class CartState(
    val cafeId: String? = null,
    val cafeName: String? = null,
    val items: List<CartItem> = emptyList()
) {
    val total: Double
        get() = items.sumOf { it.lineTotal }

    val itemCount: Int
        get() = items.sumOf { it.quantity }

    val isEmpty: Boolean
        get() = items.isEmpty()
}

data class OrderRequest(
    val cafeId: String,
    val userId: String,
    val items: List<CartItem>,
    val total: Double,
    val status: String = "pending",
    val notes: String? = null,
    val pax: Int? = null
)
