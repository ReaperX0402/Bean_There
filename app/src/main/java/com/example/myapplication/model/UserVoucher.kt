package com.example.myapplication.model

data class UserVoucher(
    val id: String,
    val reward: Reward,
    val status: String,
    val obtainedAt: String?
)
