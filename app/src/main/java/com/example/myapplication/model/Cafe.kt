package com.example.myapplication.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Cafe(
    val cafe_id: String,
    val name: String,
    val address: String? = null,
    val phone_no: String? = null,
    val rating_avg: Double? = null,
    @SerialName("img_url")
    val img_url: String? = null,
    val lat: Double? = null,
    val long: Double? = null,
    val operatingHours: String? = null,
    val tags: List<Tag> = emptyList()
) : java.io.Serializable
