package com.example.myapplication.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.example.myapplication.model.CartItem
import com.example.myapplication.model.CartState
import com.example.myapplication.model.MenuItem
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object CartManager {

    class CartConflictException(val existingCafeName: String?) : IllegalStateException()

    private const val TAG = "CartManager"
    private const val PREFS_NAME = "order_cart"
    private const val KEY_CART_STATE = "cart_state"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun getCartState(context: Context): CartState {
        val prefs = prefs(context)
        val stored = prefs.getString(KEY_CART_STATE, null) ?: return CartState()
        return try {
            json.decodeFromString(CartState.serializer(), stored)
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to decode stored cart state, clearing cache", error)
            prefs.edit { remove(KEY_CART_STATE) }
            CartState()
        }
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
        return runCatching {
            persistCart(context, newState)
            newState
        }
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

        return try {
            persistCart(context, newState)
            newState
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to persist cart quantity change", error)
            clearCart(context)
            CartState()
        }
    }

    fun removeItem(context: Context, itemId: String): CartState {
        return updateQuantity(context, itemId, 0)
    }

    fun clearCart(context: Context) {
        prefs(context).edit { remove(KEY_CART_STATE) }
    }

    private fun persistCart(context: Context, state: CartState) {
        val prefs = prefs(context)
        if (state.items.isEmpty()) {
            prefs.edit { remove(KEY_CART_STATE) }
        } else {
            val encoded = json.encodeToString(CartState.serializer(), state)
            prefs.edit { putString(KEY_CART_STATE, encoded) }
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
