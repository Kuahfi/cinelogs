package com.z0diac.tesapi.data.api

import com.z0diac.tesapi.data.model.MovieResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface TmdbApi {
    @GET("movie/popular")
    fun getPopularMovies(
        @Query("api_key") apiKey: String
    ): Call<MovieResponse>
}

//Hello