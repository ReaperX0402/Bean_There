package com.example.myapplication.model

data class User(
    val userId: String,
    val username: String,
    val email: String?,
    val password: String?,
    val profilePictureUrl: String?,
    val createdAt: String?
)
