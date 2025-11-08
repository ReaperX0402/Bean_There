package com.example.myapplication.model

import java.io.Serializable


data class WishlistItem(
    val id: String,
    val cafe: Cafe,
    val addedAt: String? = null
) : Serializable
