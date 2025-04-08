package com.z0diac.tesapi.data.model

data class Review(
    val id: String = "",
    val userId: String = "",
    val username: String = "",  // Make sure this exists in your stored documents
    val movieId: Int = 0,
    val movieTitle: String = "", // Add this since it's in your stored document
    val reviewText: String = "",
    val rating: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)