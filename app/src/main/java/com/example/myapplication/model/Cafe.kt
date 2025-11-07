package com.example.myapplication.model

data class Cafe(
    val cafe_id: String,
    val name: String,
    val address: String? = null,
    val phone_no: String? = null,
    val rating_avg: Double? = null,
    val img_url: String? = null,
    val lat: Double? = null,
    val long: Double? = null,
    val tags: List<Tag> = emptyList()
)
