package com.z0diac.tesapi.data.model

data class UserMovieList(
    val movieId: Int = 0,
    val addedAt: Long = System.currentTimeMillis(),
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val title: String = ""
)