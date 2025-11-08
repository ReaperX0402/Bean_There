package com.example.myapplication.model

import java.io.Serializable

/**
 * Represents a wishlist entry that belongs to a user.
 */
data class WishlistItem(
    val id: String,
    val cafe: Cafe,
    val createdAt: String? = null
) : Serializable
