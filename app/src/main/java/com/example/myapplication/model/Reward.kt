package com.example.myapplication.model

data class Reward(
    val id: String,
    val name: String,
    val description: String?,
    val pointsRequired: Int,
    val imageUrl: String?,
    val status: String?
)
