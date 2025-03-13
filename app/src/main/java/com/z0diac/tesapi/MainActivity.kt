package com.z0diac.tesapi

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.z0diac.tesapi.data.api.RetrofitInstance
import com.z0diac.tesapi.data.model.MovieResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private val apiKey = "043cbbcb77cb0cae18791c2111db5c75"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val moviePoster: ImageView = findViewById(R.id.moviePoster)
        fetchPopularMovies(moviePoster)
    }

    private fun fetchPopularMovies(moviePoster: ImageView) {
        RetrofitInstance.api.getPopularMovies(apiKey).enqueue(object : Callback<MovieResponse> {
            override fun onResponse(call: Call<MovieResponse>, response: Response<MovieResponse>) {
                if (response.isSuccessful) {
                    val firstMovie = response.body()?.results?.firstOrNull()
                    firstMovie?.let { movie ->
                        val posterUrl = "https://image.tmdb.org/t/p/w500${movie.posterPath}" // URL poster
                        Glide.with(this@MainActivity)
                            .load(posterUrl)
                            .into(moviePoster)
                    }
                }
            }

            override fun onFailure(call: Call<MovieResponse>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }
}
