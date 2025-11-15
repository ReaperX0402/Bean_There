package com.example.myapplication.model

import kotlinx.serialization.Serializable

@Serializable
data class Tag(
    val tag_id: String,
    val tag_name: String
) : java.io.Serializable
