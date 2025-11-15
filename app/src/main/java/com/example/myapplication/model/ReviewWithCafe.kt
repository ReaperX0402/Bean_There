package com.example.myapplication.model

import java.time.Instant

/**
 * Represents a review created by the current user along with the associated café details needed
 * for the profile screen.
 */
data class ReviewWithCafe(
    val reviewId: String,
    val cafeId: String,
    val cafeName: String,
    val comment: String?,
    val rating: Double,
    val reviewImageUrl: String?,
    val reviewDate: Instant?
)
