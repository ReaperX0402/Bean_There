package com.example.myapplication.data

import android.content.Context
import androidx.core.content.edit
import com.example.myapplication.model.CartItem
import com.example.myapplication.model.CartState
import com.example.myapplication.model.MenuItem
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object CartManager {

    class CartConflictException(val existingCafeName: String?) : IllegalStateException()

    private const val PREFS_NAME = "order_cart"
    private const val KEY_CART_STATE = "cart_state"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun getCartState(context: Context): CartState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_CART_STATE, null) ?: return CartState()
        return runCatching { json.decodeFromString(CartState.serializer(), stored) }
            .getOrElse { CartState() }
    }

    fun addItem(
        context: Context,
        cafeId: String,
        cafeName: String,
        menuItem: MenuItem
    ): Result<CartState> {
        val current = getCartState(context)
        if (current.cafeId != null && current.cafeId != cafeId) {
            return Result.failure(CartConflictException(current.cafeName))
        }

        val updatedItems = current.items.toMutableList()
        val existingIndex = updatedItems.indexOfFirst { it.itemId == menuItem.itemId }
        if (existingIndex >= 0) {
            val currentItem = updatedItems[existingIndex]
            updatedItems[existingIndex] = currentItem.copy(quantity = currentItem.quantity + 1)
        } else {
            updatedItems.add(
                CartItem(
                    itemId = menuItem.itemId,
                    itemName = menuItem.itemName,
                    price = menuItem.price,
                    quantity = 1,
                    imageUrl = menuItem.imageUrl
                )
            )
        }

        val newState = CartState(
            cafeId = cafeId,
            cafeName = cafeName,
            items = updatedItems
        )
        persistCart(context, newState)
        return Result.success(newState)
    }

    fun updateQuantity(context: Context, itemId: String, quantity: Int): CartState {
        val current = getCartState(context)
        if (current.isEmpty) return current

        val updatedItems = current.items.mapNotNull { cartItem ->
            when {
                cartItem.itemId != itemId -> cartItem
                quantity <= 0 -> null
                else -> cartItem.copy(quantity = quantity)
            }
        }

        val newState = if (updatedItems.isEmpty()) {
            CartState()
        } else {
            current.copy(items = updatedItems)
        }

        persistCart(context, newState)
        return newState
    }

    fun removeItem(context: Context, itemId: String): CartState {
        return updateQuantity(context, itemId, 0)
    }

    fun clearCart(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { remove(KEY_CART_STATE) }
    }

    private fun persistCart(context: Context, state: CartState) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (state.items.isEmpty()) {
            prefs.edit { remove(KEY_CART_STATE) }
        } else {
            val encoded = json.encodeToString(state)
            prefs.edit { putString(KEY_CART_STATE, encoded) }
        }
    }
}
