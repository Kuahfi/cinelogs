package com.z0diac.tesapi.data.model

data class Review(
    val id: String = "",
    val movieId: Int = 0,
    val userId: String = "",
    val username: String = "",
    val rating: Float = 0f,
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val likes: Int = 0
)